package com.coretool.param.ui.controller;

import com.coretool.param.application.service.ParameterAppService;
import com.coretool.param.infrastructure.persistence.entity.SystemParameterPo;
import com.coretool.param.ui.response.PageResponse;
import com.coretool.param.ui.response.ResponseObject;
import com.coretool.param.ui.vo.BaselineCountPayload;
import com.coretool.param.ui.vo.ParameterPageQuery;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

/**
 * 全产品参数列表（版本下拉 ALL）：不按版本过滤。
 */
@RestController
@RequestMapping("/api/v1/products/{productId}/parameters")
@Tag(name = "ProductParameterController", description = "全产品参数聚合查询")
public class ProductParameterController {

    @Resource
    private ParameterAppService parameterAppService;

    /**
     * 分页查询产品下全部版本的参数。
     *
     * @param productId 路径中的产品 ID
     * @param query 全产品参数分页与筛选，字段见 ParameterPageQuery
     * @return 分页结果
     */
    @Operation(summary = "分页查询产品下全部版本的参数")
    @GetMapping(produces = "application/json; charset=utf-8")
    public ResponseObject<PageResponse<SystemParameterPo>> page(
            @PathVariable("productId") String productId,
            @ModelAttribute ParameterPageQuery query) {
        return new ResponseObject<PageResponse<SystemParameterPo>>()
                .success(parameterAppService.pageByProduct(productId, query));
    }

    /**
     * 查询产品维度已基线参数总数（ALL 视图）。
     *
     * @param productId 产品 ID
     * @return 基线数量
     */
    @Operation(summary = "产品维度已基线参数总数（ALL 视图）")
    @GetMapping(value = "/baseline-count", produces = "application/json; charset=utf-8")
    public ResponseObject<BaselineCountPayload> baselineCount(@PathVariable("productId") String productId) {
        long n = parameterAppService.countBaselineInProduct(productId);
        return new ResponseObject<BaselineCountPayload>().success(new BaselineCountPayload(n));
    }
}
