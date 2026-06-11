/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.ui.controller;

import com.coretool.param.application.service.ParameterVersionCopyAppService;
import org.apache.commons.lang3.StringUtils;
import com.coretool.param.constants.CommonConst;
import com.coretool.param.ui.response.ResponseObject;
import com.coretool.param.ui.vo.ParameterSyncCommandRequest;
import com.coretool.param.ui.vo.ParameterSyncParameterOption;
import com.coretool.param.ui.vo.ParameterSyncResultPayload;
import com.coretool.param.ui.vo.ParameterSyncTypeOption;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 参数同步（跨版本复制子集）。
 *
 * @since 2026-05-21
 */
@RestController
@RequestMapping("/api/v1/products/{productId}/versions")
public class ParameterSyncController {

    private final ParameterVersionCopyAppService parameterVersionCopyAppService;

    /**
     * 构造控制器。
     *
     * @param parameterVersionCopyAppService 参数版本复制服务
     */
    public ParameterSyncController(ParameterVersionCopyAppService parameterVersionCopyAppService) {
        this.parameterVersionCopyAppService = parameterVersionCopyAppService;
    }

    /**
     * 源版本下可选「命令 + 类型」列表。
     *
     * @param productId       产品 ID
     * @param sourceVersionId 源版本 ID
     * @return 类型选项
     */
    @GetMapping(value = "/{sourceVersionId}/parameter-sync/type-options", produces = "application/json; charset=utf-8")
    public ResponseObject<List<ParameterSyncTypeOption>> typeOptions(
            @PathVariable("productId") String productId,
            @PathVariable("sourceVersionId") String sourceVersionId) {
        return new ResponseObject<List<ParameterSyncTypeOption>>()
                .success(parameterVersionCopyAppService.listTypeOptions(productId, sourceVersionId));
    }

    /**
     * 源版本 + 命令 + 类型下可选参数。
     *
     * @param productId       产品 ID
     * @param sourceVersionId 源版本 ID
     * @param commandId       命令 ID
     * @param commandTypeId   类型 ID 或枚举前缀
     * @return 参数选项
     */
    @GetMapping(value = "/{sourceVersionId}/parameter-sync/parameters", produces = "application/json; charset=utf-8")
    public ResponseObject<List<ParameterSyncParameterOption>> parameters(
            @PathVariable("productId") String productId,
            @PathVariable("sourceVersionId") String sourceVersionId,
            @RequestParam("commandId") String commandId,
            @RequestParam("commandTypeId") String commandTypeId) {
        return new ResponseObject<List<ParameterSyncParameterOption>>()
                .success(
                        parameterVersionCopyAppService.listParameterOptions(
                                productId, sourceVersionId, commandId, commandTypeId));
    }

    /**
     * 执行参数同步至目标版本。
     *
     * @param productId       产品 ID
     * @param targetVersionId 目标（当前）版本 ID
     * @param request         同步请求
     * @param creatorId       操作人（可选）
     * @param updaterId       操作人（可选）
     * @return 同步结果
     */
    @PostMapping(
            value = "/{targetVersionId}/parameter-sync/commands",
            consumes = "application/json",
            produces = "application/json; charset=utf-8")
    public ResponseObject<ParameterSyncResultPayload> syncCommands(
            @PathVariable("productId") String productId,
            @PathVariable("targetVersionId") String targetVersionId,
            @RequestBody ParameterSyncCommandRequest request,
            @RequestParam(value = "creatorId", required = false) String creatorId,
            @RequestParam(value = "updaterId", required = false) String updaterId) {
        String op = StringUtils.defaultIfBlank(StringUtils.firstNonBlank(updaterId, creatorId), "system");
        ParameterSyncResultPayload data =
                parameterVersionCopyAppService.syncMany(productId, targetVersionId, request, op);
        ResponseObject<ParameterSyncResultPayload> resp = new ResponseObject<>();
        if (data.getSuccessCount() > 0) {
            resp.setSuccess(true);
            if (data.getFailureCount() > 0) {
                resp.setMessage(
                        "成功 "
                                + data.getSuccessCount()
                                + " 条，失败 "
                                + data.getFailureCount()
                                + " 条");
            } else {
                resp.setMessage(CommonConst.OK);
            }
        } else {
            resp.setSuccess(false);
            resp.setMessage(
                    data.getFailures() != null && !data.getFailures().isEmpty()
                            ? data.getFailures().get(0).getReason()
                            : "同步失败");
        }
        resp.setData(data);
        return resp;
    }
}
