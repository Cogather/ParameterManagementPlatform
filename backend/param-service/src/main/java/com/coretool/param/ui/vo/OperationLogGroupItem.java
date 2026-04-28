package com.coretool.param.ui.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志分组条目：一组代表一次操作（同资源、同操作人、同时间、同类型、同 batch）。
 *
 * <p>用于前端折叠展示：分页与 total 均按“组”统计，而非按字段行统计。
 */
@Data
public class OperationLogGroupItem {

    private String groupKey;
    private String bizTable;
    private String ownedProductId;
    private String ownedVersionId;
    private String resourceId;
    private String resourceName;
    private String operationType;
    private String operatorId;
    private LocalDateTime operatedAt;
    private String logBatchId;

    private int itemCount;
    private List<OperationLogGroupLine> items;
}

