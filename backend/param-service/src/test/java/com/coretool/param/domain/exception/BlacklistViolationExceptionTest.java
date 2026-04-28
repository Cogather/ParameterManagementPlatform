package com.coretool.param.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BlacklistViolationExceptionTest {

    @Test
    void shouldExposeViolatedRegex() {
        var e = new BlacklistViolationException("bad", "(?i)secret");
        assertThat(e.getMessage()).isEqualTo("bad");
        assertThat(e.getViolatedKeywordRegex()).isEqualTo("(?i)secret");
    }
}
