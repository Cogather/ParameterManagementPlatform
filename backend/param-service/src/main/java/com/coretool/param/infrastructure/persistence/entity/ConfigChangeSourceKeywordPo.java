package com.coretool.param.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.time.LocalDateTime;

/** config_change_source_keyword（与 docs/table字段简介.md §7 对齐）。 */
@Data
@TableName("config_change_source_keyword")
public class ConfigChangeSourceKeywordPo {

    private String ownedProductId;

    @TableId
    private String keywordId;

    private String keywordRegex;
    private String reason;
    private Integer keywordStatus;
    private String creatorId;
    private LocalDateTime creationTimestamp;
    private String updaterId;
    private LocalDateTime updateTimestamp;
}
