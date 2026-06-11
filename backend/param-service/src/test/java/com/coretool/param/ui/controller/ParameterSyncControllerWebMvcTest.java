/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.ui.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coretool.param.application.service.ParameterVersionCopyAppService;
import com.coretool.param.ui.vo.ParameterSyncCommandRequest;
import com.coretool.param.ui.vo.ParameterSyncParameterOption;
import com.coretool.param.ui.vo.ParameterSyncResultPayload;
import com.coretool.param.ui.vo.ParameterSyncTypeOption;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

/**
 * 参数同步接口 MockMvc 冒烟测试。
 */
@ExtendWith(MockitoExtension.class)
class ParameterSyncControllerWebMvcTest {

    private static final String PRODUCT = "p1";
    private static final String SOURCE_VER = "v_src";
    private static final String TARGET_VER = "v_tgt";

    @Mock
    private ParameterVersionCopyAppService parameterVersionCopyAppService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ParameterSyncController(parameterVersionCopyAppService))
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void typeOptions_returns_ok() throws Exception {
        ParameterSyncTypeOption opt = new ParameterSyncTypeOption();
        opt.setCommandId("c1");
        opt.setCommandName("CMD");
        opt.setCommandTypeId("t1");
        opt.setCommandTypeName("DWORD");
        when(parameterVersionCopyAppService.listTypeOptions(PRODUCT, SOURCE_VER)).thenReturn(List.of(opt));

        mockMvc.perform(
                        get("/api/v1/products/{productId}/versions/{versionId}/parameter-sync/type-options", PRODUCT, SOURCE_VER)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].commandId").value("c1"));
    }

    @Test
    void parameters_returns_ok() throws Exception {
        ParameterSyncParameterOption p = new ParameterSyncParameterOption();
        p.setSourceParameterId(100);
        p.setParameterNameCn("测试参数");
        p.setDataStatus("已基线");
        when(parameterVersionCopyAppService.listParameterOptions(PRODUCT, SOURCE_VER, "c1", "t1"))
                .thenReturn(List.of(p));

        mockMvc.perform(
                        get("/api/v1/products/{productId}/versions/{versionId}/parameter-sync/parameters", PRODUCT, SOURCE_VER)
                                .param("commandId", "c1")
                                .param("commandTypeId", "t1")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].dataStatus").value("已基线"));
    }

    @Test
    void syncCommands_allSuccess_returns_ok() throws Exception {
        ParameterSyncResultPayload payload = new ParameterSyncResultPayload();
        payload.setSuccessCount(1);
        payload.setFailureCount(0);
        when(parameterVersionCopyAppService.syncMany(eq(PRODUCT), eq(TARGET_VER), any(), eq("system")))
                .thenReturn(payload);

        ParameterSyncCommandRequest req = new ParameterSyncCommandRequest();
        req.setSourceVersionId(SOURCE_VER);

        mockMvc.perform(
                        post("/api/v1/products/{productId}/versions/{versionId}/parameter-sync/commands", PRODUCT, TARGET_VER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.successCount").value(1));

        verify(parameterVersionCopyAppService).syncMany(eq(PRODUCT), eq(TARGET_VER), any(), eq("system"));
    }

    @Test
    void syncCommands_allFailed_returns_failureMessage() throws Exception {
        ParameterSyncResultPayload payload = new ParameterSyncResultPayload();
        payload.setSuccessCount(0);
        payload.setFailureCount(1);
        ParameterSyncResultPayload.ParameterSyncFailureItem fail =
                new ParameterSyncResultPayload.ParameterSyncFailureItem();
        fail.setReason("目标版本已存在相同 parameter_code");
        payload.setFailures(List.of(fail));
        when(parameterVersionCopyAppService.syncMany(eq(PRODUCT), eq(TARGET_VER), any(), eq("system")))
                .thenReturn(payload);

        ParameterSyncCommandRequest req = new ParameterSyncCommandRequest();
        req.setSourceVersionId(SOURCE_VER);

        mockMvc.perform(
                        post("/api/v1/products/{productId}/versions/{versionId}/parameter-sync/commands", PRODUCT, TARGET_VER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("目标版本已存在相同 parameter_code"));
    }
}
