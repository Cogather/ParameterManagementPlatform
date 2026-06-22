/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.application.support;

import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.infrastructure.persistence.entity.SystemParameterPo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ParameterSaveValidation} 单元测试。
 */
class ParameterSaveValidationTest {

    @Test
    void assertCreate_shouldAllowNullDetailFields() {
        SystemParameterPo main = minimalCreateMain();
        assertThatCode(() -> ParameterSaveValidation.assertCreate(main, false)).doesNotThrowAnyException();
    }

    @Test
    void assertUpdate_shouldRequireDetailFields() {
        SystemParameterPo main = minimalCreateMain();
        assertThatThrownBy(() -> ParameterSaveValidation.assertUpdate(main, false))
                .isInstanceOf(DomainRuleException.class)
                .hasMessageContaining("取值说明");
    }

    private static SystemParameterPo minimalCreateMain() {
        SystemParameterPo main = new SystemParameterPo();
        main.setParameterNameCn("名称");
        main.setOwnedCommandId("c1");
        main.setParameterCode("BIT_1");
        main.setParameterSequence(1);
        main.setParameterDefaultValue("0");
        main.setParameterRecommendedValue("1");
        main.setIntroducedVersion("v1");
        main.setParameterUnitCn("个");
        main.setValueRangeSegments("[{\"min\":0,\"max\":255}]");
        return main;
    }
}
