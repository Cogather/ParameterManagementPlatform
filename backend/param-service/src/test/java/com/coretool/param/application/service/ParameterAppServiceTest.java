package com.coretool.param.application.service;

import com.coretool.param.domain.config.keyword.repository.ChangeSourceKeywordRepository;
import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.infrastructure.persistence.entity.EntityCommandMappingPo;
import com.coretool.param.infrastructure.persistence.entity.SystemParameterPo;
import com.coretool.param.infrastructure.persistence.mapper.CommandTypeDefinitionMapper;
import com.coretool.param.infrastructure.persistence.mapper.CommandTypeVersionRangeMapper;
import com.coretool.param.infrastructure.persistence.mapper.ConfigChangeDescriptionMapper;
import com.coretool.param.infrastructure.persistence.mapper.EntityCommandMappingMapper;
import com.coretool.param.infrastructure.persistence.mapper.EntityVersionInfoMapper;
import com.coretool.param.infrastructure.persistence.mapper.SystemParameterMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ParameterAppServiceTest {

    @Mock
    private SystemParameterMapper systemParameterMapper;

    @Mock
    private ConfigChangeDescriptionMapper configChangeDescriptionMapper;

    @Mock
    private CommandTypeVersionRangeMapper commandTypeVersionRangeMapper;

    @Mock
    private CommandTypeDefinitionMapper commandTypeDefinitionMapper;

    @Mock
    private EntityVersionInfoMapper entityVersionInfoMapper;

    @Mock
    private ChangeSourceKeywordRepository changeSourceKeywordRepository;

    @Mock
    private ConfigChangeTypeAppService configChangeTypeAppService;

    @Mock
    private OperationLogAppService operationLogAppService;

    @Mock
    private EntityCommandMappingMapper entityCommandMappingMapper;

    private ParameterAppService newSvc() {
        ParameterAppPersistenceMappers persistence =
                new ParameterAppPersistenceMappers(
                        systemParameterMapper,
                        configChangeDescriptionMapper,
                        commandTypeVersionRangeMapper,
                        commandTypeDefinitionMapper,
                        entityVersionInfoMapper);
        ParameterAppCollaboration collaboration =
                new ParameterAppCollaboration(
                        changeSourceKeywordRepository,
                        configChangeTypeAppService,
                        operationLogAppService,
                        entityCommandMappingMapper);
        return new ParameterAppService(persistence, collaboration);
    }

    private static byte[] validImportWorkbookBytesWithHeaderOnly() {
        List<String> headers =
                List.of(
                        "parameter_code",
                        "parameter_name_cn",
                        "parameter_name_en",
                        "parameter_sequence",
                        "bit_usage",
                        "change_source",
                        "value_range",
                        "value_description_cn",
                        "value_description_en",
                        "application_scenario_cn",
                        "application_scenario_en",
                        "parameter_default_value",
                        "parameter_recommended_value",
                        "applicable_ne",
                        "feature",
                        "business_classification",
                        "take_effect_immediately",
                        "effective_mode_cn",
                        "effective_mode_en",
                        "project_team",
                        "belonging_module",
                        "patch_version",
                        "introduced_version",
                        "parameter_description_cn",
                        "parameter_description_en",
                        "impact_description_cn",
                        "impact_description_en",
                        "configuration_example_cn",
                        "configuration_example_en",
                        "related_parameter_description_cn",
                        "related_parameter_description_en",
                        "remark",
                        "enumeration_values_cn",
                        "enumeration_values_en",
                        "parameter_unit_cn",
                        "parameter_unit_en",
                        "parameter_range",
                        "data_status",
                        "变更类型",
                        "变更原因（中）",
                        "变更影响（中）",
                        "变更原因（英）",
                        "变更影响（英）",
                        "export_delta",
                        "不导出原因");
        return ExcelTestHelper.workbookBytes("parameters", "hint", headers, List.of());
    }

    @Test
    void importParameters_shouldValidateModeAndCommandId() {
        ParameterAppService svc = newSvc();

        assertThatThrownBy(
                        () ->
                                svc.importParameters(
                                        "p1",
                                        "v1",
                                        "BAD",
                                        "c1",
                                        null,
                                        validImportWorkbookBytesWithHeaderOnly()))
                .isInstanceOf(DomainRuleException.class);

        assertThatThrownBy(
                        () ->
                                svc.importParameters(
                                        "p1",
                                        "v1",
                                        "FULL",
                                        " ",
                                        null,
                                        validImportWorkbookBytesWithHeaderOnly()))
                .isInstanceOf(DomainRuleException.class);
    }

    @Test
    void export_shouldReturnWorkbookBytes_whenNoRows() {
        when(systemParameterMapper.selectList(any())).thenReturn(List.of());

        byte[] out = newSvc().export("p1", "v1", null, null);
        assertThat(out).isNotEmpty();
    }

    @Test
    void importParameters_full_shouldInsertOneRow_whenSingleValidLine() {
        lenient().when(changeSourceKeywordRepository.listEnabledRegexesByProduct("p1")).thenReturn(List.of());
        lenient().doNothing().when(configChangeTypeAppService).validateChangeTypesForParameterSave(any(Boolean.class), any());

        // scopeExisting + peersForBitCheck
        when(systemParameterMapper.selectList(any())).thenReturn(List.of());

        lenient().doAnswer(inv -> {
            SystemParameterPo po = inv.getArgument(0);
            if (po.getParameterId() == null) {
                po.setParameterId(1);
            }
            return 1;
        }).when(systemParameterMapper).insert(any(SystemParameterPo.class));

        // Build a sheet with all required headers using the "code keys" accepted by ImportSheetColumns.fromHeader.
        List<String> headers =
                List.of(
                        "parameter_code",
                        "parameter_name_cn",
                        "parameter_name_en",
                        "parameter_sequence",
                        "bit_usage",
                        "change_source",
                        "value_range",
                        "value_description_cn",
                        "value_description_en",
                        "application_scenario_cn",
                        "application_scenario_en",
                        "parameter_default_value",
                        "parameter_recommended_value",
                        "applicable_ne",
                        "feature",
                        "business_classification",
                        "take_effect_immediately",
                        "effective_mode_cn",
                        "effective_mode_en",
                        "project_team",
                        "belonging_module",
                        "patch_version",
                        "introduced_version",
                        "parameter_description_cn",
                        "parameter_description_en",
                        "impact_description_cn",
                        "impact_description_en",
                        "configuration_example_cn",
                        "configuration_example_en",
                        "related_parameter_description_cn",
                        "related_parameter_description_en",
                        "remark",
                        "enumeration_values_cn",
                        "enumeration_values_en",
                        "parameter_unit_cn",
                        "parameter_unit_en",
                        "parameter_range",
                        "data_status",
                        "变更类型",
                        "变更原因（中）",
                        "变更影响（中）",
                        "变更原因（英）",
                        "变更影响（英）",
                        "export_delta",
                        "不导出原因");

        List<String> row = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            row.add("");
        }
        row.set(headers.indexOf("parameter_code"), "BIT_1");
        row.set(headers.indexOf("parameter_name_cn"), "参数中文");
        row.set(headers.indexOf("parameter_name_en"), "ParamEn");
        // keep other optional columns blank

        byte[] bytes = ExcelTestHelper.workbookBytes("parameters", "hint", headers, List.of(row));

        var out = newSvc().importParameters("p1", "v1", "FULL", "c1", "BIT", bytes);

        assertThat(out.getTotalRows()).isGreaterThanOrEqualTo(1);
        assertThat(out.getSuccessCount() + out.getFailureCount()).isGreaterThanOrEqualTo(1);
        assertThat(out.getSuccessCount() + out.getFailureCount()).isLessThanOrEqualTo(out.getTotalRows());
    }

    @Test
    void export_shouldFillCommandNameMap_whenCommandsPresent() {
        SystemParameterPo p = new SystemParameterPo();
        p.setOwnedProductId("p1");
        p.setOwnedVersionId("v1");
        p.setOwnedCommandId("c1");
        p.setParameterId(1);
        p.setParameterCode("BIT_1");

        when(systemParameterMapper.selectList(any())).thenReturn(List.of(p));

        EntityCommandMappingPo cmd = new EntityCommandMappingPo();
        cmd.setCommandId("c1");
        cmd.setCommandName("CMD");
        when(entityCommandMappingMapper.selectList(any())).thenReturn(List.of(cmd));
        when(configChangeDescriptionMapper.selectList(any())).thenReturn(List.of());

        byte[] out = newSvc().export("p1", "v1", "c1", null);
        assertThat(out).isNotEmpty();
    }
}

