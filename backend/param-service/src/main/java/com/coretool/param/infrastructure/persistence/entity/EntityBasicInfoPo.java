package com.coretool.param.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 产品/产品形态主数据（与 docs/table字段简介.md §1 entity_basic_info 对齐）。
 */
@Data
@TableName("entity_basic_info")
public class EntityBasicInfoPo {

    private String entityName;

    @TableId
    private String productFormId;

    private String productSoftParamType;
    private String productForm;
    private String productId;
    private String creatorId;
    private LocalDateTime creationTimestamp;
    private String updaterId;
    private LocalDateTime updateTimestamp;
    private String ownerList;
    private Integer entityStatus;
}
