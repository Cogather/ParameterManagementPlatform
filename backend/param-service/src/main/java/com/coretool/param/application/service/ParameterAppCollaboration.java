package com.coretool.param.application.service;

import com.coretool.param.domain.config.keyword.repository.ChangeSourceKeywordRepository;
import com.coretool.param.infrastructure.persistence.mapper.EntityCommandMappingMapper;

import org.springframework.stereotype.Component;

/**
 * 参数应用服务所需领域协作依赖（与 {@link ParameterAppPersistenceMappers} 共同构成完整构造入参）。
 */
@Component
public record ParameterAppCollaboration(
        ChangeSourceKeywordRepository changeSourceKeywordRepository,
        ConfigChangeTypeAppService configChangeTypeAppService,
        OperationLogAppService operationLogAppService,
        EntityCommandMappingMapper entityCommandMappingMapper) {}
