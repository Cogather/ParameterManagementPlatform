package com.coretool.param.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.coretool.param.infrastructure.persistence.entity.TypeBitDictPo;
import com.coretool.param.infrastructure.persistence.mapper.TypeBitDictMapper;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TypeBitDictAppService {

    private final TypeBitDictMapper mapper;

    /**
     * 构造应用服务。
     *
     * @param mapper 类型 BIT 字典 Mapper
     */
    public TypeBitDictAppService(TypeBitDictMapper mapper) {
        this.mapper = mapper;
    }

    /** 只读：按 type_enum 排序返回 */
    /**
     * 查询全部类型 BIT 字典项（按 type_enum 排序）。
     *
     * @return 列表结果
     */
    public List<TypeBitDictPo> listAll() {
        return mapper.selectList(new QueryWrapper<TypeBitDictPo>().orderByAsc("type_enum"));
    }
}

