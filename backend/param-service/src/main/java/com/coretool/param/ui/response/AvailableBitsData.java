/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.ui.response;

import lombok.Data;

import java.util.List;

/**
 * GET .../available-bits 的 data（openspec/schemas/available-bits.json）。
 *
 * @since 2026-04-28
 */
@Data
public class AvailableBitsData {

    private int sequence;
    private List<Integer> availableBitIndexes;
}
