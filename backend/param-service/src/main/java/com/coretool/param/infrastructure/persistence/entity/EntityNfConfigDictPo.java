package com.coretool.param.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@TableName("entity_nf_config_dict")
@Getter
@Setter
public class EntityNfConfigDictPo {
    private String ownedProductId;

    @TableId
    private String nfConfigId;

    private String nfConfigNameCn;
    private String nfConfigDescription;
    private Integer nfConfigStatus;
    private String creatorId;
    private LocalDateTime creationTimestamp;
    private String updaterId;
    private LocalDateTime updateTimestamp;
}
