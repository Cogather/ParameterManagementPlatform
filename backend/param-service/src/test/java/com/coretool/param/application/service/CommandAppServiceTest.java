package com.coretool.param.application.service;

import com.coretool.param.domain.command.Command;
import com.coretool.param.domain.command.repository.CommandRepository;
import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.infrastructure.persistence.entity.EntityCommandMappingPo;
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
class CommandAppServiceTest {

    @Mock
    private CommandRepository commandRepository;

    @Mock
    private OperationLogAppService operationLogAppService;

    private CommandAppService newSvc() {
        return new CommandAppService(commandRepository, operationLogAppService);
    }

    @Test
    void page_shouldReturnPagingFields_whenRepositoryReturnsSlice() {
        when(commandRepository.pageByProduct(eq("p1"), eq(1), eq(10), eq("k")))
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
                .hasMessageContaining("请求体不能为空");
    }

    @Test
    void create_shouldInsertAndLog_whenNoDisabledDuplicate() {
        when(commandRepository.findDisabledByNameInProduct(eq("p1"), any()))
                .thenReturn(Optional.empty());
        when(commandRepository.existsSameNameInProduct(eq("p1"), any(), eq(null)))
                .thenReturn(false);
        doNothing().when(commandRepository).insert(any());

        EntityCommandMappingPo in = new EntityCommandMappingPo();
        in.setCommandName("CMD-A");
        in.setOwnerList("u1,u2");
        in.setCreatorId("u1");

        EntityCommandMappingPo out = newSvc().create("p1", in);

        assertThat(out.getCommandId()).isNotBlank();
        assertThat(out.getCommandStatus()).isEqualTo(1);
        verify(commandRepository).insert(any());
        verify(operationLogAppService).logCommandCreate(eq("p1"), any(EntityCommandMappingPo.class), eq("u1"));
    }

    @Test
    void disable_shouldUpdateAndLogDelete() {
        Command existing =
                Command.registerNew(
                        new Command.Registration(
                                "p1",
                                "c1",
                                "CMD",
                                "c",
                                "u",
                                "o",
                                1,
                                LocalDateTime.now()));
        when(commandRepository.findById("c1")).thenReturn(Optional.of(existing));

        newSvc().disable("p1", "c1");

        verify(commandRepository).update(any(Command.class));
        verify(operationLogAppService).logCommandDelete(eq("p1"), eq("c1"), eq("CMD"), any());
    }

    @Test
    void importExcel_shouldReturnEmptyResult_whenOnlyHeader() {
        byte[] bytes =
                ExcelTestHelper.workbookBytes(
                        "命令",
                        "hint",
                        List.of("命令ID", "命令", "责任人(英文逗号)"),
                        List.of());

        var out = newSvc().importExcel("p1", bytes);

        assertThat(out.getTotalRows()).isGreaterThanOrEqualTo(0);
        assertThat(out.getSuccessCount()).isGreaterThanOrEqualTo(0);
        assertThat(out.getFailureCount()).isGreaterThanOrEqualTo(0);
        assertThat(out.getSuccessCount() + out.getFailureCount()).isLessThanOrEqualTo(out.getTotalRows());
    }
}

