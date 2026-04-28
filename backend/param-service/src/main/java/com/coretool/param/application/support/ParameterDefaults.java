package com.coretool.param.application.support;

import com.coretool.param.infrastructure.persistence.entity.SystemParameterPo;

import org.apache.commons.lang3.StringUtils;

/**
 * 创建参数时，为表 §15 必填列补齐占位默认值（避免 ORM 插入 NULL 违反 NOT NULL）。
 * 前端联调仍应传真实业务值；此处仅作服务端兜底。
 */
public final class ParameterDefaults {

    private ParameterDefaults() {}

    /**
     * 创建参数时补齐必填字段的服务端兜底默认值。
     *
     * @param p 参数持久化对象（为空则直接返回）
     */
    public static void applyForCreate(SystemParameterPo p) {
        if (p == null) {
            return;
        }
        p.setTenantId(blankTo(p.getTenantId(), "default"));
        p.setDataStatus(blankTo(p.getDataStatus(), "Draft"));
        p.setIntroduceType(blankTo(p.getIntroduceType(), "版本新增Version additions"));
        p.setParameterNameCn(blankTo(p.getParameterNameCn(), "待补充"));
        p.setParameterNameEn(blankTo(p.getParameterNameEn(), "TBD"));
        p.setValueRange(blankTo(p.getValueRange(), "0"));
        p.setValueDescriptionCn(blankTo(p.getValueDescriptionCn(), "待补充"));
        p.setValueDescriptionEn(blankTo(p.getValueDescriptionEn(), "TBD"));
        p.setApplicationScenarioCn(blankTo(p.getApplicationScenarioCn(), "待补充"));
        p.setApplicationScenarioEn(blankTo(p.getApplicationScenarioEn(), "TBD"));
        p.setParameterDefaultValue(blankTo(p.getParameterDefaultValue(), "0"));
        p.setApplicableNe(blankTo(p.getApplicableNe(), "默认"));
        p.setBusinessClassification(blankTo(p.getBusinessClassification(), "默认"));
        p.setCategoryId(blankTo(p.getCategoryId(), "default"));
        p.setTakeEffectImmediately(blankTo(p.getTakeEffectImmediately(), "是"));
        p.setProjectTeam(blankTo(p.getProjectTeam(), "默认"));
        p.setIntroducedVersion(blankTo(p.getIntroducedVersion(), "default"));
        p.setParameterDescriptionCn(blankTo(p.getParameterDescriptionCn(), "待补充"));
        p.setParameterDescriptionEn(blankTo(p.getParameterDescriptionEn(), "TBD"));
        p.setImpactDescriptionCn(blankTo(p.getImpactDescriptionCn(), "待补充"));
        p.setImpactDescriptionEn(blankTo(p.getImpactDescriptionEn(), "TBD"));
        p.setHelpDocumentInDatabase(blankTo(p.getHelpDocumentInDatabase(), "否"));
        p.setRelatedParameterCn(blankTo(p.getRelatedParameterCn(), "无"));
        p.setRelatedParameterEn(blankTo(p.getRelatedParameterEn(), "None"));
        p.setRelatedFeatureEn(blankTo(p.getRelatedFeatureEn(), "TBD"));
        p.setChangeFactors(blankTo(p.getChangeFactors(), "AR"));
        p.setChangeRelatedNumber(blankTo(p.getChangeRelatedNumber(), "N/A"));
    }

    private static String blankTo(String v, String def) {
        return StringUtils.isBlank(v) ? def : v;
    }
}
