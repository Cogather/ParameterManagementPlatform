package com.coretool.param.ui.vo;

import lombok.Data;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class OperationLogGroupPageQuery {
    private String productId;
    private String bizTable;
    private String versionId;
    private boolean ignoreVersionFilter = false;
    private String resourceId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime operatedFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime operatedTo;

    private int page = 1;
    private int size = 20;
}

