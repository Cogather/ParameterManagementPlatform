package com.coretool.param.application.service;

import com.coretool.param.domain.config.keyword.repository.ChangeSourceKeywordRepository;
import com.coretool.param.infrastructure.persistence.mapper.CommandTypeDefinitionMapper;
import com.coretool.param.infrastructure.persistence.mapper.CommandTypeVersionRangeMapper;
import com.coretool.param.infrastructure.persistence.mapper.ConfigChangeDescriptionMapper;
import com.coretool.param.infrastructure.persistence.mapper.EntityCommandMappingMapper;
import com.coretool.param.infrastructure.persistence.mapper.EntityVersionInfoMapper;
import com.coretool.param.infrastructure.persistence.mapper.SystemParameterMapper;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

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

        ParameterAppCollaboration c =
                new ParameterAppCollaboration(
                        changeSourceKeywordRepository,
                        configChangeTypeAppService,
                        operationLogAppService,
                        entityCommandMappingMapper);

        assertThat(c.operationLogAppService()).isSameAs(operationLogAppService);
        assertThat(c.entityCommandMappingMapper()).isSameAs(entityCommandMappingMapper);
    }
}

