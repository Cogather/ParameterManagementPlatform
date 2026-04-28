package com.coretool.param.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.time.LocalDateTime;

/** entity_business_category（与 docs/table字段简介.md §6 对齐）。 */
@Data
@TableName("entity_business_category")
public class EntityBusinessCategoryPo {

    private String ownedProductId;

    @TableId
    private String categoryId;

    private String categoryNameCn;
    private String categoryNameEn;
    private String featureRange;
    private String categoryType;
    private Integer categoryStatus;
    private String creatorId;
    private LocalDateTime creationTimestamp;
    private String updaterId;
    private LocalDateTime updateTimestamp;
}
