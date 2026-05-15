/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 持久化实体「EntityCommandMappingPo」，映射数据库表结构。
 *
 * @since 2026-04-28
 */

@TableName("entity_command_mapping")
@Getter
@Setter
public class EntityCommandMappingPo {

    private String ownedProductId;

    @TableId
    private String commandId;

    private String commandName;
    private String creatorId;
    private LocalDateTime creationTimestamp;
    private String updaterId;
    private LocalDateTime updateTimestamp;
    private String ownerList;
    private Integer commandStatus;
}
