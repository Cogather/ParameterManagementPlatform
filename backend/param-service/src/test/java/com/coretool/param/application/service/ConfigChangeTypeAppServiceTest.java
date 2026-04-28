package com.coretool.param.application.service;

import com.coretool.param.infrastructure.persistence.entity.ConfigChangeTypePo;
import com.coretool.param.infrastructure.persistence.mapper.ConfigChangeTypeMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.domain.parameter.ChangeDescriptionTypeRules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigChangeTypeAppServiceTest {

    @Mock
    private ConfigChangeTypeMapper mapper;

    @Test
    void listAllOrdered_shouldReturnDefaultRows_whenTableEmpty() {
        when(mapper.selectList(any())).thenReturn(List.of());

        var out = new ConfigChangeTypeAppService(mapper).listAllOrdered();

        assertThat(out).isNotEmpty();
    }

    @Test
    void allowedChangeTypeNameCn_shouldReturnDedupedOrderedSet() {
        ConfigChangeTypePo a = new ConfigChangeTypePo();
        a.setChangeTypeNameCn(" A ");
        ConfigChangeTypePo b = new ConfigChangeTypePo();
        b.setChangeTypeNameCn("A");
        when(mapper.selectList(any())).thenReturn(List.of(a, b));

        var out = new ConfigChangeTypeAppService(mapper).allowedChangeTypeNameCn();

        assertThat(out).containsExactly("A");
    }

    @Test
    void listAllOrdered_shouldReturnDbRows_whenMapperReturnsRows() {
        ConfigChangeTypePo row = new ConfigChangeTypePo();
        row.setChangeTypeNameCn("仅来自库");
        when(mapper.selectList(any())).thenReturn(List.of(row));

        var out = new ConfigChangeTypeAppService(mapper).listAllOrdered();

        assertThat(out).extracting(ConfigChangeTypePo::getChangeTypeNameCn).containsExactly("仅来自库");
    }

    @Test
    void validateChangeTypesForParameterSave_shouldPass_whenCreateAndOnlyNewParameter() {
        when(mapper.selectList(any())).thenReturn(List.of());

        new ConfigChangeTypeAppService(mapper)
                .validateChangeTypesForParameterSave(
                        true, List.of(ChangeDescriptionTypeRules.TYPE_NEW_PARAMETER_CN));
    }

    @Test
    void validateChangeTypesForParameterSave_shouldThrow_whenCreateAndNotOnlyNewParameter() {
        when(mapper.selectList(any())).thenReturn(List.of());

        assertThatThrownBy(
                        () ->
                                new ConfigChangeTypeAppService(mapper)
                                        .validateChangeTypesForParameterSave(
                                                true, List.of("修改参数含义")))
                .isInstanceOf(DomainRuleException.class)
                .hasMessageContaining("新增参数");
    }

    @Test
    void validateChangeTypesForParameterSave_shouldPass_whenUpdateAndAllowedType() {
        when(mapper.selectList(any())).thenReturn(List.of());

        new ConfigChangeTypeAppService(mapper)
                .validateChangeTypesForParameterSave(false, List.of("修改参数含义"));
    }

    @Test
    void validateChangeTypesForParameterSave_shouldDelegateToAllowedSet() {
        ConfigChangeTypePo row = new ConfigChangeTypePo();
        row.setChangeTypeNameCn("自定义");
        when(mapper.selectList(any())).thenReturn(List.of(row));

        assertThatThrownBy(
                        () ->
                                new ConfigChangeTypeAppService(mapper)
                                        .validateChangeTypesForParameterSave(
                                                false, List.of("修改参数含义")))
                .isInstanceOf(DomainRuleException.class)
                .hasMessageContaining("字典");
    }

    @Test
    void validateChangeTypesForParameterSave_shouldThrow_whenListEmpty() {
        when(mapper.selectList(any())).thenReturn(List.of());

        assertThatThrownBy(
                        () ->
                                new ConfigChangeTypeAppService(mapper)
                                        .validateChangeTypesForParameterSave(false, List.of()))
                .isInstanceOf(DomainRuleException.class);
    }
}

