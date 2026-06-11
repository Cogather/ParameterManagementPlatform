/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.ui.vo;

import java.util.List;

import lombok.Data;

/**
 * 参数同步执行请求。
 *
 * @since 2026-05-21
 */
@Data
public class ParameterSyncCommandRequest {
    private String sourceVersionId;
    private List<ParameterSyncItemRequest> items;
}
