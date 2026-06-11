/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.ui.vo;

import java.util.List;

import lombok.Data;

/**
 * 参数同步批量结果。
 *
 * @since 2026-05-21
 */
@Data
public class ParameterSyncResultPayload {
    private int successCount;
    private int failureCount;
    private List<ParameterSyncFailureItem> failures;
    private List<ParameterSyncSuccessItem> successes;

    @Data
    public static class ParameterSyncFailureItem {
        private Integer sourceParameterId;
        private String parameterNameCn;
        private String reason;
    }

    @Data
    public static class ParameterSyncSuccessItem {
        private Integer sourceParameterId;
        private Integer newParameterId;
    }
}
