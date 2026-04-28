package com.coretool.param.application.service;

import com.coretool.param.infrastructure.persistence.mapper.CommandTypeDefinitionMapper;
import com.coretool.param.infrastructure.persistence.mapper.CommandTypeVersionRangeMapper;
import com.coretool.param.infrastructure.persistence.mapper.ConfigChangeDescriptionMapper;
import com.coretool.param.infrastructure.persistence.mapper.EntityVersionInfoMapper;
import com.coretool.param.infrastructure.persistence.mapper.SystemParameterMapper;

import org.springframework.stereotype.Component;

/**
 * 参数应用服务所需持久化 Mapper 分组（单构造记录 ≤5 个形参，供 {@link ParameterAppService} 注入）。
 */
@Component
public record ParameterAppPersistenceMappers(
        SystemParameterMapper systemParameterMapper,
        ConfigChangeDescriptionMapper configChangeDescriptionMapper,
        CommandTypeVersionRangeMapper commandTypeVersionRangeMapper,
        CommandTypeDefinitionMapper commandTypeDefinitionMapper,
        EntityVersionInfoMapper entityVersionInfoMapper) {}
