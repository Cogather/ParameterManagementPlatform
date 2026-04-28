package com.coretool.param.application.service;

import com.coretool.param.domain.command.repository.CommandTypeDefinitionRepository;
import com.coretool.param.domain.command.repository.CommandTypeVersionRangeRepository;
import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.infrastructure.persistence.entity.CommandTypeVersionRangePo;
import com.coretool.param.ui.vo.ListPageWithTypeFilterQuery;

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
class CommandTypeVersionRangeAppServiceTest {

    @Mock
    private CommandTypeVersionRangeRepository rangeRepository;

    @Mock
    private CommandTypeDefinitionRepository typeRepository;

    @Mock
    private OperationLogAppService operationLogAppService;

    private CommandTypeVersionRangeAppService newSvc() {
        return new CommandTypeVersionRangeAppService(rangeRepository, typeRepository, operationLogAppService);
    }

    @Test
    void page_shouldReturnPagingFields_whenRepositoryReturnsSlice() {
        when(rangeRepository.pageByProduct(eq("p1"), eq(1), eq(10), eq("t1")))
                .thenReturn(new PageSlice<>(List.of(), 0, 1, 10));

        var out = newSvc().page("p1", ListPageWithTypeFilterQuery.of(1, 10, "t1"));

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
    void importExcel_shouldReturnCountsConsistent_whenHeaderOnly() {
        byte[] bytes =
                ExcelTestHelper.workbookBytes(
                        "版本区段",
                        "hint",
                        List.of("归属命令ID", "类型ID", "区段ID", "起始序号", "结束序号", "说明", "区段划分类型", "归属版本ID"),
                        List.of());

        var out = newSvc().importExcel("p1", bytes);

        assertThat(out.getTotalRows()).isGreaterThanOrEqualTo(0);
        assertThat(out.getSuccessCount()).isGreaterThanOrEqualTo(0);
        assertThat(out.getFailureCount()).isGreaterThanOrEqualTo(0);
        assertThat(out.getSuccessCount() + out.getFailureCount()).isLessThanOrEqualTo(out.getTotalRows());
    }

    @Test
    void exportAndTemplate_shouldReturnBytes() {
        when(rangeRepository.pageByProduct(eq("p1"), eq(1), eq(10), eq(null)))
                .thenReturn(new PageSlice<>(List.of(), 0, 1, 10));

        assertThat(newSvc().exportExcel("p1", 1, 10, null)).isNotEmpty();
        assertThat(newSvc().importTemplate()).isNotEmpty();
    }

    @Test
    void update_shouldThrow_whenNotFoundOrNotOwned() {
        CommandTypeVersionRangePo patch = new CommandTypeVersionRangePo();
        assertThatThrownBy(() -> newSvc().update("p1", "r1", patch))
                .isInstanceOf(DomainRuleException.class);
    }
}

