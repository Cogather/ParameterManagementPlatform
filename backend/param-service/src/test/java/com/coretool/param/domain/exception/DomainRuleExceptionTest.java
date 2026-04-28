package com.coretool.param.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
