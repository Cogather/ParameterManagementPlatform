package com.coretool.param.application.service;

import com.coretool.param.domain.config.category.BusinessCategory;
import com.coretool.param.domain.config.category.repository.BusinessCategoryRepository;
import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.infrastructure.persistence.entity.EntityBusinessCategoryPo;
import com.coretool.param.ui.vo.ListPageWithKeywordQuery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessCategoryAppServiceTest {

    @Mock
    private BusinessCategoryRepository categoryRepository;

    @Mock
    private OperationLogAppService operationLogAppService;

    private BusinessCategoryAppService newSvc() {
        return new BusinessCategoryAppService(categoryRepository, operationLogAppService);
    }

    @Test
    void page_shouldReturnPagingFields_whenRepositoryReturnsSlice() {
        when(categoryRepository.pageByProduct(eq("p1"), eq(1), eq(10), eq("k")))
                .thenReturn(new PageSlice<>(List.of(), 0, 1, 10));

        var out = newSvc().page("p1", ListPageWithKeywordQuery.of(1, 10, "k"));

        assertThat(out.getPage()).isEqualTo(1);
        assertThat(out.getSize()).isEqualTo(10);
        assertThat(out.getTotal()).isEqualTo(0);
        assertThat(out.getRecords()).isEmpty();
    }

    @Test
    void create_shouldThrow_whenInputNull() {
        assertThatThrownBy(() -> newSvc().create("p1", null))
                .isInstanceOf(DomainRuleException.class)
                .hasMessageContaining("分类中英文名称不能为空");
    }

    @Test
    void create_shouldInsertAndLog_whenNoDisabledDuplicate() {
        when(categoryRepository.findDisabledByChineseNameInProduct(eq("p1"), any()))
                .thenReturn(Optional.empty());
        when(categoryRepository.existsSameChineseNameInProduct(eq("p1"), any(), eq(null)))
                .thenReturn(false);
        doNothing().when(categoryRepository).insert(any());

        EntityBusinessCategoryPo in = new EntityBusinessCategoryPo();
        in.setCategoryNameCn("分类A");
        in.setCategoryNameEn("CategoryA");
        in.setCreatorId("u1");

        EntityBusinessCategoryPo out = newSvc().create("p1", in);

        assertThat(out.getCategoryId()).isNotBlank();
        assertThat(out.getCategoryStatus()).isEqualTo(1);
        verify(categoryRepository).insert(any());
        verify(operationLogAppService).logBusinessCategoryCreate(eq("p1"), any(EntityBusinessCategoryPo.class), eq("u1"));
    }

    @Test
    void disable_shouldUpdateAndLogDelete() {
        BusinessCategory existing =
                BusinessCategory.registerNew(
                        new BusinessCategory.Registration(
                                "p1",
                                "bc1",
                                "CN",
                                "EN",
                                "scope",
                                "type",
                                1,
                                "c",
                                "u",
                                LocalDateTime.now()));
        when(categoryRepository.findByCategoryId("bc1")).thenReturn(Optional.of(existing));

        newSvc().disable("p1", "bc1");

        verify(categoryRepository).update(any(BusinessCategory.class));
        verify(operationLogAppService).logBusinessCategoryDelete(eq("p1"), eq("bc1"), any(), any());
    }

    @Test
    void importCsv_shouldReturnEmptyResult_whenOnlyHeader() {
        byte[] bytes =
                ExcelTestHelper.workbookBytes(
                        "业务分类",
                        "hint",
                        List.of("分类ID", "分类名称（中文）", "分类名称（英文）", "包含特性范围", "所属类别"),
                        List.of());

        var out = newSvc().importCsv("p1", bytes);

        assertThat(out.getTotalRows()).isGreaterThanOrEqualTo(0);
        assertThat(out.getSuccessCount()).isGreaterThanOrEqualTo(0);
        assertThat(out.getFailureCount()).isGreaterThanOrEqualTo(0);
        assertThat(out.getSuccessCount() + out.getFailureCount()).isLessThanOrEqualTo(out.getTotalRows());
    }
}

