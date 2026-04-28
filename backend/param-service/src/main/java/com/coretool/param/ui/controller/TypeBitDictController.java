package com.coretool.param.ui.controller;

import com.coretool.param.application.service.TypeBitDictAppService;
import com.coretool.param.infrastructure.persistence.entity.TypeBitDictPo;
import com.coretool.param.ui.response.ResponseObject;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 类型 BIT 字典接口。
 *
 * @since 2026-04-24
 */
@RestController
@RequestMapping("/api/v1/type-bits")
public class TypeBitDictController {

    private final TypeBitDictAppService appService;

    /**
     * 构造控制器。
     *
     * @param appService 类型 BIT 字典应用服务
     */
    public TypeBitDictController(TypeBitDictAppService appService) {
        this.appService = appService;
    }

    /**
     * 查询全部类型 BIT 字典项。
     *
     * @return 列表结果
     */
    @GetMapping(produces = "application/json; charset=utf-8")
    public ResponseObject<List<TypeBitDictPo>> listAll() {
        return new ResponseObject<List<TypeBitDictPo>>().success(appService.listAll());
    }
}

