package com.coretool.param.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@TableName("version_feature_dict")
@Getter
@Setter
public class VersionFeatureDictPo {
    private String ownedProductPbiId;
    private String ownedVersionId;

    @TableId
    private String featureId;

    private String featureCode;
    private String featureNameCn;
    private String featureNameEn;
    private String introduceType;
    private String inheritReferenceVersionId;
    private Integer featureStatus;
    private String creatorId;
    private LocalDateTime creationTimestamp;
    private String updaterId;
    private LocalDateTime updateTimestamp;
}
