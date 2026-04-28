package com.coretool.param.ui.controller;

import com.coretool.param.application.service.ConfigChangeTypeAppService;
import com.coretool.param.infrastructure.persistence.entity.ConfigChangeTypePo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ConfigChangeTypeControllerWebMvcTest {

    @Mock
    private ConfigChangeTypeAppService configChangeTypeAppService;

    @InjectMocks
    private ConfigChangeTypeController configChangeTypeController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(configChangeTypeController).build();
    }

    @Test
    void listAll_returnsSuccessEnvelope() throws Exception {
        ConfigChangeTypePo row = new ConfigChangeTypePo();
        row.setChangeTypeNameCn("新增参数");
        when(configChangeTypeAppService.listAllOrdered()).thenReturn(List.of(row));

        mockMvc.perform(get("/api/v1/config-change-types").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].changeTypeNameCn").value("新增参数"));
    }
}
