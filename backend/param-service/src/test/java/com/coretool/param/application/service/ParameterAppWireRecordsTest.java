package com.coretool.param.application.service;

import com.coretool.param.domain.config.keyword.repository.ChangeSourceKeywordRepository;
import com.coretool.param.infrastructure.persistence.mapper.CommandTypeDefinitionMapper;
import com.coretool.param.infrastructure.persistence.mapper.CommandTypeVersionRangeMapper;
import com.coretool.param.infrastructure.persistence.mapper.ConfigChangeDescriptionMapper;
import com.coretool.param.infrastructure.persistence.mapper.EntityCommandMappingMapper;
import com.coretool.param.infrastructure.persistence.mapper.EntityVersionInfoMapper;
import com.coretool.param.infrastructure.persistence.mapper.SystemParameterMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/** 构造注入用 Record 分型测试（与 {@link ParameterAppService} 装配一致）。 */
@ExtendWith(MockitoExtension.class)
class ParameterAppWireRecordsTest {

    @Mock
    private ChangeSourceKeywordRepository changeSourceKeywordRepository;

    @Mock
    private ConfigChangeTypeAppService configChangeTypeAppService;

    @Mock
    private OperationLogAppService operationLogAppService;

    @Mock
    private EntityCommandMappingMapper entityCommandMappingMapper;

    @Mock
    private SystemParameterMapper systemParameterMapper;

    @Mock
    private ConfigChangeDescriptionMapper configChangeDescriptionMapper;

    @Mock
    private CommandTypeVersionRangeMapper commandTypeVersionRangeMapper;

    @Mock
    private CommandTypeDefinitionMapper commandTypeDefinitionMapper;

    @Mock
    private EntityVersionInfoMapper entityVersionInfoMapper;

    @Test
    void parameterAppCollaboration_shouldExposeComponents() {
        var r =
                new ParameterAppCollaboration(
                        changeSourceKeywordRepository,
                        configChangeTypeAppService,
                        operationLogAppService,
                        entityCommandMappingMapper);

        assertThat(r.changeSourceKeywordRepository()).isSameAs(changeSourceKeywordRepository);
        assertThat(r.configChangeTypeAppService()).isSameAs(configChangeTypeAppService);
        assertThat(r.operationLogAppService()).isSameAs(operationLogAppService);
        assertThat(r.entityCommandMappingMapper()).isSameAs(entityCommandMappingMapper);
    }

    @Test
    void parameterAppPersistenceMappers_shouldExposeComponents() {
        var r =
                new ParameterAppPersistenceMappers(
                        systemParameterMapper,
                        configChangeDescriptionMapper,
                        commandTypeVersionRangeMapper,
                        commandTypeDefinitionMapper,
                        entityVersionInfoMapper);

        assertThat(r.systemParameterMapper()).isSameAs(systemParameterMapper);
        assertThat(r.configChangeDescriptionMapper()).isSameAs(configChangeDescriptionMapper);
        assertThat(r.commandTypeVersionRangeMapper()).isSameAs(commandTypeVersionRangeMapper);
        assertThat(r.commandTypeDefinitionMapper()).isSameAs(commandTypeDefinitionMapper);
        assertThat(r.entityVersionInfoMapper()).isSameAs(entityVersionInfoMapper);
    }
}
