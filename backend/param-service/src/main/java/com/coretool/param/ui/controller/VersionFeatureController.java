package com.coretool.param.ui.controller;

import com.coretool.param.application.service.VersionFeatureAppService;
import com.coretool.param.infrastructure.persistence.entity.VersionFeatureDictPo;
import com.coretool.param.infrastructure.util.ExcelHelper;
import com.coretool.param.ui.response.BatchImportResult;
import com.coretool.param.ui.response.PageResponse;
import com.coretool.param.ui.response.ResponseObject;
import com.coretool.param.ui.vo.ListPageWithKeywordQuery;

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
 * 版本特性字典接口（版本维度）。
 *
 * @since 2026-04-24
 */
@RestController
@RequestMapping("/api/v1/products/{productId}/versions/{versionId}/features")
public class VersionFeatureController {

    private final VersionFeatureAppService versionFeatureAppService;

    /**
     * 构造控制器。
     *
     * @param versionFeatureAppService 版本特性应用服务
     */
    public VersionFeatureController(VersionFeatureAppService versionFeatureAppService) {
        this.versionFeatureAppService = versionFeatureAppService;
    }

    /**
     * 分页查询版本特性列表。
     *
     * @param productId 路径中的产品 ID
     * @param versionId 路径中的版本 ID
     * @param query 分页与名称关键字，字段见 ListPageWithKeywordQuery
     * @return 分页结果
     */
    @GetMapping(produces = "application/json; charset=utf-8")
    public ResponseObject<PageResponse<VersionFeatureDictPo>> page(
            @PathVariable("productId") String productId,
            @PathVariable("versionId") String versionId,
            @ModelAttribute ListPageWithKeywordQuery query) {
        return new ResponseObject<PageResponse<VersionFeatureDictPo>>()
                .success(versionFeatureAppService.page(productId, versionId, query));
    }

    /**
     * 新增版本特性。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @param request   请求体
     * @return 新增后的数据
     */
    @PostMapping(consumes = "application/json", produces = "application/json; charset=utf-8")
    public ResponseObject<VersionFeatureDictPo> create(
            @PathVariable("productId") String productId,
            @PathVariable("versionId") String versionId,
            @RequestBody VersionFeatureDictPo request) {
        return new ResponseObject<VersionFeatureDictPo>()
                .success(versionFeatureAppService.create(productId, versionId, request));
    }

    /**
     * 更新版本特性。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @param featureId 特性 ID
     * @param request   请求体
     * @return 更新后的数据
     */
    @PutMapping(value = "/{featureId}", consumes = "application/json", produces = "application/json; charset=utf-8")
    public ResponseObject<VersionFeatureDictPo> update(
            @PathVariable("productId") String productId,
            @PathVariable("versionId") String versionId,
            @PathVariable("featureId") String featureId,
            @RequestBody VersionFeatureDictPo request) {
        return new ResponseObject<VersionFeatureDictPo>()
                .success(versionFeatureAppService.update(productId, versionId, featureId, request));
    }

    /**
     * 禁用版本特性。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @param featureId 特性 ID
     * @return 操作结果
     */
    @DeleteMapping(value = "/{featureId}", produces = "application/json; charset=utf-8")
    public ResponseObject<Void> disable(
            @PathVariable("productId") String productId,
            @PathVariable("versionId") String versionId,
            @PathVariable("featureId") String featureId) {
        versionFeatureAppService.disable(productId, versionId, featureId);
        return new ResponseObject<Void>().success("已禁用");
    }

    /**
     * 导入版本特性字典（文件上传）。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @param file      上传文件
     * @return 导入结果
     * @throws Exception 文件解析或导入异常
     */
    @PostMapping(
            value = "/imports",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "application/json; charset=utf-8")
    public ResponseObject<BatchImportResult> importCsv(
            @PathVariable("productId") String productId,
            @PathVariable("versionId") String versionId,
            @RequestPart("file") MultipartFile file)
            throws Exception {
        return new ResponseObject<BatchImportResult>()
                .success(versionFeatureAppService.importCsv(productId, versionId, file.getBytes()));
    }

    /**
     * 下载导入模板。
     *
     * @return 模板文件
     */
    @GetMapping(value = "/import-templates", produces = ExcelHelper.XLSX_CONTENT_TYPE)
    public ResponseEntity<byte[]> importTemplate() {
        return CsvDownload.attachment(versionFeatureAppService.templateCsv(), "version-features-template.xlsx");
    }

    /**
     * 导出版本特性字典。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @param page      页码（从 1 开始）
     * @param size      页大小
     * @return 导出文件
     */
    @GetMapping(value = "/exports", produces = ExcelHelper.XLSX_CONTENT_TYPE)
    public ResponseEntity<byte[]> exportCsv(
            @PathVariable("productId") String productId,
            @PathVariable("versionId") String versionId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "5000") int size) {
        return CsvDownload.attachment(
                versionFeatureAppService.exportCsv(productId, versionId, page, size), "version-features.xlsx");
    }
}
