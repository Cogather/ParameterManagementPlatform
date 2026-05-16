/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.domain.parameter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 参数基线锁定策略单元测试。
 *
 * @since 2026-04-28
 */
class ParameterBaselinePolicyTest {

    @Test
    void isBaselineLocked_shouldMatchCanonicalStatus() {
        assertThat(ParameterBaselinePolicy.isBaselineLocked(ParameterBaselinePolicy.STATUS_BASELINE_LOCKED))
                .isTrue();
        assertThat(ParameterBaselinePolicy.isBaselineLocked(null)).isFalse();
        assertThat(ParameterBaselinePolicy.isBaselineLocked("草稿")).isFalse();
    }
}
