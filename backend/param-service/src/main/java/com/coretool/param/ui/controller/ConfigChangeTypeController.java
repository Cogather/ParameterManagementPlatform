package com.coretool.param.ui.controller;

import com.coretool.param.application.service.ConfigChangeTypeAppService;
import com.coretool.param.infrastructure.persistence.entity.ConfigChangeTypePo;
import com.coretool.param.ui.response.ResponseObject;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import jakarta.annotation.Resource;

/**
 * 变更类型字典（spec-03 §3.6）；供参数变更说明子表下拉。
 */
@RestController
@RequestMapping("/api/v1/config-change-types")
@Tag(name = "ConfigChangeTypeController", description = "变更类型字典")
public class ConfigChangeTypeController {

    @Resource
    private ConfigChangeTypeAppService configChangeTypeAppService;

    /**
     * 查询全部变更类型（按排序字段）。
     *
     * @return 变更类型列表
     */
    @Operation(summary = "查询全部变更类型（按排序字段）")
    @GetMapping(produces = "application/json; charset=utf-8")
    public ResponseObject<List<ConfigChangeTypePo>> listAll() {
        return new ResponseObject<List<ConfigChangeTypePo>>().success(configChangeTypeAppService.listAllOrdered());
    }
}
