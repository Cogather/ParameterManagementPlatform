package com.coretool.param.ui.vo;

import lombok.Data;

import java.time.LocalDateTime;

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

