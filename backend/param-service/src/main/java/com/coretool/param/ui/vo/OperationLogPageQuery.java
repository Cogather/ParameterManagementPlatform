package com.coretool.param.ui.vo;

import lombok.Data;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class OperationLogPageQuery {
    private String productId;
    private String bizTable;
    private String versionId;
    private boolean ignoreVersionFilter = false;
    private String resourceId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime operatedFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime operatedTo;

    /** operatedAt,asc 为升序，否则默认按时间倒序 */
    private String sort;

    private int page = 1;
    private int size = 20;
}

