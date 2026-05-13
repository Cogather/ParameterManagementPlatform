package com.coretool.param.ui.response;

import lombok.Data;

import java.util.List;

/**
 * 批量导入结果载荷：总行数、成功/失败计数、成功行号列表与失败明细。
 *
 * @since 2026-04-28
 */

@Data
public class BatchImportResult {
    private int totalRows;
    private int successCount;
    private int failureCount;
    private List<Integer> successRowNumbers;
    private List<BatchImportFailure> failures;

    /**
     * 单行导入失败信息（行号与原因）。
     *
     * @since 2026-04-28
     */
    @Data
    public static class BatchImportFailure {
        private int rowNumber;
        private String reason;
    }
}
