package com.coretool.param.application.service;

import com.coretool.param.domain.config.version.repository.ProductVersionRepository;
import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.infrastructure.persistence.entity.EntityVersionInfoPo;
import com.coretool.param.ui.vo.ListPageWithKeywordQuery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VersionAppServiceTest {

    @Mock
    private ProductVersionRepository productVersionRepository;

    @Mock
    private OperationLogAppService operationLogAppService;

    private VersionAppService newSvc() {
        return new VersionAppService(productVersionRepository, operationLogAppService);
    }

    @Test
    void page_shouldReturnPagingFields_whenRepositoryReturnsSlice() {
        when(productVersionRepository.pageByProduct(eq("p1"), eq(1), eq(10)))
                .thenReturn(new PageSlice<>(List.of(), 0, 1, 10));

        var out = newSvc().page("p1", ListPageWithKeywordQuery.of(1, 10, "ignored"));

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
                        "产品版本",
                        "hint",
                        List.of("版本ID", "版本名称", "版本类型", "支持版本", "版本说明", "版本描述", "责任人"),
                        List.of());

        var out = newSvc().importCsv("p1", bytes);

        assertThat(out.getTotalRows()).isGreaterThanOrEqualTo(0);
        assertThat(out.getSuccessCount()).isGreaterThanOrEqualTo(0);
        assertThat(out.getFailureCount()).isGreaterThanOrEqualTo(0);
        assertThat(out.getSuccessCount() + out.getFailureCount()).isLessThanOrEqualTo(out.getTotalRows());
    }

    @Test
    void importCsv_shouldProcessOneRow_andReturnAtLeastOneResult() {
        when(productVersionRepository.findDisabledByNameInProduct(eq("p1"), any())).thenReturn(Optional.empty());
        when(productVersionRepository.existsSameNameInProduct(eq("p1"), any(), eq(null))).thenReturn(false);

        byte[] bytes =
                ExcelTestHelper.workbookBytes(
                        "产品版本",
                        "hint",
                        List.of("版本ID", "版本名称", "版本类型", "支持版本", "版本说明", "版本描述", "责任人"),
                        List.of(List.of("", "V-A", "TYPE", "SUP", "D1", "D2", "u1")));

        var out = newSvc().importCsv("p1", bytes);

        assertThat(out.getSuccessCount() + out.getFailureCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void exportAndTemplate_shouldReturnBytes() {
        when(productVersionRepository.pageByProduct(eq("p1"), eq(1), eq(10)))
                .thenReturn(new PageSlice<>(List.of(), 0, 1, 10));

        assertThat(newSvc().exportCsvBody("p1", 1, 10)).isNotEmpty();
        assertThat(newSvc().templateCsv()).isNotEmpty();
    }

    @Test
    void update_shouldThrow_whenNotFound() {
        EntityVersionInfoPo patch = new EntityVersionInfoPo();
        assertThatThrownBy(() -> newSvc().update("p1", "v1", patch))
                .isInstanceOf(DomainRuleException.class);
    }
}

