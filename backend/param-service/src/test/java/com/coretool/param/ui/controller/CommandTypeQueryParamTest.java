/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.ui.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 命令类型查询参数解析单元测试。
 *
 * @since 2026-04-28
 */
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
