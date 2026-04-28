package com.coretool.param.ui.controller;

import com.coretool.param.application.service.VersionAppService;
import com.coretool.param.infrastructure.persistence.entity.EntityVersionInfoPo;
import com.coretool.param.infrastructure.util.ExcelHelper;
import com.coretool.param.ui.response.BatchImportResult;
import com.coretool.param.ui.response.PageResponse;
import com.coretool.param.ui.response.ResponseObject;
import com.coretool.param.ui.vo.ListPageWithKeywordQuery;
import com.coretool.param.ui.vo.VersionBranchRequest;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 版本管理接口。
 *
 * @since 2026-04-24
 */
@RestController
@RequestMapping("/api/v1/products/{productId}/versions")
public class VersionController {
    private final VersionAppService versionAppService;

    /**
     * 构造控制器。
     *
     * @param versionAppService 版本应用服务
     */
    public VersionController(VersionAppService versionAppService) {
        this.versionAppService = versionAppService;
    }

    /**
     * 分页查询版本列表。
     *
     * @param productId 路径中的产品 ID
     * @param query 分页与名称关键字，字段见 ListPageWithKeywordQuery
     * @return 分页结果
     */
    @GetMapping(produces = "application/json; charset=utf-8")
    public ResponseObject<PageResponse<EntityVersionInfoPo>> page(
            @PathVariable("productId") String productId, @ModelAttribute ListPageWithKeywordQuery query) {
        return new ResponseObject<PageResponse<EntityVersionInfoPo>>()
                .success(versionAppService.page(productId, query));
    }

    /**
     * 新增版本。
     *
     * @param productId 产品 ID
     * @param request   请求体
     * @return 新增后的数据
     */
    @PostMapping(consumes = "application/json", produces = "application/json; charset=utf-8")
    public ResponseObject<EntityVersionInfoPo> create(
            @PathVariable("productId") String productId,
            @RequestBody EntityVersionInfoPo request) {
        return new ResponseObject<EntityVersionInfoPo>().success(versionAppService.create(productId, request));
    }

    /**
     * 基于指定基线创建分支版本。
     *
     * @param productId 产品 ID
     * @param request   分支请求
     * @return 新建的版本数据
     */
    @PostMapping(value = "/branch", consumes = "application/json", produces = "application/json; charset=utf-8")
    public ResponseObject<EntityVersionInfoPo> branch(
            @PathVariable("productId") String productId, @RequestBody VersionBranchRequest request) {
        return new ResponseObject<EntityVersionInfoPo>().success(versionAppService.branch(productId, request));
    }

    /**
     * 更新版本信息。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @param request   请求体
     * @return 更新后的数据
     */
    @PutMapping(value = "/{versionId}", consumes = "application/json", produces = "application/json; charset=utf-8")
    public ResponseObject<EntityVersionInfoPo> update(
            @PathVariable("productId") String productId,
            @PathVariable("versionId") String versionId,
            @RequestBody EntityVersionInfoPo request) {
        return new ResponseObject<EntityVersionInfoPo>()
                .success(versionAppService.update(productId, versionId, request));
    }

    /**
     * 禁用版本。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @return 操作结果
     */
    @DeleteMapping(value = "/{versionId}", produces = "application/json; charset=utf-8")
    public ResponseObject<Void> disable(
            @PathVariable("productId") String productId,
            @PathVariable("versionId") String versionId) {
        versionAppService.disable(productId, versionId);
        return new ResponseObject<Void>().success("已禁用");
    }

    /**
     * 导入版本列表（文件上传）。
     *
     * @param productId 产品 ID
     * @param file      上传文件
     * @return 导入结果
     * @throws Exception 文件解析或导入异常
     */
    @PostMapping(
            value = "/imports",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "application/json; charset=utf-8")
    public ResponseObject<BatchImportResult> importCsv(
            @PathVariable("productId") String productId, @RequestPart("file") MultipartFile file)
            throws Exception {
        byte[] bytes = file.getBytes();
        return new ResponseObject<BatchImportResult>().success(versionAppService.importCsv(productId, bytes));
    }

    /**
     * 下载导入模板。
     *
     * @return 模板文件
     */
    @GetMapping(value = "/import-templates", produces = ExcelHelper.XLSX_CONTENT_TYPE)
    public ResponseEntity<byte[]> importTemplate() {
        byte[] bytes = versionAppService.templateCsv();
        return CsvDownload.attachment(bytes, "version-import-template.xlsx");
    }

    /**
     * 导出版本列表。
     *
     * @param productId 产品 ID
     * @param page      页码（从 1 开始）
     * @param size      页大小
     * @return 导出文件
     */
    @GetMapping(value = "/exports", produces = ExcelHelper.XLSX_CONTENT_TYPE)
    public ResponseEntity<byte[]> exportCsv(
            @PathVariable("productId") String productId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "5000") int size) {
        byte[] bytes = versionAppService.exportCsvBody(productId, page, size);
        return CsvDownload.attachment(bytes, "versions.xlsx");
    }
}
