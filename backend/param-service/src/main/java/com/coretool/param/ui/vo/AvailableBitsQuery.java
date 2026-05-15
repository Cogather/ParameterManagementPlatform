/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.ui.vo;

import lombok.Data;

/**
 * 请求/查询视图对象「AvailableBitsQuery」。
 *
 * @since 2026-04-28
 */

@Data
public class AvailableBitsQuery {
    private String commandId;
    private String commandTypeId;
    private String commandTypeCode;
    private int sequence;
}
