/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 黑名单违规异常单元测试。
 *
 * @since 2026-04-28
 */
class BlacklistViolationExceptionTest {

    @Test
    void shouldExposeViolatedRegex() {
        var e = new BlacklistViolationException("bad", "(?i)secret");
        assertThat(e.getMessage()).isEqualTo("bad");
        assertThat(e.getViolatedKeywordRegex()).isEqualTo("(?i)secret");
    }
}
