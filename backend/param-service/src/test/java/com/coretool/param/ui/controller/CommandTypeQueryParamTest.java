package com.coretool.param.ui.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommandTypeQueryParamTest {

    @Test
    void optionalTypeKey_shouldPreferId_thenCode() {
        assertThat(CommandTypeQueryParam.optionalTypeKey(" id ", "code")).isEqualTo("id");
        assertThat(CommandTypeQueryParam.optionalTypeKey("  ", " code ")).isEqualTo("code");
        assertThat(CommandTypeQueryParam.optionalTypeKey(null, null)).isNull();
    }

    @Test
    void requireTypeKey_shouldThrow_whenBothBlank() {
        assertThatThrownBy(() -> CommandTypeQueryParam.requireTypeKey(" ", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("至少传其一");
    }
}
