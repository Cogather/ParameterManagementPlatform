package com.coretool.param.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("config_change_description")
public class ConfigChangeDescriptionPo {

    @TableId
    private String changeDescriptionId;

    private Integer parameterId;
    private String changeType;
    private String changeReasonCn;
    private String changeImpactCn;
    private String changeReasonEn;
    private String changeImpactEn;
    private String exportDelta;
    private String noExportReason;
    private String updaterId;
    private LocalDateTime updateTimestamp;
}
