/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.ui.vo;

import lombok.Data;

/**
 * 参数同步：源版本下可选「命令 + 类型」。
 *
 * @since 2026-05-21
 */
@Data
public class ParameterSyncTypeOption {
    private String commandId;
    private String commandName;
    private String commandTypeId;
    private String commandTypeName;
}
