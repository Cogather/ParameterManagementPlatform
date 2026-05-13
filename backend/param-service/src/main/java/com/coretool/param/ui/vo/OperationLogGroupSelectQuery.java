package com.coretool.param.ui.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 请求/查询视图对象「OperationLogGroupSelectQuery」。
 *
 * @since 2026-04-28
 */

@Data
public class OperationLogGroupSelectQuery {
    private String productId;
    private String bizTable;
    private String versionId;
    private boolean ignoreVersionFilter;
    private String resourceId;
    private LocalDateTime operatedFrom;
    private LocalDateTime operatedTo;
    private long offset;
    private long size;
}
