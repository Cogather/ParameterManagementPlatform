package com.coretool.param.application.service;

import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.domain.command.repository.CommandTypeDefinitionRepository;
import com.coretool.param.infrastructure.persistence.entity.CommandTypeDefinitionPo;
import com.coretool.param.infrastructure.persistence.mapper.EntityCommandMappingMapper;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandTypeDefinitionAppServiceTest {

    @Mock
    private CommandTypeDefinitionRepository repo;

    @Mock
    private OperationLogAppService operationLogAppService;

    @Mock
    private EntityCommandMappingMapper entityCommandMappingMapper;

    private CommandTypeDefinitionAppService newSvc() {
        return new CommandTypeDefinitionAppService(repo, operationLogAppService, entityCommandMappingMapper);
    }

    @Test
    void page_shouldReturnPagingFields_whenRepositoryReturnsSlice() {
        when(repo.pageByProduct(eq("p1"), eq(1), eq(10), eq("k")))
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
    void create_shouldInsert_whenValid() {
        when(repo.findDisabledByNameInProduct(eq("p1"), any())).thenReturn(Optional.empty());
        when(repo.existsSameNameInProduct(eq("p1"), any(), eq(null))).thenReturn(false);
        doNothing().when(repo).insert(any());

        CommandTypeDefinitionPo in = new CommandTypeDefinitionPo();
        in.setOwnedCommandId("c1");
        in.setCommandTypeName("类型A");
        in.setCommandType("BIT");
        in.setCreatorId("u1");

        CommandTypeDefinitionPo out = newSvc().create("p1", in);

        assertThat(out.getCommandTypeId()).isNotBlank();
        verify(repo).insert(any());
    }

    @Test
    void importExcel_shouldReturnCountsConsistent_whenHeaderOnly() {
        byte[] bytes =
                ExcelTestHelper.workbookBytes(
                        "类型定义",
                        "hint",
                        List.of("类型ID", "归属命令ID", "类型名称", "类型枚举", "占用序号"),
                        List.of());

        var out = newSvc().importExcel("p1", bytes);

        assertThat(out.getTotalRows()).isGreaterThanOrEqualTo(0);
        assertThat(out.getSuccessCount()).isGreaterThanOrEqualTo(0);
        assertThat(out.getFailureCount()).isGreaterThanOrEqualTo(0);
        assertThat(out.getSuccessCount() + out.getFailureCount()).isLessThanOrEqualTo(out.getTotalRows());
    }

    @Test
    void exportAndTemplate_shouldReturnBytes() {
        when(repo.pageByProduct(eq("p1"), eq(1), eq(10), eq(null)))
                .thenReturn(new PageSlice<>(List.of(), 0, 1, 10));

        assertThat(newSvc().exportExcel("p1", 1, 10, null)).isNotEmpty();
        assertThat(newSvc().importTemplate()).isNotEmpty();
    }
}

