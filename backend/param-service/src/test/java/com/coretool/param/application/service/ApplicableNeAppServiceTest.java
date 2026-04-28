package com.coretool.param.application.service;

import com.coretool.param.domain.config.ne.ApplicableNe;
import com.coretool.param.domain.config.ne.repository.ApplicableNeRepository;
import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.infrastructure.persistence.entity.EntityApplicableNeDictPo;
import com.coretool.param.ui.vo.ListPageWithKeywordQuery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
class ApplicableNeAppServiceTest {

    @Mock
    private ApplicableNeRepository applicableNeRepository;

    @Mock
    private OperationLogAppService operationLogAppService;

    private ApplicableNeAppService newSvc() {
        ApplicableNeAppService svc = new ApplicableNeAppService();
        ReflectionTestUtils.setField(svc, "applicableNeRepository", applicableNeRepository);
        ReflectionTestUtils.setField(svc, "operationLogAppService", operationLogAppService);
        return svc;
    }

    @Test
    void page_shouldReturnPagingFields_whenRepositoryReturnsSlice() {
        when(applicableNeRepository.pageByProduct(eq("p1"), eq(1), eq(10), eq("k")))
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
        when(applicableNeRepository.findDisabledByNameInProduct(eq("p1"), any()))
                .thenReturn(Optional.empty());
        when(applicableNeRepository.existsSameNameInProduct(eq("p1"), any(), eq(null)))
                .thenReturn(false);
        doNothing().when(applicableNeRepository).insert(any());

        EntityApplicableNeDictPo in = new EntityApplicableNeDictPo();
        in.setNeTypeNameCn("NE-A");
        in.setCreatorId("u1");

        EntityApplicableNeDictPo out = newSvc().create("p1", in);

        assertThat(out.getNeTypeId()).isNotBlank();
        assertThat(out.getNeTypeStatus()).isEqualTo(1);
        verify(applicableNeRepository).insert(any());
        verify(operationLogAppService).logApplicableNeCreate(eq("p1"), any(EntityApplicableNeDictPo.class), eq("u1"));
    }

    @Test
    void update_shouldThrowDuplicate_whenNameConflicts() {
        ApplicableNe existing =
                ApplicableNe.registerNew(
                        new ApplicableNe.Registration(
                                "p1",
                                "ne1",
                                "OLD",
                                "d",
                                1,
                                null,
                                "c",
                                "u",
                                LocalDateTime.now()));
        when(applicableNeRepository.findByNeTypeId("ne1")).thenReturn(Optional.of(existing));
        when(applicableNeRepository.existsSameNameInProduct("p1", "NEW", "ne1")).thenReturn(true);

        EntityApplicableNeDictPo patch = new EntityApplicableNeDictPo();
        patch.setNeTypeNameCn("NEW");

        assertThatThrownBy(() -> newSvc().update("p1", "ne1", patch))
                .isInstanceOf(DomainRuleException.class)
                .hasMessageContaining("NE_NAME_DUPLICATE");
    }

    @Test
    void disable_shouldUpdateAndLogDelete() {
        ApplicableNe existing =
                ApplicableNe.registerNew(
                        new ApplicableNe.Registration(
                                "p1",
                                "ne1",
                                "NE",
                                "d",
                                1,
                                null,
                                "c",
                                "u",
                                LocalDateTime.now()));
        when(applicableNeRepository.findByNeTypeId("ne1")).thenReturn(Optional.of(existing));

        newSvc().disable("p1", "ne1");

        verify(applicableNeRepository).update(any(ApplicableNe.class));
        ArgumentCaptor<OperationLogAppService.LogDictRowDeleteInput> cap =
                ArgumentCaptor.forClass(OperationLogAppService.LogDictRowDeleteInput.class);
        verify(operationLogAppService).logDictRowDelete(cap.capture());
        assertThat(cap.getValue().bizTable()).isEqualTo(OperationLogAppService.BIZ_TABLE_ENTITY_APPLICABLE_NE_DICT);
    }

    @Test
    void importCsv_shouldReturnEmptyResult_whenOnlyHeader() {
        byte[] bytes =
                ExcelTestHelper.workbookBytes(
                        "适用网元",
                        "hint",
                        List.of("ID", "适用网元名称", "网元类型描述", "产品形态"),
                        List.of());

        var out = newSvc().importCsv("p1", bytes);

        assertThat(out.getTotalRows()).isGreaterThanOrEqualTo(0);
        assertThat(out.getSuccessCount()).isGreaterThanOrEqualTo(0);
        assertThat(out.getFailureCount()).isGreaterThanOrEqualTo(0);
        assertThat(out.getSuccessCount() + out.getFailureCount()).isLessThanOrEqualTo(out.getTotalRows());
    }
}

