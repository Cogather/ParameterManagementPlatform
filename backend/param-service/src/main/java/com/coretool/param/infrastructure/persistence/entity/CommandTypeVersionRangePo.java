package com.coretool.param.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.time.LocalDateTime;

/** command_type_version_range（与 docs/table字段简介.md §4 对齐）。 */
@Data
@TableName("command_type_version_range")
public class CommandTypeVersionRangePo {

    private String ownedProductId;
    private String ownedCommandId;
    /** 关联 command_type_definition.command_type_id */
    private String ownedTypeId;

    @TableId
    private String versionRangeId;

    private Integer startIndex;
    private Integer endIndex;
    private String rangeDescription;
    private String rangeType;
    /** 版本场景下存 version_id */
    private String ownedVersionOrBusinessId;
    private Integer rangeStatus;
    private String creatorId;
    private LocalDateTime creationTimestamp;
    private String updaterId;
    private LocalDateTime updateTimestamp;
}
