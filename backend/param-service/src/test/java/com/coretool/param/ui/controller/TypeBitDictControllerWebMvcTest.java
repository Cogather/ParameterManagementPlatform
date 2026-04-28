package com.coretool.param.ui.controller;

import com.coretool.param.application.service.TypeBitDictAppService;
import com.coretool.param.infrastructure.persistence.entity.TypeBitDictPo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class TypeBitDictControllerWebMvcTest {

    @Mock
    private TypeBitDictAppService appService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TypeBitDictController(appService)).build();
    }

    @Test
    void listAll_returnsSuccessEnvelope() throws Exception {
        TypeBitDictPo po = new TypeBitDictPo();
        po.setTypeEnum("ENUM_A");
        when(appService.listAll()).thenReturn(List.of(po));

        mockMvc.perform(get("/api/v1/type-bits").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].typeEnum").value("ENUM_A"));
    }
}
