package com.coretool.param.domain.parameter;

import com.coretool.param.domain.exception.DomainRuleException;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChangeDescriptionTypeRulesTest {

    private static final Set<String> ALLOWED = Set.of("新增参数", "修改参数含义");

    @Test
    void validateForSave_shouldThrow_whenListNullOrEmpty() {
        assertThatThrownBy(() -> ChangeDescriptionTypeRules.validateForSave(false, null, ALLOWED))
                .isInstanceOf(DomainRuleException.class)
                .hasMessageContaining("至少一条");
        assertThatThrownBy(() -> ChangeDescriptionTypeRules.validateForSave(false, List.of(), ALLOWED))
                .isInstanceOf(DomainRuleException.class);
    }

    @Test
    void validateForSave_shouldThrow_whenBlankEntry() {
        assertThatThrownBy(
                        () ->
                                ChangeDescriptionTypeRules.validateForSave(
                                        false, List.of("新增参数", " "), ALLOWED))
                .isInstanceOf(DomainRuleException.class)
                .hasMessageContaining("变更类型不能为空");
    }

    @Test
    void validateForSave_shouldThrow_whenNotInDictionary() {
        assertThatThrownBy(
                        () ->
                                ChangeDescriptionTypeRules.validateForSave(
                                        false, List.of("不存在"), ALLOWED))
                .isInstanceOf(DomainRuleException.class)
                .hasMessageContaining("字典");
    }

    @Test
    void validateForSave_shouldPass_whenCreateAndOnlyNewParameter() {
        assertThatCode(
                        () ->
                                ChangeDescriptionTypeRules.validateForSave(
                                        true,
                                        List.of(ChangeDescriptionTypeRules.TYPE_NEW_PARAMETER_CN),
                                        ALLOWED))
                .doesNotThrowAnyException();
    }

    @Test
    void validateForSave_shouldThrow_whenCreate_butNotOnlyNewParameter() {
        assertThatThrownBy(
                        () ->
                                ChangeDescriptionTypeRules.validateForSave(true, List.of("修改参数含义"), ALLOWED))
                .isInstanceOf(DomainRuleException.class)
                .hasMessageContaining("新增参数");
    }

    @Test
    void validateForSave_shouldPass_whenEditAndAllowedTypes() {
        assertThatCode(
                        () ->
                                ChangeDescriptionTypeRules.validateForSave(false, List.of("修改参数含义"), ALLOWED))
                .doesNotThrowAnyException();
    }
}
