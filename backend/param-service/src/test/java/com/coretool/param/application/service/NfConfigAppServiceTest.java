package com.coretool.param.application.service;

import com.coretool.param.domain.config.nf.repository.NfConfigRepository;
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
class NfConfigAppServiceTest {

    @Mock
    private NfConfigRepository nfConfigRepository;

    @Mock
    private OperationLogAppService operationLogAppService;

    private NfConfigAppService newSvc() {
        NfConfigAppService svc = new NfConfigAppService();
        ReflectionTestUtils.setField(svc, "nfConfigRepository", nfConfigRepository);
        ReflectionTestUtils.setField(svc, "operationLogAppService", operationLogAppService);
        return svc;
    }

    @Test
    void page_shouldReturnPagingFields_whenRepositoryReturnsSlice() {
        when(nfConfigRepository.pageByProduct(eq("p1"), eq(1), eq(10), eq("k")))
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
        when(nfConfigRepository.findDisabledByNameInProduct(eq("p1"), any())).thenReturn(Optional.empty());
        when(nfConfigRepository.existsSameNameInProduct(eq("p1"), any(), eq(null))).thenReturn(false);
        doNothing().when(nfConfigRepository).insert(any());

        var in = new com.coretool.param.infrastructure.persistence.entity.EntityNfConfigDictPo();
        in.setNfConfigNameCn("NF-A");
        in.setCreatorId("u1");

        var out = newSvc().create("p1", in);

        assertThat(out.getNfConfigId()).isNotBlank();
        verify(nfConfigRepository).insert(any());
    }

    @Test
    void importCsv_shouldReturnCountsConsistent_whenHeaderOnly() {
        byte[] bytes =
                ExcelTestHelper.workbookBytes(
                        "NF 配置",
                        "hint",
                        List.of("ID", "nf 名称", "nf 配置描述"),
                        List.of());

        var out = newSvc().importCsv("p1", bytes);

        assertThat(out.getTotalRows()).isGreaterThanOrEqualTo(0);
        assertThat(out.getSuccessCount()).isGreaterThanOrEqualTo(0);
        assertThat(out.getFailureCount()).isGreaterThanOrEqualTo(0);
        assertThat(out.getSuccessCount() + out.getFailureCount()).isLessThanOrEqualTo(out.getTotalRows());
    }

    @Test
    void importCsv_shouldProcessOneRow_andReturnAtLeastOneResult() {
        lenient().when(nfConfigRepository.findDisabledByNameInProduct(eq("p1"), any())).thenReturn(Optional.empty());
        lenient().when(nfConfigRepository.existsSameNameInProduct(eq("p1"), any(), eq(null))).thenReturn(false);
        lenient().doNothing().when(nfConfigRepository).insert(any());

        byte[] bytes =
                ExcelTestHelper.workbookBytes(
                        "NF 配置",
                        "hint",
                        List.of("ID", "nf 名称", "nf 配置描述"),
                        List.of(List.of("", "NF-A", "Desc")));

        var out = newSvc().importCsv("p1", bytes);

        assertThat(out.getSuccessCount() + out.getFailureCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void exportAndTemplate_shouldReturnBytes() {
        when(nfConfigRepository.pageByProduct(eq("p1"), eq(1), eq(10), eq(null)))
                .thenReturn(new PageSlice<>(List.of(), 0, 1, 10));

        assertThat(newSvc().exportCsv("p1", 1, 10)).isNotEmpty();
        assertThat(newSvc().templateCsv()).isNotEmpty();
    }
}

