package com.coretool.param.application.service;

import com.coretool.param.domain.config.version.ProductVersion;
import com.coretool.param.domain.config.version.repository.ProductVersionRepository;
import com.coretool.param.domain.config.versionfeature.repository.VersionFeatureRepository;
import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.infrastructure.persistence.entity.VersionFeatureDictPo;
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

@ExtendWith(MockitoExtension.class)
class VersionFeatureAppServiceTest {

    @Mock
    private VersionFeatureRepository featureRepository;

    @Mock
    private ProductVersionRepository productVersionRepository;

    @Mock
    private OperationLogAppService operationLogAppService;

    private VersionFeatureAppService newSvc() {
        VersionFeatureAppService svc = new VersionFeatureAppService();
        ReflectionTestUtils.setField(svc, "featureRepository", featureRepository);
        ReflectionTestUtils.setField(svc, "productVersionRepository", productVersionRepository);
        ReflectionTestUtils.setField(svc, "operationLogAppService", operationLogAppService);
        return svc;
    }

    @Test
    void page_shouldReturnPagingFields_whenVersionOwnedAndRepositoryReturnsSlice() {
        ProductVersion v = org.mockito.Mockito.mock(ProductVersion.class);
        when(v.belongsToProduct("p1")).thenReturn(true);
        when(productVersionRepository.findById("v1")).thenReturn(Optional.of(v));

        when(featureRepository.pageByProductAndVersion(eq("p1"), eq("v1"), eq(1), eq(10), eq("k")))
                .thenReturn(new PageSlice<>(List.of(), 0, 1, 10));

        var out = newSvc().page("p1", "v1", ListPageWithKeywordQuery.of(1, 10, "k"));

        assertThat(out.getPage()).isEqualTo(1);
        assertThat(out.getSize()).isEqualTo(10);
        assertThat(out.getTotal()).isEqualTo(0);
        assertThat(out.getRecords()).isEmpty();
    }

    @Test
    void create_shouldThrow_whenInputNull() {
        assertThatThrownBy(() -> newSvc().create("p1", "v1", null))
                .isInstanceOf(DomainRuleException.class);
    }

    @Test
    void create_shouldInsert_whenNew() {
        ProductVersion v = org.mockito.Mockito.mock(ProductVersion.class);
        when(v.belongsToProduct("p1")).thenReturn(true);
        when(productVersionRepository.findById("v1")).thenReturn(Optional.of(v));

        when(featureRepository.findDisabledByNameCnInScope(eq("p1"), eq("v1"), any())).thenReturn(Optional.empty());
        when(featureRepository.existsSameFeatureNameCnInScope(eq("p1"), eq("v1"), any(), eq(null))).thenReturn(false);
        doNothing().when(featureRepository).insert(any());

        VersionFeatureDictPo in = new VersionFeatureDictPo();
        in.setFeatureNameCn("特性A");
        in.setFeatureNameEn("FeatureA");
        in.setCreatorId("u1");
        in.setIntroduceType("INTRODUCE");

        VersionFeatureDictPo out = newSvc().create("p1", "v1", in);

        assertThat(out.getFeatureId()).isNotBlank();
        verify(featureRepository).insert(any());
    }

    @Test
    void importCsv_shouldReturnCountsConsistent_whenHeaderOnly() {
        ProductVersion v = org.mockito.Mockito.mock(ProductVersion.class);
        when(v.belongsToProduct("p1")).thenReturn(true);
        when(productVersionRepository.findById("v1")).thenReturn(Optional.of(v));

        byte[] bytes =
                ExcelTestHelper.workbookBytes(
                        "版本特性",
                        "hint",
                        List.of("ID", "中文名称", "英文名称", "引入类型", "继承/引用版本 ID"),
                        List.of());

        var out = newSvc().importCsv("p1", "v1", bytes);

        assertThat(out.getTotalRows()).isGreaterThanOrEqualTo(0);
        assertThat(out.getSuccessCount()).isGreaterThanOrEqualTo(0);
        assertThat(out.getFailureCount()).isGreaterThanOrEqualTo(0);
        assertThat(out.getSuccessCount() + out.getFailureCount()).isLessThanOrEqualTo(out.getTotalRows());
    }

    @Test
    void exportAndTemplate_shouldReturnBytes() {
        ProductVersion v = org.mockito.Mockito.mock(ProductVersion.class);
        when(v.belongsToProduct("p1")).thenReturn(true);
        when(productVersionRepository.findById("v1")).thenReturn(Optional.of(v));

        when(featureRepository.pageByProductAndVersion(eq("p1"), eq("v1"), eq(1), eq(10), eq(null)))
                .thenReturn(new PageSlice<>(List.of(), 0, 1, 10));

        assertThat(newSvc().exportCsv("p1", "v1", 1, 10)).isNotEmpty();
        assertThat(newSvc().templateCsv()).isNotEmpty();
    }
}

