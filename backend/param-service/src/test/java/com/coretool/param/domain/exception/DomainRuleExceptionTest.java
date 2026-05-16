/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 领域规则异常单元测试。
 *
 * @since 2026-04-28
 */
class DomainRuleExceptionTest {

    @Test
    void shouldCarryMessage_andCause() {
        var cause = new IllegalStateException("x");
        var e = new DomainRuleException("m", cause);
        assertThat(e.getMessage()).isEqualTo("m");
        assertThat(e.getCause()).isSameAs(cause);
    }

    @Test
    void shouldSupportMessageOnly() {
        assertThat(new DomainRuleException("only").getMessage()).isEqualTo("only");
    }
}
