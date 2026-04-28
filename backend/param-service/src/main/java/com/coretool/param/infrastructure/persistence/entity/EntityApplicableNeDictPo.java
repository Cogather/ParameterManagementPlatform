package com.coretool.param.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@TableName("entity_applicable_ne_dict")
@Getter
@Setter
public class EntityApplicableNeDictPo {
    private String ownedProductId;

    @TableId
    private String neTypeId;

    private String neTypeNameCn;
    private String neTypeDescription;
    private Integer neTypeStatus;
    private String productForm;
    private String creatorId;
    private LocalDateTime creationTimestamp;
    private String updaterId;
    private LocalDateTime updateTimestamp;
}
