package com.coretool.param.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** type_bit_dict（类型枚举与 BIT 位数映射；仅数据库维护）。 */
@Data
@TableName("type_bit_dict")
public class TypeBitDictPo {

    @TableId
    private String typeBitId;

    private String typeEnum;

    private Integer bitCount;
}

