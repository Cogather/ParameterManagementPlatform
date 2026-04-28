package com.coretool.param.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/**
 * 操作审计（单表，详设 §1.7.3；表名与字段与 DDL 一致）。
 */
@Getter
@Setter
@TableName("operation_log")
public class OperationLogPo {

    @TableId(value = "log_id", type = IdType.INPUT)
    private String logId;

    @TableField("biz_table")
    private String bizTable;

    @TableField("owned_product_id")
    private String ownedProductId;

    @TableField("owned_version_id")
    private String ownedVersionId;

    @TableField("resource_id")
    private String resourceId;

    @TableField("resource_name")
    private String resourceName;

    @TableField("operation_type")
    private String operationType;

    @TableField("field_label_cn")
    private String fieldLabelCn;

    @TableField("old_value")
    private String oldValue;

    @TableField("new_value")
    private String newValue;

    @TableField("operator_id")
    private String operatorId;

    @TableField("operated_at")
    private LocalDateTime operatedAt;

    @TableField("log_batch_id")
    private String logBatchId;
}
