package com.coretool.param.application.service;

import com.coretool.param.domain.config.effectivemode.repository.EffectiveModeRepository;
import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.ui.vo.ListPageWithKeywordQuery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class EffectiveModeAppServiceTest {

    @Mock
    private EffectiveModeRepository effectiveModeRepository;

    @Mock
    private OperationLogAppService operationLogAppService;

    private EffectiveModeAppService newSvc() {
        EffectiveModeAppService svc = new EffectiveModeAppService();
        ReflectionTestUtils.setField(svc, "effectiveModeRepository", effectiveModeRepository);
        ReflectionTestUtils.setField(svc, "operationLogAppService", operationLogAppService);
        return svc;
    }

    @Test
    void page_shouldReturnPagingFields_whenRepositoryReturnsSlice() {
        when(effectiveModeRepository.pageByProduct(eq("p1"), eq(1), eq(10), eq("k")))
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
    void create_shouldInsert_whenNew() {
        when(effectiveModeRepository.findDisabledByNameCnInProduct(eq("p1"), any())).thenReturn(Optional.empty());
        doNothing().when(effectiveModeRepository).insert(any());

        var in = new com.coretool.param.infrastructure.persistence.entity.EntityEffectiveModeDictPo();
        in.setEffectiveModeNameCn("方式A");
        in.setEffectiveModeNameEn("ModeA");
        in.setCreatorId("u1");

        var out = newSvc().create("p1", in);

        assertThat(out.getEffectiveModeId()).isNotBlank();
        verify(effectiveModeRepository).insert(any());
    }

    @Test
    void importCsv_shouldReturnCountsConsistent_whenHeaderOnly() {
        byte[] bytes =
                ExcelTestHelper.workbookBytes(
                        "生效方式",
                        "hint",
                        List.of("ID", "生效方式（中文）", "生效方式（英文）", "生效方式描述"),
                        List.of());

        var out = newSvc().importCsv("p1", bytes);

        assertThat(out.getTotalRows()).isGreaterThanOrEqualTo(0);
        assertThat(out.getSuccessCount()).isGreaterThanOrEqualTo(0);
        assertThat(out.getFailureCount()).isGreaterThanOrEqualTo(0);
        assertThat(out.getSuccessCount() + out.getFailureCount()).isLessThanOrEqualTo(out.getTotalRows());
    }

    @Test
    void importCsv_shouldProcessOneRow_andReturnAtLeastOneResult() {
        lenient().when(effectiveModeRepository.findDisabledByNameCnInProduct(eq("p1"), any())).thenReturn(Optional.empty());
        lenient().doNothing().when(effectiveModeRepository).insert(any());

        byte[] bytes =
                ExcelTestHelper.workbookBytes(
                        "生效方式",
                        "hint",
                        List.of("ID", "生效方式（中文）", "生效方式（英文）", "生效方式描述"),
                        List.of(List.of("", "方式A", "ModeA", "Desc")));

        var out = newSvc().importCsv("p1", bytes);

        assertThat(out.getSuccessCount() + out.getFailureCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void exportAndTemplate_shouldReturnBytes() {
        when(effectiveModeRepository.pageByProduct(eq("p1"), eq(1), eq(10), eq(null)))
                .thenReturn(new PageSlice<>(List.of(), 0, 1, 10));

        assertThat(newSvc().exportCsv("p1", 1, 10)).isNotEmpty();
        assertThat(newSvc().templateCsv()).isNotEmpty();
    }
}

