/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.domain.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 领域分页切片单元测试。
 *
 * @since 2026-04-28
 */
class PageSliceTest {

    @Test
    void accessors_shouldExposeConstructorArgs() {
        PageSlice<String> s = new PageSlice<>(List.of("a"), 10, 2, 5);
        assertThat(s.getRecords()).containsExactly("a");
        assertThat(s.getTotal()).isEqualTo(10);
        assertThat(s.getPage()).isEqualTo(2);
        assertThat(s.getSize()).isEqualTo(5);
    }
}
