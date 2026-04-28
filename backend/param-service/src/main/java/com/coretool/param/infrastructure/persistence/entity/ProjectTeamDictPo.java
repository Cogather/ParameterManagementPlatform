package com.coretool.param.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@TableName("project_team_dict")
@Getter
@Setter
public class ProjectTeamDictPo {
    private String ownedProductId;

    @TableId
    private String teamId;

    private String teamName;
    private String teamDescription;
    private Integer teamStatus;
    private String ownerList;
    private String creatorId;
    private LocalDateTime creationTimestamp;
    private String updaterId;
    private LocalDateTime updateTimestamp;
}
