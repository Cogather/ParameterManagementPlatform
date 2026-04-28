package com.coretool.param.ui.controller;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coretool.param.application.service.OperationLogAppService;
import com.coretool.param.infrastructure.persistence.entity.OperationLogPo;
import com.coretool.param.ui.response.PageResponse;
import com.coretool.param.ui.vo.OperationLogPageQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志查询接口冒烟：MockMvc standalone，不加载 MyBatis 等基础设施。
 */
@ExtendWith(MockitoExtension.class)
class OperationLogControllerWebMvcTest {

    @Mock
    private OperationLogAppService operationLogAppService;

    @InjectMocks
    private OperationLogController operationLogController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(operationLogController).build();
    }

    @Test
    void page_returns_ok() throws Exception {
        PageResponse<OperationLogPo> page = new PageResponse<>();
        page.setRecords(List.of());
        page.setTotal(0L);
        page.setPage(1);
        page.setSize(20);
        when(operationLogAppService.page(
                        argThat(
                                q ->
                                        "p1".equals(q.getProductId())
                                                && "entity_command_mapping".equals(q.getBizTable())
                                                && q.getVersionId() == null
                                                && q.getResourceId() == null
                                                && q.getOperatedFrom() == null
                                                && q.getOperatedTo() == null
                                                && q.getSort() == null
                                                && q.getPage() == 1
                                                && q.getSize() == 20
                                                && !q.isIgnoreVersionFilter())))
                .thenReturn(page);

        mockMvc.perform(
                        get("/api/v1/operation-logs")
                                .param("productId", "p1")
                                .param("bizTable", "entity_command_mapping")
                                .param("page", "1")
                                .param("size", "20")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void page_passes_time_range() throws Exception {
        PageResponse<OperationLogPo> page = new PageResponse<>();
        page.setRecords(List.of());
        page.setTotal(0L);
        page.setPage(1);
        page.setSize(10);
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 2, 23, 59);
        when(operationLogAppService.page(
                        argThat(
                                q ->
                                        "p1".equals(q.getProductId())
                                                && "command_type_definition".equals(q.getBizTable())
                                                && q.getVersionId() == null
                                                && q.getResourceId() == null
                                                && from.equals(q.getOperatedFrom())
                                                && to.equals(q.getOperatedTo())
                                                && "operatedAt,asc".equals(q.getSort())
                                                && q.getPage() == 1
                                                && q.getSize() == 10
                                                && !q.isIgnoreVersionFilter())))
                .thenReturn(page);

        mockMvc.perform(
                        get("/api/v1/operation-logs")
                                .param("productId", "p1")
                                .param("bizTable", "command_type_definition")
                                .param("operatedFrom", "2026-01-01T00:00:00")
                                .param("operatedTo", "2026-01-02T23:59:00")
                                .param("sort", "operatedAt,asc")
                                .param("page", "1")
                                .param("size", "10")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(operationLogAppService)
                .page(
                        argThat(
                                q ->
                                        "p1".equals(q.getProductId())
                                                && "command_type_definition".equals(q.getBizTable())
                                                && q.getVersionId() == null
                                                && q.getResourceId() == null
                                                && from.equals(q.getOperatedFrom())
                                                && to.equals(q.getOperatedTo())
                                                && "operatedAt,asc".equals(q.getSort())
                                                && q.getPage() == 1
                                                && q.getSize() == 10
                                                && !q.isIgnoreVersionFilter()));
    }
}
