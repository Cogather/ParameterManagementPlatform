package com.coretool.param.ui.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 请求/查询视图对象「OperationLogGroupKey」。
 *
 * @since 2026-04-28
 */

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
