package com.coretool.param.domain.parameter;

import com.coretool.param.domain.exception.DomainRuleException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParameterTest {

    @Test
    void assertWritable_shouldPass_whenNotBaselineLocked() {
        assertThatCode(() -> new Parameter("草稿").assertWritable()).doesNotThrowAnyException();
        assertThatCode(() -> new Parameter(null).assertWritable()).doesNotThrowAnyException();
    }

    @Test
    void assertWritable_shouldThrow_whenBaselineLocked() {
        assertThatThrownBy(() -> new Parameter(ParameterBaselinePolicy.STATUS_BASELINE_LOCKED).assertWritable())
                .isInstanceOf(DomainRuleException.class)
                .hasMessageContaining("基线");
    }
}
