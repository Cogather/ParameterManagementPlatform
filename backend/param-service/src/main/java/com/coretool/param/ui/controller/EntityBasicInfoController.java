package com.coretool.param.ui.controller;

import com.coretool.param.application.service.EntityBasicInfoAppService;
import com.coretool.param.infrastructure.persistence.entity.EntityBasicInfoPo;
import com.coretool.param.ui.response.PageResponse;
import com.coretool.param.ui.response.ResponseObject;
import com.coretool.param.ui.vo.EntityBasicInfoListQuery;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 产品主数据（entity_basic_info）维护；不位于 /products/{productId} 之下，供「产品配置」独立页使用。
 */
@RestController
@RequestMapping("/api/v1/entity-basic-infos")
public class EntityBasicInfoController {

    private final EntityBasicInfoAppService entityBasicInfoAppService;

    /**
     * 构造控制器。
     *
     * @param entityBasicInfoAppService 产品主数据应用服务
     */
    public EntityBasicInfoController(EntityBasicInfoAppService entityBasicInfoAppService) {
        this.entityBasicInfoAppService = entityBasicInfoAppService;
    }

    /**
     * 分页查询产品主数据。
     *
     * @param query 产品主数据列表查询，字段见 EntityBasicInfoListQuery
     * @return 分页结果
     */
    @GetMapping(produces = "application/json; charset=utf-8")
    public ResponseObject<PageResponse<EntityBasicInfoPo>> page(
            @ModelAttribute EntityBasicInfoListQuery query) {
        return new ResponseObject<PageResponse<EntityBasicInfoPo>>()
                .success(entityBasicInfoAppService.page(query));
    }

    /**
     * 查询产品下拉选项（按产品 ID 聚合）。
     *
     * @return 产品下拉选项
     */
    @GetMapping(value = "/product-choices", produces = "application/json; charset=utf-8")
    public ResponseObject<List<EntityBasicInfoPo>> productChoices() {
        return new ResponseObject<List<EntityBasicInfoPo>>()
                .success(entityBasicInfoAppService.listProductChoices());
    }

    /**
     * 新增产品主数据。
     *
     * @param request 请求体
     * @return 新增后的数据
     */
    @PostMapping(consumes = "application/json", produces = "application/json; charset=utf-8")
    public ResponseObject<EntityBasicInfoPo> create(@RequestBody EntityBasicInfoPo request) {
        return new ResponseObject<EntityBasicInfoPo>().success(entityBasicInfoAppService.create(request));
    }

    /**
     * 更新产品主数据（按产品形态维度）。
     *
     * @param productFormId 产品形态 ID
     * @param request       请求体
     * @return 更新后的数据
     */
    @PutMapping(
            value = "/{productFormId}",
            consumes = "application/json",
            produces = "application/json; charset=utf-8")
    public ResponseObject<EntityBasicInfoPo> update(
            @PathVariable("productFormId") String productFormId,
            @RequestBody EntityBasicInfoPo request) {
        return new ResponseObject<EntityBasicInfoPo>()
                .success(entityBasicInfoAppService.update(productFormId, request));
    }

    /**
     * 删除产品主数据（软删除）。
     *
     * @param productFormId 产品形态 ID
     * @return 操作结果
     */
    @DeleteMapping(value = "/{productFormId}", produces = "application/json; charset=utf-8")
    public ResponseObject<Void> delete(@PathVariable("productFormId") String productFormId) {
        entityBasicInfoAppService.softDelete(productFormId);
        return new ResponseObject<Void>().success("已删除");
    }
}
