/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.ui.vo;

import lombok.Data;

/**
 * 参数同步单条选中项。
 *
 * @since 2026-05-21
 */
@Data
public class ParameterSyncItemRequest {
    private Integer sourceParameterId;
    private String commandId;
    private String commandTypeId;
}
