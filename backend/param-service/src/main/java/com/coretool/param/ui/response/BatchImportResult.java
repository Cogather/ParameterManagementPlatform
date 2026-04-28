package com.coretool.param.ui.response;

import lombok.Data;

import java.util.List;

@Data
public class BatchImportResult {
    private int totalRows;
    private int successCount;
    private int failureCount;
    private List<Integer> successRowNumbers;
    private List<BatchImportFailure> failures;

    @Data
    public static class BatchImportFailure {
        private int rowNumber;
        private String reason;
    }
}

