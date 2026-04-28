package com.coretool.param.ui.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogGroupKey {
    private String productId;
    private String bizTable;
    private String ownedVersionId;
    private String resourceId;
    private String operationType;
    private String operatorId;
    private LocalDateTime operatedAt;
    private String logBatchId;
}

