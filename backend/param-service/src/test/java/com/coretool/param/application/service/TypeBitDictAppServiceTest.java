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

