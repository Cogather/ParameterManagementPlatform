package com.coretool.param.ui.controller;

import com.coretool.param.application.service.ApplicableNeAppService;
import com.coretool.param.application.service.BusinessCategoryAppService;
import com.coretool.param.application.service.ChangeSourceKeywordAppService;
import com.coretool.param.application.service.CommandAppService;
import com.coretool.param.application.service.CommandTypeDefinitionAppService;
import com.coretool.param.application.service.CommandTypeVersionRangeAppService;
import com.coretool.param.application.service.EffectiveFormAppService;
import com.coretool.param.application.service.EffectiveModeAppService;
import com.coretool.param.application.service.NfConfigAppService;
import com.coretool.param.application.service.ProjectTeamAppService;
import com.coretool.param.application.service.VersionAppService;
import com.coretool.param.application.service.VersionFeatureAppService;
import com.coretool.param.infrastructure.persistence.entity.CommandTypeDefinitionPo;
import com.coretool.param.infrastructure.persistence.entity.CommandTypeVersionRangePo;
import com.coretool.param.infrastructure.persistence.entity.ConfigChangeSourceKeywordPo;
import com.coretool.param.infrastructure.persistence.entity.EntityApplicableNeDictPo;
import com.coretool.param.infrastructure.persistence.entity.EntityBusinessCategoryPo;
import com.coretool.param.infrastructure.persistence.entity.EntityCommandMappingPo;
import com.coretool.param.infrastructure.persistence.entity.EntityEffectiveFormDictPo;
import com.coretool.param.infrastructure.persistence.entity.EntityEffectiveModeDictPo;
import com.coretool.param.infrastructure.persistence.entity.EntityNfConfigDictPo;
import com.coretool.param.infrastructure.persistence.entity.EntityVersionInfoPo;
import com.coretool.param.infrastructure.persistence.entity.ProjectTeamDictPo;
import com.coretool.param.infrastructure.persistence.entity.VersionFeatureDictPo;
import com.coretool.param.ui.response.PageResponse;
import com.coretool.param.ui.vo.ListPageWithKeywordQuery;
import com.coretool.param.ui.vo.ListPageWithTypeFilterQuery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UiControllerDelegateBatchTest {

    @Mock
    private ApplicableNeAppService applicableNeAppService;
    @Mock
    private BusinessCategoryAppService businessCategoryAppService;
    @Mock
    private ChangeSourceKeywordAppService changeSourceKeywordAppService;
    @Mock
    private EffectiveFormAppService effectiveFormAppService;
    @Mock
    private EffectiveModeAppService effectiveModeAppService;
    @Mock
    private NfConfigAppService nfConfigAppService;
    @Mock
    private ProjectTeamAppService projectTeamAppService;
    @Mock
    private VersionAppService versionAppService;
    @Mock
    private VersionFeatureAppService versionFeatureAppService;
    @Mock
    private CommandTypeVersionRangeAppService rangeAppService;
    @Mock
    private CommandTypeDefinitionAppService typeDefAppService;
    @Mock
    private CommandAppService commandAppService;

    @Test
    void applicableNe_page_shouldDelegate() {
        ApplicableNeController c = new ApplicableNeController(applicableNeAppService);
        ListPageWithKeywordQuery q = ListPageWithKeywordQuery.of(1, 20, "k");
        PageResponse<EntityApplicableNeDictPo> page = new PageResponse<>();
        when(applicableNeAppService.page("p", q)).thenReturn(page);
        assertThat(c.page("p", q).getData()).isSameAs(page);
    }

    @Test
    void businessCategory_page_shouldDelegate() {
        BusinessCategoryController c = new BusinessCategoryController(businessCategoryAppService);
        ListPageWithKeywordQuery q = ListPageWithKeywordQuery.of(1, 20, "k");
        PageResponse<EntityBusinessCategoryPo> page = new PageResponse<>();
        when(businessCategoryAppService.page("p", q)).thenReturn(page);
        assertThat(c.page("p", q).getData()).isSameAs(page);
    }

    @Test
    void changeSourceKeyword_export_shouldAttachFileName() {
        ChangeSourceKeywordController c = new ChangeSourceKeywordController(changeSourceKeywordAppService);
        when(changeSourceKeywordAppService.exportCsv("p", 1, 2)).thenReturn(new byte[] {1});
        assertThat(c.exportCsv("p", 1, 2).getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("change-source-keywords.xlsx");
    }

    @Test
    void effectiveForm_page_shouldDelegate() {
        EffectiveFormController c = new EffectiveFormController(effectiveFormAppService);
        ListPageWithKeywordQuery q = ListPageWithKeywordQuery.of(1, 20, null);
        PageResponse<EntityEffectiveFormDictPo> page = new PageResponse<>();
        when(effectiveFormAppService.page("p", q)).thenReturn(page);
        assertThat(c.page("p", q).getData()).isSameAs(page);
    }

    @Test
    void effectiveMode_page_shouldDelegate() {
        EffectiveModeController c = new EffectiveModeController(effectiveModeAppService);
        ListPageWithKeywordQuery q = ListPageWithKeywordQuery.of(1, 20, null);
        PageResponse<EntityEffectiveModeDictPo> page = new PageResponse<>();
        when(effectiveModeAppService.page("p", q)).thenReturn(page);
        assertThat(c.page("p", q).getData()).isSameAs(page);
    }

    @Test
    void nfConfig_page_shouldDelegate() {
        NfConfigController c = new NfConfigController(nfConfigAppService);
        ListPageWithKeywordQuery q = ListPageWithKeywordQuery.of(1, 20, null);
        PageResponse<EntityNfConfigDictPo> page = new PageResponse<>();
        when(nfConfigAppService.page("p", q)).thenReturn(page);
        assertThat(c.page("p", q).getData()).isSameAs(page);
    }

    @Test
    void projectTeam_page_shouldDelegate() {
        ProjectTeamController c = new ProjectTeamController(projectTeamAppService);
        ListPageWithKeywordQuery q = ListPageWithKeywordQuery.of(1, 20, null);
        PageResponse<ProjectTeamDictPo> page = new PageResponse<>();
        when(projectTeamAppService.page("p", q)).thenReturn(page);
        assertThat(c.page("p", q).getData()).isSameAs(page);
    }

    @Test
    void version_page_and_disable_shouldDelegate() {
        VersionController c = new VersionController(versionAppService);
        ListPageWithKeywordQuery q = ListPageWithKeywordQuery.of(1, 20, null);
        PageResponse<EntityVersionInfoPo> page = new PageResponse<>();
        when(versionAppService.page("p", q)).thenReturn(page);
        assertThat(c.page("p", q).getData()).isSameAs(page);
        c.disable("p", "v1");
        verify(versionAppService).disable("p", "v1");
    }

    @Test
    void versionFeature_page_shouldDelegate() {
        VersionFeatureController c = new VersionFeatureController(versionFeatureAppService);
        ListPageWithKeywordQuery q = ListPageWithKeywordQuery.of(1, 20, null);
        PageResponse<VersionFeatureDictPo> page = new PageResponse<>();
        when(versionFeatureAppService.page("p", "v1", q)).thenReturn(page);
        assertThat(c.page("p", "v1", q).getData()).isSameAs(page);
    }

    @Test
    void commandTypeVersionRange_page_shouldDelegate() {
        CommandTypeVersionRangeController c = new CommandTypeVersionRangeController(rangeAppService);
        ListPageWithTypeFilterQuery q = ListPageWithTypeFilterQuery.of(1, 20, "t1");
        PageResponse<CommandTypeVersionRangePo> page = new PageResponse<>();
        when(rangeAppService.page("p", q)).thenReturn(page);
        assertThat(c.page("p", q).getData()).isSameAs(page);
    }

    @Test
    void commandType_page_shouldDelegate() {
        CommandTypeController c = new CommandTypeController(typeDefAppService);
        ListPageWithKeywordQuery q = ListPageWithKeywordQuery.of(1, 20, "kw");
        PageResponse<CommandTypeDefinitionPo> page = new PageResponse<>();
        when(typeDefAppService.page("p", q)).thenReturn(page);
        assertThat(c.page("p", q).getData()).isSameAs(page);
    }

    @Test
    void command_page_shouldDelegate() {
        CommandController c = new CommandController(commandAppService);
        ListPageWithKeywordQuery q = ListPageWithKeywordQuery.of(1, 20, "kw");
        PageResponse<EntityCommandMappingPo> page = new PageResponse<>();
        when(commandAppService.page("p", q)).thenReturn(page);
        assertThat(c.page("p", q).getData()).isSameAs(page);
    }

    @Test
    void changeSourceKeyword_page_shouldDelegate() {
        ChangeSourceKeywordController c = new ChangeSourceKeywordController(changeSourceKeywordAppService);
        ListPageWithKeywordQuery q = ListPageWithKeywordQuery.of(1, 20, "k");
        PageResponse<ConfigChangeSourceKeywordPo> page = new PageResponse<>();
        when(changeSourceKeywordAppService.page("p", q)).thenReturn(page);
        assertThat(c.page("p", q).getData()).isSameAs(page);
    }
}
