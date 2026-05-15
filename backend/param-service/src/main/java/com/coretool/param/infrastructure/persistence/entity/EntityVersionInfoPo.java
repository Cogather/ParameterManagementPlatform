/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * entity_version_info（与 docs/table字段简介.md §5 对齐）。
 *
 * @since 2026-04-28
 */
@Data
@TableName("entity_version_info")
public class EntityVersionInfoPo {

    private String ownedProductId;

    @TableId
    private String versionId;

    private String versionName;
    private String versionType;
    private String versionDescription;
    private String baselineVersionId;
    private String baselineVersionName;
    private String versionDesc;
    private String approver;
    private String isHidden;
    private String supportedVersion;
    private Integer versionStatus;
    private String introducedProductId;
    private String ownerList;
    private String creatorId;
    private LocalDateTime creationTimestamp;
    private String updaterId;
    private LocalDateTime updateTimestamp;
}
