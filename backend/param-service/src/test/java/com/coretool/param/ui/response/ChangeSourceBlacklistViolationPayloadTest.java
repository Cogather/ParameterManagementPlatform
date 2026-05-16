/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.ui.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 变更来源黑名单违规响应载荷单元测试。
 *
 * @since 2026-04-28
 */
class ChangeSourceBlacklistViolationPayloadTest {

    @Test
    void constructor_shouldSetRegexField() {
        ChangeSourceBlacklistViolationPayload p =
                new ChangeSourceBlacklistViolationPayload("(?i)secret");
        assertThat(p.getViolatedKeywordRegex()).isEqualTo("(?i)secret");
    }

    @Test
    void noArgConstructor_shouldAllowSetter() {
        ChangeSourceBlacklistViolationPayload p = new ChangeSourceBlacklistViolationPayload();
        p.setViolatedKeywordRegex("a+");
        assertThat(p.getViolatedKeywordRegex()).isEqualTo("a+");
    }
}
