package com.coretool.param.application.service;

import com.coretool.param.domain.config.keyword.repository.ChangeSourceKeywordRepository;
import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.ui.vo.ListPageWithKeywordQuery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeSourceKeywordAppServiceTest {

    @Mock
    private ChangeSourceKeywordRepository keywordRepository;

    @Mock
    private OperationLogAppService operationLogAppService;

    private ChangeSourceKeywordAppService newSvc() {
        return new ChangeSourceKeywordAppService(keywordRepository, operationLogAppService);
    }

    @Test
    void page_shouldReturnPagingFields_whenRepositoryReturnsSlice() {
        when(keywordRepository.pageByProduct(eq("p1"), eq(1), eq(10), eq("k")))
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
                .isInstanceOf(DomainRuleException.class);
    }

    @Test
    void importCsv_shouldReturnCountsConsistent_whenHeaderOnly() {
        byte[] bytes =
                ExcelTestHelper.workbookBytes(
                        "变更来源关键字",
                        "hint",
                        List.of("关键字 ID", "关键字正则字段", "原因"),
                        List.of());

        var out = newSvc().importCsv("p1", bytes);

        assertThat(out.getTotalRows()).isGreaterThanOrEqualTo(0);
        assertThat(out.getSuccessCount()).isGreaterThanOrEqualTo(0);
        assertThat(out.getFailureCount()).isGreaterThanOrEqualTo(0);
        assertThat(out.getSuccessCount() + out.getFailureCount()).isLessThanOrEqualTo(out.getTotalRows());
    }

    @Test
    void exportAndTemplate_shouldReturnBytes() {
        when(keywordRepository.pageByProduct(eq("p1"), eq(1), eq(10), eq(null)))
                .thenReturn(new PageSlice<>(List.of(), 0, 1, 10));

        assertThat(newSvc().exportCsv("p1", 1, 10)).isNotEmpty();
        assertThat(newSvc().templateCsv()).isNotEmpty();
    }
}

