package com.coretool.param.ui.controller;

import com.coretool.param.application.service.OperationLogAppService;
import com.coretool.param.infrastructure.persistence.entity.OperationLogPo;
import com.coretool.param.ui.response.PageResponse;
import com.coretool.param.ui.response.ResponseObject;
import com.coretool.param.ui.vo.OperationLogGroupItem;
import com.coretool.param.ui.vo.OperationLogGroupPageQuery;
import com.coretool.param.ui.vo.OperationLogPageQuery;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作审计查询（详设 §1.7.6；写入由各应用服务在成功路径落库）。
 */
@RestController
@RequestMapping("/api/v1/operation-logs")
@Tag(name = "OperationLogController", description = "操作日志")
public class OperationLogController {

    private final OperationLogAppService operationLogAppService;

    /**
     * 构造控制器。
     *
     * @param operationLogAppService 操作日志应用服务
     */
    public OperationLogController(OperationLogAppService operationLogAppService) {
        this.operationLogAppService = operationLogAppService;
    }

    /**
     * 分页查询操作日志（不分组）。
     *
     * @param query 分页与业务过滤，字段见 OperationLogPageQuery
     * @return 分页结果
     */
    @Operation(summary = "分页查询操作日志")
    @GetMapping(produces = "application/json; charset=utf-8")
    public ResponseObject<PageResponse<OperationLogPo>> page(
            @ModelAttribute OperationLogPageQuery query) {
        return new ResponseObject<PageResponse<OperationLogPo>>()
                .success(operationLogAppService.page(query));
    }

    /**
     * 分页查询操作日志（按一次操作分组折叠）。
     *
     * @param query 分组分页与业务过滤，字段见 OperationLogGroupPageQuery
     * @return 分页结果（按组统计）
     */
    @Operation(summary = "分页查询操作日志（按一次操作分组折叠）")
    @GetMapping(value = "/groups", produces = "application/json; charset=utf-8")
    public ResponseObject<PageResponse<OperationLogGroupItem>> pageGroups(
            @ModelAttribute OperationLogGroupPageQuery query) {
        return new ResponseObject<PageResponse<OperationLogGroupItem>>()
                .success(operationLogAppService.pageGroups(query));
    }
}
