/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.application.support;

import com.coretool.param.infrastructure.persistence.entity.SystemParameterPo;

import org.apache.commons.lang3.StringUtils;

/**
 * 创建参数时仅为系统级字段补齐默认值；业务字段由用户填写或保持 NULL。
 *
 * @since 2026-04-28
 */
public final class ParameterDefaults {

    private ParameterDefaults() {}

    /**
     * 创建参数时补齐系统级默认值。
     *
     * @param p 参数持久化对象（为空则直接返回）
     */
    public static void applySystemDefaults(SystemParameterPo p) {
        if (p == null) {
            return;
        }
        p.setTenantId(blankTo(p.getTenantId(), "default"));
        p.setDataStatus(blankTo(p.getDataStatus(), "Draft"));
        p.setIntroduceType(blankTo(p.getIntroduceType(), "版本新增Version additions"));
    }

    /**
     * 创建参数时补齐系统级默认值（已废弃）。
     *
     * @param p 参数持久化对象
     * @deprecated 使用 {@link #applySystemDefaults(SystemParameterPo)}；不再填充业务占位
     */
    @Deprecated
    public static void applyForCreate(SystemParameterPo p) {
        applySystemDefaults(p);
    }

    private static String blankTo(String v, String def) {
        return StringUtils.isBlank(v) ? def : v;
    }
}
