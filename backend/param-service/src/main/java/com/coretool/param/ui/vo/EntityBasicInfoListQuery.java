/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.ui.vo;

import lombok.Data;

/**
 * 产品主数据全路径分页：keyword / productId 均可选。
 *
 * @since 2026-04-28
 */
@Data
public class EntityBasicInfoListQuery {
    private int page = 1;
    private int size = 20;
    private String keyword;
    private String productId;
}
