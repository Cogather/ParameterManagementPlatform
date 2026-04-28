package com.coretool.param.domain.parameter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParameterBaselinePolicyTest {

    @Test
    void isBaselineLocked_shouldMatchCanonicalStatus() {
        assertThat(ParameterBaselinePolicy.isBaselineLocked(ParameterBaselinePolicy.STATUS_BASELINE_LOCKED))
                .isTrue();
        assertThat(ParameterBaselinePolicy.isBaselineLocked(null)).isFalse();
        assertThat(ParameterBaselinePolicy.isBaselineLocked("草稿")).isFalse();
    }
}
