package com.coretool.param.application.service;

import com.coretool.param.infrastructure.persistence.entity.CommandTypeDefinitionPo;
import com.coretool.param.infrastructure.persistence.entity.EntityCommandMappingPo;
import com.coretool.param.infrastructure.persistence.mapper.CommandTypeDefinitionMapper;
import com.coretool.param.infrastructure.persistence.mapper.EntityCommandMappingMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParameterCommandTreeAppServiceTest {

    @Mock
    private EntityCommandMappingMapper commandMapper;

    @Mock
    private CommandTypeDefinitionMapper typeMapper;

    @Test
    void treeForProduct_shouldReturnDemo_whenProductIdBlank() {
        var out = new ParameterCommandTreeAppService(commandMapper, typeMapper).treeForProduct(" ");
        assertThat(out).hasSize(1);
        assertThat(out.getFirst().getCommandId()).isEqualTo("command_demo");
    }

    @Test
    void treeForProduct_shouldReturnDemo_whenNoCommands() {
        when(commandMapper.selectList(any())).thenReturn(List.of());
        var out = new ParameterCommandTreeAppService(commandMapper, typeMapper).treeForProduct("p1");
        assertThat(out).hasSize(1);
        assertThat(out.getFirst().getCommandId()).isEqualTo("command_demo");
    }

    @Test
    void treeForProduct_shouldBuildCommandAndTypes_whenDataPresent() {
        EntityCommandMappingPo c = new EntityCommandMappingPo();
        c.setOwnedProductId("p1");
        c.setCommandId("c1");
        c.setCommandName("CMD");
        c.setCommandStatus(1);
        when(commandMapper.selectList(any())).thenReturn(List.of(c));

        CommandTypeDefinitionPo t1 = new CommandTypeDefinitionPo();
        t1.setOwnedProductId("p1");
        t1.setOwnedCommandId("c1");
        t1.setCommandType("byte");
        t1.setCommandTypeName("BYTE");
        CommandTypeDefinitionPo t2 = new CommandTypeDefinitionPo();
        t2.setOwnedProductId("p1");
        t2.setOwnedCommandId("c1");
        t2.setCommandType("bit");
        t2.setCommandTypeName("BIT");
        when(typeMapper.selectList(any())).thenReturn(List.of(t1, t2));

        var out = new ParameterCommandTreeAppService(commandMapper, typeMapper).treeForProduct("p1");

        assertThat(out).hasSize(1);
        assertThat(out.getFirst().getCommandId()).isEqualTo("c1");
        assertThat(out.getFirst().getTypes()).hasSize(2);
        assertThat(out.getFirst().getTypes().getFirst().getCode()).isEqualTo("BIT");
        assertThat(out.getFirst().getTypes().get(1).getCode()).isEqualTo("BYTE");
    }
}

