package com.coretool.param.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.time.LocalDateTime;

/** command_type_definition（与 docs/table字段简介.md §3 对齐）。 */
@Data
@TableName("command_type_definition")
public class CommandTypeDefinitionPo {

    private String ownedProductId;
    private String ownedCommandId;

    @TableId("command_type_id")
    private String commandTypeId;

    private String commandTypeName;
    /** 类型枚举：BIT / BYTE / DWORD / STRING 等 */
    private String commandType;
    private Integer minValue;
    private Integer maxValue;
    private String occupiedSerialNumber;
    private Integer commandTypeStatus;
    private String creatorId;
    private LocalDateTime creationTimestamp;
    private String updaterId;
    private LocalDateTime updateTimestamp;
}
