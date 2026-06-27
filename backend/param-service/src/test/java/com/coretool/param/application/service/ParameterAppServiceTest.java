/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.application.service;

import com.coretool.param.application.support.ParameterExportHeadersZh;
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
import com.coretool.param.infrastructure.util.ExcelHelper;
import com.coretool.param.infrastructure.util.ExcelInstructions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 参数核心应用服务单元测试。
 *
 * @since 2026-04-28
 */
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

    /**
     * 返回与导入模板一致的中文表头列名（含变更说明列）。
     *
     * @return 表头列名列表
     */
    private static List<String> alignedImportHeadersZh() {
        return ParameterExportHeadersZh.listForImport();
    }

    private static byte[] validImportWorkbookBytesWithHeaderOnly() {
        return ExcelTestHelper.workbookBytes(
                "parameters",
                ExcelInstructions.parameterImportExportInstructionLines(),
                alignedImportHeadersZh(),
                List.of());
    }

    private static List<String> emptyRow(List<String> headers) {
        List<String> row = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            row.add("");
        }
        return row;
    }

    private static List<String> minimalCreateRow(List<String> headers) {
        List<String> row = emptyRow(headers);
        row.set(headers.indexOf("参数编码"), "BIT_1");
        row.set(headers.indexOf("参数名称（中）"), "参数中文");
        row.set(headers.indexOf("BIT 占用"), "1");
        row.set(headers.indexOf("参数默认值"), "0");
        row.set(headers.indexOf("参数推荐值"), "0");
        row.set(headers.indexOf("引入版本"), "V1");
        row.set(headers.indexOf("单位（中文）"), "个");
        row.set(headers.indexOf("取值范围"), "0-255");
        return row;
    }

    @Test
    void importParameters_shouldValidateMode() {
        ParameterAppService svc = newSvc();
        byte[] sheet = validImportWorkbookBytesWithHeaderOnly();

        assertThatThrownBy(() -> svc.importParameters("p1", "v1", "BAD", "c1", null, sheet))
                .isInstanceOf(DomainRuleException.class)
                .hasMessageContaining("mode");
    }

    @Test
    void importParameters_shouldRejectEmptyFileBytes() {
        ParameterAppService svc = newSvc();

        assertThatThrownBy(() -> svc.importParameters("p1", "v1", "FULL", "c1", null, new byte[0]))
                .isInstanceOf(DomainRuleException.class)
                .hasMessageContaining("文件为空");
    }

    @Test
    void export_shouldReturnWorkbookBytes_whenNoRows() {
        when(systemParameterMapper.selectList(any())).thenReturn(List.of());

        byte[] out = newSvc().export("p1", "v1", null, null);
        assertThat(out).isNotEmpty();
    }

    @Test
    void export_shouldIncludeAlignedHeaders() {
        when(systemParameterMapper.selectList(any())).thenReturn(List.of());

        ExcelHelper.ParsedSheet sheet = ExcelHelper.parseFirstSheet(newSvc().export("p1", "v1", null, null));
        Map<String, Integer> hi =
                ExcelHelper.headerIndex(
                        sheet.rows()
                                .get(ExcelHelper.detectHeaderRowIndex(sheet.rows(), "参数ID", "参数编码")));
        assertThat(hi).containsKeys("取值范围", "是否发布", "单位（中文）", "产品形态ID");
        assertThat(hi).doesNotContainKeys("取值区间", "变更类型", "立即生效", "变更来源", "枚举值（中）", "参数范围");
    }

    @Test
    void importParameters_full_shouldInsertOneRow_whenSingleValidLine() {
        lenient().when(changeSourceKeywordRepository.listEnabledRegexesByProduct("p1")).thenReturn(List.of());
        lenient().doNothing().when(configChangeTypeAppService).validateChangeTypesForParameterSave(any(Boolean.class), any());

        when(systemParameterMapper.selectList(any())).thenReturn(List.of());

        lenient().doAnswer(inv -> {
            SystemParameterPo po = inv.getArgument(0);
            if (po.getParameterId() == null) {
                po.setParameterId(1);
            }
            return 1;
        }).when(systemParameterMapper).insert(any(SystemParameterPo.class));

        List<String> headers = alignedImportHeadersZh();
        List<String> row = minimalCreateRow(headers);
        byte[] bytes =
                ExcelTestHelper.workbookBytes(
                        "parameters",
                        ExcelInstructions.parameterImportExportInstructionLines(),
                        headers,
                        List.of(row));

        var out = newSvc().importParameters("p1", "v1", "FULL", "c1", "BIT", bytes);

        assertThat(out.getTotalRows()).isGreaterThanOrEqualTo(1);
        assertThat(out.getSuccessCount()).isGreaterThanOrEqualTo(1);
        verify(systemParameterMapper, atLeast(2)).selectList(any());
    }

    @Test
    void importParameters_createMissingRequired_shouldFailRow() {
        lenient().when(changeSourceKeywordRepository.listEnabledRegexesByProduct("p1")).thenReturn(List.of());
        when(systemParameterMapper.selectList(any())).thenReturn(List.of());

        List<String> headers = alignedImportHeadersZh();
        List<String> row = emptyRow(headers);
        row.set(headers.indexOf("参数编码"), "BIT_1");
        row.set(headers.indexOf("参数名称（中）"), "参数中文");
        byte[] bytes =
                ExcelTestHelper.workbookBytes(
                        "parameters",
                        ExcelInstructions.parameterImportExportInstructionLines(),
                        headers,
                        List.of(row));

        var out = newSvc().importParameters("p1", "v1", "FULL", "c1", "BIT", bytes);

        assertThat(out.getFailureCount()).isGreaterThanOrEqualTo(1);
        assertThat(out.getFailures().get(0).getReason()).contains("必填");
    }

    @Test
    void importParameters_oldValueRangeText_shouldParseSegments() {
        lenient().when(changeSourceKeywordRepository.listEnabledRegexesByProduct("p1")).thenReturn(List.of());
        lenient().doNothing().when(configChangeTypeAppService).validateChangeTypesForParameterSave(any(Boolean.class), any());
        when(systemParameterMapper.selectList(any())).thenReturn(List.of());
        lenient().doAnswer(inv -> {
            SystemParameterPo po = inv.getArgument(0);
            po.setParameterId(1);
            assertThat(po.getValueRange()).isEqualTo("1-10,20-30");
            assertThat(po.getValueRangeSegments()).contains("\"min\":1");
            return 1;
        }).when(systemParameterMapper).insert(any(SystemParameterPo.class));

        List<String> headers = alignedImportHeadersZh();
        List<String> row = minimalCreateRow(headers);
        row.set(headers.indexOf("取值范围"), "1-10,20-30");
        byte[] bytes =
                ExcelTestHelper.workbookBytes(
                        "parameters",
                        ExcelInstructions.parameterImportExportInstructionLines(),
                        headers,
                        List.of(row));

        var out = newSvc().importParameters("p1", "v1", "FULL", "c1", "BIT", bytes);
        assertThat(out.getSuccessCount()).isGreaterThanOrEqualTo(1);
    }

    private static SystemParameterPo existingParameterForHiddenFieldMerge() {
        SystemParameterPo existing = new SystemParameterPo();
        existing.setParameterId(9);
        existing.setParameterCode("BIT_1");
        existing.setOwnedProductId("p1");
        existing.setOwnedVersionId("v1");
        existing.setOwnedCommandId("c1");
        existing.setTakeEffectImmediately("是");
        existing.setChangeSource("legacy-src");
        existing.setPatchVersion("P1");
        existing.setParameterNameCn("旧名");
        existing.setParameterDefaultValue("0");
        existing.setParameterRecommendedValue("0");
        existing.setIntroducedVersion("V0");
        existing.setParameterUnitCn("个");
        existing.setValueRangeSegments("[{\"min\":0,\"max\":255}]");
        existing.setValueRange("0-255");
        existing.setBitUsage("1");
        existing.setValueDescriptionCn("说明");
        existing.setApplicationScenarioCn("场景");
        existing.setApplicableNe("NE1");
        existing.setBusinessClassification("类");
        existing.setCategoryId("cat1");
        existing.setProjectTeam("组");
        existing.setParameterDescriptionCn("含义");
        existing.setImpactDescriptionCn("影响");
        existing.setConfigurationExampleCn("举例");
        existing.setIsPublished("是");
        existing.setFeatureId("f1");
        existing.setPlatformGeneration("裸机形态");
        existing.setApplicationRegion("全球");
        return existing;
    }

    private void stubHiddenFieldMergeUpdate(SystemParameterPo existing) {
        when(systemParameterMapper.selectById(existing.getParameterId())).thenReturn(existing);
        when(systemParameterMapper.selectList(any())).thenReturn(List.of(existing));
        when(systemParameterMapper.updateById(any(SystemParameterPo.class))).thenAnswer(inv -> {
            SystemParameterPo po = inv.getArgument(0);
            assertThat(po.getTakeEffectImmediately()).isEqualTo("是");
            assertThat(po.getChangeSource()).isEqualTo("legacy-src");
            assertThat(po.getPatchVersion()).isEqualTo("P1");
            return 1;
        });
    }

    private static byte[] importWorkbookBytes(List<String> headers, List<String> row) {
        return ExcelTestHelper.workbookBytes(
                "parameters",
                ExcelInstructions.parameterImportExportInstructionLines(),
                headers,
                List.of(row));
    }

    @Test
    void importParameters_update_shouldMergeHiddenFields() {
        lenient().when(changeSourceKeywordRepository.listEnabledRegexesByProduct("p1")).thenReturn(List.of());
        lenient().doNothing().when(configChangeTypeAppService).validateChangeTypesForParameterSave(any(Boolean.class), any());

        SystemParameterPo existing = existingParameterForHiddenFieldMerge();
        stubHiddenFieldMergeUpdate(existing);

        List<String> headers = alignedImportHeadersZh();
        List<String> row = minimalCreateRow(headers);
        row.set(headers.indexOf("参数ID"), "9");
        row.set(headers.indexOf("参数名称（中）"), "新名");

        var out = newSvc().importParameters("p1", "v1", "INCREMENTAL", "c1", "BIT", importWorkbookBytes(headers, row));
        assertThat(out.getSuccessCount()).isGreaterThanOrEqualTo(1);
        verify(systemParameterMapper).updateById(any(SystemParameterPo.class));
    }

    @Test
    void importParameters_updateByParameterId_shouldApplyBitAndValueDescription() {
        lenient().when(changeSourceKeywordRepository.listEnabledRegexesByProduct("p1")).thenReturn(List.of());
        lenient().doNothing().when(configChangeTypeAppService).validateChangeTypesForParameterSave(any(Boolean.class), any());

        SystemParameterPo existing = existingParameterForHiddenFieldMerge();
        when(systemParameterMapper.selectById(existing.getParameterId())).thenReturn(existing);
        when(systemParameterMapper.selectList(any())).thenReturn(List.of(existing));
        when(systemParameterMapper.updateById(any(SystemParameterPo.class))).thenAnswer(inv -> {
            SystemParameterPo po = inv.getArgument(0);
            assertThat(po.getParameterNameCn()).isEqualTo("新名称");
            assertThat(po.getValueDescriptionCn()).isEqualTo("新取值说明");
            return 1;
        });

        List<String> headers = alignedImportHeadersZh();
        List<String> row = minimalCreateRow(headers);
        row.set(headers.indexOf("参数ID"), "9");
        row.set(headers.indexOf("参数名称（中）"), "新名称");
        row.set(headers.indexOf("取值说明（中）"), "新取值说明");

        var out = newSvc().importParameters("p1", "v1", "INCREMENTAL", "c1", "BIT", importWorkbookBytes(headers, row));
        assertThat(out.getFailures()).as("%s", out.getFailures()).isEmpty();
        assertThat(out.getSuccessCount()).isEqualTo(1);
        verify(systemParameterMapper).updateById(any(SystemParameterPo.class));
    }

    @Test
    void importParameters_withoutCommandId_shouldResolveOwnedCommandFromFile() {
        lenient().when(changeSourceKeywordRepository.listEnabledRegexesByProduct("p1")).thenReturn(List.of());
        lenient().doNothing().when(configChangeTypeAppService).validateChangeTypesForParameterSave(any(Boolean.class), any());

        EntityCommandMappingPo cmd = new EntityCommandMappingPo();
        cmd.setOwnedProductId("p1");
        cmd.setCommandId("c1");
        cmd.setCommandName("CMD");
        when(entityCommandMappingMapper.selectList(any())).thenReturn(List.of(cmd));
        when(systemParameterMapper.selectList(any())).thenReturn(List.of());
        when(systemParameterMapper.insert(any(SystemParameterPo.class))).thenAnswer(inv -> {
            SystemParameterPo po = inv.getArgument(0);
            po.setParameterId(1);
            assertThat(po.getOwnedCommandId()).isEqualTo("c1");
            return 1;
        });

        List<String> headers = alignedImportHeadersZh();
        List<String> row = minimalCreateRow(headers);
        row.set(headers.indexOf("归属命令"), "CMD");

        var out = newSvc().importParameters("p1", "v1", "FULL", null, "BIT", importWorkbookBytes(headers, row));
        assertThat(out.getSuccessCount()).isEqualTo(1);
    }

    @Test
    void export_shouldFillCommandNameMap_whenCommandsPresent() {
        SystemParameterPo p = new SystemParameterPo();
        p.setOwnedProductId("p1");
        p.setOwnedVersionId("v1");
        p.setOwnedCommandId("c1");
        p.setParameterId(1);
        p.setParameterCode("BIT_1");
        p.setValueRangeSegments("[{\"min\":1,\"max\":3}]");
        p.setValueRange("1-3");
        p.setIsPublished("是");
        p.setPlatformGeneration("裸机形态");

        when(systemParameterMapper.selectList(any())).thenReturn(List.of(p));

        EntityCommandMappingPo cmd = new EntityCommandMappingPo();
        cmd.setCommandId("c1");
        cmd.setCommandName("CMD");
        when(entityCommandMappingMapper.selectList(any())).thenReturn(List.of(cmd));

        ExcelHelper.ParsedSheet sheet = ExcelHelper.parseFirstSheet(newSvc().export("p1", "v1", "c1", null));
        int headerIdx = ExcelHelper.detectHeaderRowIndex(sheet.rows(), "参数ID", "参数编码");
        List<String> dataRow = sheet.rows().get(headerIdx + 1);
        Map<String, Integer> hi = ExcelHelper.headerIndex(sheet.rows().get(headerIdx));
        assertThat(dataRow.get(hi.get("取值范围"))).isEqualTo("1-3");
        assertThat(hi).doesNotContainKey("变更类型");
        assertThat(hi).doesNotContainKey("取值区间");
        assertThat(dataRow.get(hi.get("是否发布"))).isEqualTo("是");
        assertThat(dataRow.get(hi.get("归属命令"))).isEqualTo("CMD");
    }
}
