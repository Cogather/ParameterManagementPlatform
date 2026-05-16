/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.application.service;

import com.coretool.param.infrastructure.persistence.entity.TypeBitDictPo;
import com.coretool.param.infrastructure.persistence.mapper.TypeBitDictMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 类型 BIT 字典应用服务单元测试。
 *
 * @since 2026-04-28
 */
@ExtendWith(MockitoExtension.class)
class TypeBitDictAppServiceTest {

    @Mock
    private TypeBitDictMapper mapper;

    @Test
    void listAll_shouldDelegateToMapper() {
        when(mapper.selectList(any())).thenReturn(List.of(new TypeBitDictPo()));

        var out = new TypeBitDictAppService(mapper).listAll();

        assertThat(out).hasSize(1);
    }
}

