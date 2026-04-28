package com.coretool.param.ui.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 操作日志分组内的明细行（对应 operation_log 的一行）。 */
@Data
public class OperationLogGroupLine {

    private String logId;
    private String fieldLabelCn;
    private String oldValue;
    private String newValue;
    private LocalDateTime operatedAt;
}

