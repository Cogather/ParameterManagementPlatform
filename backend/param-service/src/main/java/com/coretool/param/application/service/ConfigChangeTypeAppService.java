package com.coretool.param.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coretool.param.application.support.ChangeDescriptionTypeDictionary;
import com.coretool.param.domain.parameter.ChangeDescriptionTypeRules;
import com.coretool.param.infrastructure.persistence.entity.ConfigChangeTypePo;
import com.coretool.param.infrastructure.persistence.mapper.ConfigChangeTypeMapper;

import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ConfigChangeTypeAppService {

    private final ConfigChangeTypeMapper configChangeTypeMapper;

    /**
     * 构造应用服务。
     *
     * @param configChangeTypeMapper 变更类型 Mapper
     */
    public ConfigChangeTypeAppService(ConfigChangeTypeMapper configChangeTypeMapper) {
        this.configChangeTypeMapper = configChangeTypeMapper;
    }

    /**
     * 查询全部变更类型（按排序字段）；若表为空则返回内置默认字典。
     *
     * @return 变更类型列表
     */
    public List<ConfigChangeTypePo> listAllOrdered() {
        List<ConfigChangeTypePo> list =
                configChangeTypeMapper.selectList(
                        new LambdaQueryWrapper<ConfigChangeTypePo>()
                                .orderByAsc(ConfigChangeTypePo::getChangeSequence));
        if (list.isEmpty()) {
            return ChangeDescriptionTypeDictionary.defaultRows();
        }
        return list;
    }

    /**
     * 获取允许的变更类型中文名集合（按字典排序去重）。
     *
     * @return 允许的变更类型中文名集合
     */
    public Set<String> allowedChangeTypeNameCn() {
        return listAllOrdered().stream()
                .map(ConfigChangeTypePo::getChangeTypeNameCn)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 校验参数保存时的变更类型集合是否合法。
     *
     * @param isCreate          是否为新增
     * @param changeTypeNamesCn 变更类型中文名列表
     */
    public void validateChangeTypesForParameterSave(boolean isCreate, List<String> changeTypeNamesCn) {
        ChangeDescriptionTypeRules.validateForSave(isCreate, changeTypeNamesCn, allowedChangeTypeNameCn());
    }
}
