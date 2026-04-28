package com.coretool.param.ui.controller;

import com.coretool.param.application.service.ParameterCommandTreeAppService;
import com.coretool.param.ui.response.ResponseObject;
import com.coretool.param.ui.vo.ParameterCommandTreeNode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import jakarta.annotation.Resource;

/**
 * 参数页左树：命令 → 类型（spec-03 §1.2）；无命令主数据时返回演示数据。
 */
@RestController
@RequestMapping("/api/v1/products/{productId}")
@Tag(name = "ParameterCommandTreeController", description = "参数页命令类型树")
public class ParameterCommandTreeController {

    @Resource
    private ParameterCommandTreeAppService parameterCommandTreeAppService;

    /**
     * 查询参数上下文树（命令 → 类型）。
     *
     * @param productId 产品 ID
     * @return 树节点列表
     */
    @Operation(summary = "参数上下文树（命令 → 类型）")
    @GetMapping(value = "/parameter-command-tree", produces = "application/json; charset=utf-8")
    public ResponseObject<List<ParameterCommandTreeNode>> tree(@PathVariable("productId") String productId) {
        return new ResponseObject<List<ParameterCommandTreeNode>>()
                .success(parameterCommandTreeAppService.treeForProduct(productId));
    }
}
