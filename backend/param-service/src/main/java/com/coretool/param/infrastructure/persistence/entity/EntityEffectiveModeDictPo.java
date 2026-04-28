package com.coretool.param.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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
