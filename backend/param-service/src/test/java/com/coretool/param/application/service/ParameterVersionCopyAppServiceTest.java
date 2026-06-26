/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.application.service;

import com.coretool.param.domain.config.keyword.repository.ChangeSourceKeywordRepository;
import com.coretool.param.domain.parameter.ParameterBaselinePolicy;
import com.coretool.param.infrastructure.persistence.entity.SystemParameterPo;
import com.coretool.param.infrastructure.persistence.mapper.ConfigChangeDescriptionMapper;
import com.coretool.param.infrastructure.persistence.mapper.EntityCommandMappingMapper;
import com.coretool.param.infrastructure.persistence.mapper.SystemParameterMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 参数版本复制服务单元测试。
 *
 * @since 2026-05-21
 */
@ExtendWith(MockitoExtension.class)
class ParameterVersionCopyAppServiceTest {

    @Mock
    private SystemParameterMapper systemParameterMapper;

    @Mock
    private ConfigChangeDescriptionMapper configChangeDescriptionMapper;

    @Mock
    private EntityCommandMappingMapper entityCommandMappingMapper;

    @Mock
    private ChangeSourceKeywordRepository changeSourceKeywordRepository;

    @Mock
    private OperationLogAppService operationLogAppService;

    private ParameterVersionCopyAppService svc() {
        return new ParameterVersionCopyAppService(
                systemParameterMapper,
                configChangeDescriptionMapper,
                entityCommandMappingMapper,
                changeSourceKeywordRepository,
                operationLogAppService);
    }

    @Test
    void copyAll_emptySource_returnsZero() {
        when(systemParameterMapper.selectList(any())).thenReturn(Collections.emptyList());
        int n = svc().copyAll("p1", "v_src", "v_tgt", "op1");
        assertThat(n).isZero();
    }

    @Test
    void copyAll_copiesBaselineStatus() {
        SystemParameterPo src = sample("DWORD_1", "已基线");
        when(systemParameterMapper.selectList(any()))
                .thenReturn(List.of(src))
                .thenReturn(Collections.emptyList());
        lenient().when(entityCommandMappingMapper.selectList(any())).thenReturn(Collections.emptyList());
        lenient().when(changeSourceKeywordRepository.listEnabledRegexesByProduct(any()))
                .thenReturn(Collections.emptyList());
        lenient().when(configChangeDescriptionMapper.selectList(any())).thenReturn(Collections.emptyList());

        int n = svc().copyAll("p1", "v_src", "v_tgt", "op1");
        assertThat(n).isEqualTo(1);

        ArgumentCaptor<SystemParameterPo> cap = ArgumentCaptor.forClass(SystemParameterPo.class);
        verify(systemParameterMapper, atLeastOnce()).insert(cap.capture());
        SystemParameterPo inserted = cap.getValue();
        assertThat(inserted.getDataStatus()).isEqualTo(ParameterBaselinePolicy.STATUS_BASELINE_LOCKED);
        assertThat(inserted.getOwnedVersionId()).isEqualTo("v_tgt");
        assertThat(inserted.getParameterCode()).isEqualTo("DWORD_1");
    }

    @Test
    void copyAll_allowsDuplicateParameterCodeOnSource() {
        SystemParameterPo a = sample("BYTE_1", "Draft");
        a.setBitUsage("1");
        SystemParameterPo b = sample("BYTE_1", "Draft");
        b.setParameterId(101);
        b.setBitUsage("2");
        when(systemParameterMapper.selectList(any()))
                .thenReturn(List.of(a, b))
                .thenReturn(Collections.emptyList());
        lenient().when(entityCommandMappingMapper.selectList(any())).thenReturn(Collections.emptyList());
        lenient().when(changeSourceKeywordRepository.listEnabledRegexesByProduct(any()))
                .thenReturn(Collections.emptyList());
        lenient().when(configChangeDescriptionMapper.selectList(any())).thenReturn(Collections.emptyList());

        int n = svc().copyAll("p1", "v_src", "v_tgt", "op1");
        assertThat(n).isEqualTo(2);
    }

    private static SystemParameterPo sample(String code, String status) {
        SystemParameterPo p = new SystemParameterPo();
        p.setParameterId(100);
        p.setParameterCode(code);
        p.setParameterSequence(1);
        p.setOwnedProductId("p1");
        p.setOwnedVersionId("v_src");
        p.setOwnedCommandId("command_abc");
        p.setDataStatus(status);
        p.setChangeSource("");
        p.setValueRange("1");
        p.setBitUsage("1");
        return p;
    }
}
