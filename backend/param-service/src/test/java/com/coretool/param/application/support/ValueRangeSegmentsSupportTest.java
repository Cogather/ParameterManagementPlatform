/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.application.support;

import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.infrastructure.persistence.entity.SystemParameterPo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ValueRangeSegmentsSupport} 单元测试。
 */
class ValueRangeSegmentsSupportTest {

    @Test
    void formatValueRange_shouldJoinSegments() {
        String out =
                ValueRangeSegmentsSupport.formatValueRange(
                        List.of(new ValueRangeSegmentsSupport.Segment(1, 10), new ValueRangeSegmentsSupport.Segment(20, 30)));
        assertThat(out).isEqualTo("1-10,20-30");
    }

    @Test
    void parseSegments_shouldRejectOverlap() {
        String json = "[{\"min\":1,\"max\":10},{\"min\":5,\"max\":15}]";
        assertThatThrownBy(() -> ValueRangeSegmentsSupport.parseSegments(json))
                .isInstanceOf(DomainRuleException.class)
                .hasMessageContaining("重叠");
    }

    @Test
    void applyToParameter_shouldSetSegmentsAndValueRange() {
        SystemParameterPo po = new SystemParameterPo();
        ValueRangeSegmentsSupport.applyToParameter("[{\"min\":1,\"max\":3}]", po);
        assertThat(po.getValueRange()).isEqualTo("1-3");
        assertThat(po.getValueRangeSegments()).contains("\"min\":1");
    }

    @Test
    void applyFromJoinedText_shouldSetSegmentsAndValueRange() {
        SystemParameterPo po = new SystemParameterPo();
        ValueRangeSegmentsSupport.applyFromJoinedText("1-10,20-30", po);
        assertThat(po.getValueRange()).isEqualTo("1-10,20-30");
        assertThat(po.getValueRangeSegments()).contains("\"min\":1");
    }

    @Test
    void parseFromJoinedText_shouldRejectInvalidToken() {
        assertThatThrownBy(() -> ValueRangeSegmentsSupport.parseFromJoinedText("abc"))
                .isInstanceOf(DomainRuleException.class);
    }
}
