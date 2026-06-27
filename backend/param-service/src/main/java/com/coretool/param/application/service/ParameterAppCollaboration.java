/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.application.service;

import com.coretool.param.domain.config.keyword.repository.ChangeSourceKeywordRepository;
import com.coretool.param.infrastructure.persistence.mapper.EntityBusinessCategoryMapper;
import com.coretool.param.infrastructure.persistence.mapper.EntityCommandMappingMapper;
import com.coretool.param.infrastructure.persistence.mapper.VersionFeatureDictMapper;

import org.springframework.stereotype.Component;

/**
 * 参数应用服务所需领域协作依赖（与 ParameterAppPersistenceMappers共同构成完整构造入参）。
 *
 * @since 2026-04-28
 */

@Component
public record ParameterAppCollaboration(
        ChangeSourceKeywordRepository changeSourceKeywordRepository,
        ConfigChangeTypeAppService configChangeTypeAppService,
        OperationLogAppService operationLogAppService,
        EntityCommandMappingMapper entityCommandMappingMapper,
        EntityBusinessCategoryMapper entityBusinessCategoryMapper,
        VersionFeatureDictMapper versionFeatureDictMapper) {}
