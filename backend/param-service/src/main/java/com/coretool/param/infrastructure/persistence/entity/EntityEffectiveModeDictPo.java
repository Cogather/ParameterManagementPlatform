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
 * 持久化实体「EntityEffectiveModeDictPo」，映射数据库表结构。
 *
 * @since 2026-04-28
 */

@TableName("entity_effective_mode_dict")
@Getter
@Setter
public class EntityEffectiveModeDictPo {
    private String ownedProductId;

    @TableId
    private String effectiveModeId;

    private String effectiveModeNameCn;
    private String effectiveModeNameEn;
    private String effectiveModeDescription;
    private Integer effectiveModeStatus;
    private String creatorId;
    private LocalDateTime creationTimestamp;
    private String updaterId;
    private LocalDateTime updateTimestamp;
}
