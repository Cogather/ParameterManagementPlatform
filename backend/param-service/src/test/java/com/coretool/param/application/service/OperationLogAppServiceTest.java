package com.coretool.param.application.service;

import com.coretool.param.infrastructure.persistence.entity.EntityCommandMappingPo;
import com.coretool.param.infrastructure.persistence.entity.OperationLogPo;
import com.coretool.param.infrastructure.persistence.mapper.OperationLogMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class OperationLogAppServiceTest {

    @Mock
    private OperationLogMapper operationLogMapper;

    @Test
    void insertAll_shouldNoop_whenNullOrEmpty() {
        OperationLogAppService svc = new OperationLogAppService(operationLogMapper);

        svc.insertAll(null);
        svc.insertAll(List.of());

        verify(operationLogMapper, never()).insert(org.mockito.Mockito.<OperationLogPo>any());
    }

    @Test
    void insertAll_shouldAssignIds_andInsertEachRow() {
        OperationLogAppService svc = new OperationLogAppService(operationLogMapper);

        OperationLogPo a = new OperationLogPo();
        OperationLogPo b = new OperationLogPo();
        svc.insertAll(List.of(a, b));

        verify(operationLogMapper, times(2)).insert(org.mockito.Mockito.<OperationLogPo>any());
    }

    @Test
    void logCommandCreate_shouldInsertTwoLines_whenNameAndOwnerPresent() {
        OperationLogAppService svc = new OperationLogAppService(operationLogMapper);

        EntityCommandMappingPo cmd = new EntityCommandMappingPo();
        cmd.setCommandId("c1");
        cmd.setCommandName("CMD");
        cmd.setOwnerList("u1,u2");

        svc.logCommandCreate("p1", cmd, "op1", null);

        verify(operationLogMapper, times(2)).insert(org.mockito.Mockito.<OperationLogPo>any());
    }
}

