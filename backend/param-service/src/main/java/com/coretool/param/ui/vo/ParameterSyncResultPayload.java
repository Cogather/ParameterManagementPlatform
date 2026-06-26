/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.ui.vo;

import java.util.List;

import lombok.Data;

/**
 * 参数同步批量执行结果载荷（{@code POST .../parameter-sync/commands} 的 data）。
 *
 * @since 2026-05-21
 */
@Data
public class ParameterSyncResultPayload {

    /**
     * 成功条数
     */
    private int successCount;

    /**
     * 失败条数
     */
    private int failureCount;

    /**
     * 失败明细
     */
    private List<ParameterSyncFailureItem> failures;

    /**
     * 成功明细
     */
    private List<ParameterSyncSuccessItem> successes;

    /**
     * 单条参数同步失败信息。
     *
     * @since 2026-05-21
     */
    @Data
    public static class ParameterSyncFailureItem {

        /**
         * 源版本中的参数 ID
         */
        private Integer sourceParameterId;

        /**
         * 参数名称（中文），便于前端展示
         */
        private String parameterNameCn;

        /**
         * 失败原因（可读文案）
         */
        private String reason;
    }

    /**
     * 单条参数同步成功信息。
     *
     * @since 2026-05-21
     */
    @Data
    public static class ParameterSyncSuccessItem {

        /**
         * 源版本中的参数 ID
         */
        private Integer sourceParameterId;

        /**
         * 目标版本中新建参数的 ID
         */
        private Integer newParameterId;
    }
}
