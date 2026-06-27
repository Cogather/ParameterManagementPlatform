/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.application.service;

import com.coretool.param.domain.config.keyword.repository.ChangeSourceKeywordRepository;
import com.coretool.param.infrastructure.persistence.mapper.CommandTypeDefinitionMapper;
import com.coretool.param.infrastructure.persistence.mapper.CommandTypeVersionRangeMapper;
import com.coretool.param.infrastructure.persistence.mapper.ConfigChangeDescriptionMapper;
import com.coretool.param.infrastructure.persistence.mapper.EntityBusinessCategoryMapper;
import com.coretool.param.infrastructure.persistence.mapper.EntityCommandMappingMapper;
import com.coretool.param.infrastructure.persistence.mapper.EntityVersionInfoMapper;
import com.coretool.param.infrastructure.persistence.mapper.SystemParameterMapper;
import com.coretool.param.infrastructure.persistence.mapper.VersionFeatureDictMapper;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 参数应用服务 Record 构造与字段访问单元测试。
 *
 * @since 2026-04-28
 */
class ParameterRecordsTest {

    @Test
    void persistenceMappers_recordShouldExposeComponents() {
        SystemParameterMapper systemParameterMapper = Mockito.mock(SystemParameterMapper.class);
        ConfigChangeDescriptionMapper configChangeDescriptionMapper = Mockito.mock(ConfigChangeDescriptionMapper.class);
        CommandTypeVersionRangeMapper commandTypeVersionRangeMapper = Mockito.mock(CommandTypeVersionRangeMapper.class);
        CommandTypeDefinitionMapper commandTypeDefinitionMapper = Mockito.mock(CommandTypeDefinitionMapper.class);
        EntityVersionInfoMapper entityVersionInfoMapper = Mockito.mock(EntityVersionInfoMapper.class);

        ParameterAppPersistenceMappers m =
                new ParameterAppPersistenceMappers(
                        systemParameterMapper,
                        configChangeDescriptionMapper,
                        commandTypeVersionRangeMapper,
                        commandTypeDefinitionMapper,
                        entityVersionInfoMapper);

        assertThat(m.systemParameterMapper()).isSameAs(systemParameterMapper);
        assertThat(m.commandTypeDefinitionMapper()).isSameAs(commandTypeDefinitionMapper);
    }

    @Test
    void collaboration_recordShouldExposeComponents() {
        ChangeSourceKeywordRepository changeSourceKeywordRepository = Mockito.mock(ChangeSourceKeywordRepository.class);
        ConfigChangeTypeAppService configChangeTypeAppService = Mockito.mock(ConfigChangeTypeAppService.class);
        OperationLogAppService operationLogAppService = Mockito.mock(OperationLogAppService.class);
        EntityCommandMappingMapper entityCommandMappingMapper = Mockito.mock(EntityCommandMappingMapper.class);
        EntityBusinessCategoryMapper entityBusinessCategoryMapper = Mockito.mock(EntityBusinessCategoryMapper.class);
        VersionFeatureDictMapper versionFeatureDictMapper = Mockito.mock(VersionFeatureDictMapper.class);

        ParameterAppCollaboration c =
                new ParameterAppCollaboration(
                        changeSourceKeywordRepository,
                        configChangeTypeAppService,
                        operationLogAppService,
                        entityCommandMappingMapper,
                        entityBusinessCategoryMapper,
                        versionFeatureDictMapper);

        assertThat(c.operationLogAppService()).isSameAs(operationLogAppService);
        assertThat(c.entityCommandMappingMapper()).isSameAs(entityCommandMappingMapper);
        assertThat(c.entityBusinessCategoryMapper()).isSameAs(entityBusinessCategoryMapper);
        assertThat(c.versionFeatureDictMapper()).isSameAs(versionFeatureDictMapper);
    }
}

