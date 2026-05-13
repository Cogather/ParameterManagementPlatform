package com.coretool.param.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 持久化实体「ConfigChangeTypePo」，映射数据库表结构。
 *
 * @since 2026-04-28
 */

@Data
@TableName("config_change_type")
public class ConfigChangeTypePo {

    @TableId
    private Integer changeTypeId;

    private String changeTypeName;
    private String changeTypeNameCn;
    private String changeTypeNameEn;
    private Integer changeSequence;
}
