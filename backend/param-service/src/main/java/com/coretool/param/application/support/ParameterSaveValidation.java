/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.application.support;

import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.infrastructure.persistence.entity.SystemParameterPo;

import org.apache.commons.lang3.StringUtils;

/**
 * 参数保存分阶段必填校验（新增仅基础信息，编辑叠加详细信息）。
 *
 * @since 2026-06-11
 */
public final class ParameterSaveValidation {

    private static final String NO_PUBLISH = "否";

    private ParameterSaveValidation() {}

    /**
     * 新增参数：基础信息 + 取值区间。
     *
     * @param main           参数主表
     * @param bitCountPositive 当前类型 bit_count 是否大于 0
     * @throws DomainRuleException 校验失败时
     */
    public static void assertCreate(SystemParameterPo main, boolean bitCountPositive) {
        assertBasic(main, bitCountPositive);
        ValueRangeSegmentsSupport.applyToParameter(main.getValueRangeSegments(), main);
    }

    /**
     * 更新参数：基础信息 + 详细信息 + 取值区间。
     *
     * @param main           参数主表
     * @param bitCountPositive 当前类型 bit_count 是否大于 0
     * @throws DomainRuleException 校验失败时
     */
    public static void assertUpdate(SystemParameterPo main, boolean bitCountPositive) {
        assertBasic(main, bitCountPositive);
        assertDetail(main);
        ValueRangeSegmentsSupport.applyToParameter(main.getValueRangeSegments(), main);
    }

    /**
     * 编辑时保留页面隐藏字段的原值。
     *
     * @param incoming 请求体
     * @param existing 库中记录
     */
    public static void mergeHiddenFields(SystemParameterPo incoming, SystemParameterPo existing) {
        incoming.setTakeEffectImmediately(existing.getTakeEffectImmediately());
        incoming.setChangeSource(existing.getChangeSource());
        incoming.setPatchVersion(existing.getPatchVersion());
    }

    private static void assertBasic(SystemParameterPo main, boolean bitCountPositive) {
        requireText(main.getParameterNameCn(), "参数名称（中文）");
        requireText(main.getOwnedCommandId(), "归属命令");
        requireText(main.getParameterCode(), "参数编码");
        if (main.getParameterSequence() == null) {
            throw new DomainRuleException("序号必填");
        }
        requireText(main.getParameterDefaultValue(), "参数默认值");
        assertInteger(main.getParameterDefaultValue(), "参数默认值");
        requireText(main.getParameterRecommendedValue(), "参数推荐值");
        assertInteger(main.getParameterRecommendedValue(), "参数推荐值");
        requireText(main.getIntroducedVersion(), "引入版本");
        requireText(main.getParameterUnitCn(), "单位（中文）");
        if (bitCountPositive && StringUtils.isBlank(main.getBitUsage())) {
            throw new DomainRuleException("使用 BIT 位必填");
        }
        if (StringUtils.isBlank(main.getValueRangeSegments())) {
            throw new DomainRuleException("取值区间至少 1 段");
        }
    }

    private static void assertDetail(SystemParameterPo main) {
        requireText(main.getValueDescriptionCn(), "取值说明（中文）");
        requireText(main.getApplicationScenarioCn(), "应用场景（中文）");
        requireText(main.getApplicableNe(), "适用网元");
        requireText(main.getBusinessClassification(), "业务分类");
        requireText(main.getCategoryId(), "业务分类");
        requireText(main.getProjectTeam(), "项目组");
        requireText(main.getParameterDescriptionCn(), "参数含义（中文）");
        requireText(main.getImpactDescriptionCn(), "影响说明（中文）");
        requireText(main.getConfigurationExampleCn(), "配置举例（中文）");
        requireText(main.getIsPublished(), "是否发布");
        requireText(main.getFeatureId(), "所属特性");
        requireText(main.getPlatformGeneration(), "平台代际");
        requireText(main.getApplicationRegion(), "应用区域");
        if (NO_PUBLISH.equals(StringUtils.trim(main.getIsPublished()))) {
            requireText(main.getNoPublishReason(), "不发布原因");
        }
    }

    private static void requireText(String v, String label) {
        if (StringUtils.isBlank(v)) {
            throw new DomainRuleException(label + "必填");
        }
    }

    private static void assertInteger(String v, String label) {
        if (!StringUtils.isBlank(v) && !v.trim().matches("-?\\d+")) {
            throw new DomainRuleException(label + "须为整数");
        }
    }
}
