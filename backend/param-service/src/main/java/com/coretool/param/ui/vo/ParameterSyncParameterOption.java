/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.ui.vo;

import lombok.Data;

/**
 * 参数同步：可选参数行（展示用）。
 *
 * @since 2026-05-21
 */
@Data
public class ParameterSyncParameterOption {
    private Integer sourceParameterId;
    private String parameterNameCn;
    private String dataStatus;
}
