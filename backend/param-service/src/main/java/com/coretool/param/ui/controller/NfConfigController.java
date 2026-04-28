package com.coretool.param.ui.controller;

import com.coretool.param.application.service.NfConfigAppService;
import com.coretool.param.infrastructure.persistence.entity.EntityNfConfigDictPo;
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
 * NF 配置字典接口。
 *
 * @since 2026-04-24
 */
@RestController
@RequestMapping("/api/v1/products/{productId}/nf-configs")
public class NfConfigController {

    private final NfConfigAppService nfConfigAppService;

    /**
     * 构造控制器。
     *
     * @param nfConfigAppService NF 配置应用服务
     */
    public NfConfigController(NfConfigAppService nfConfigAppService) {
        this.nfConfigAppService = nfConfigAppService;
    }

    /**
     * 分页查询 NF 配置字典。
     *
     * @param productId 路径中的产品 ID
     * @param query 分页与名称关键字，字段见 ListPageWithKeywordQuery
     * @return 分页结果
     */
    @GetMapping(produces = "application/json; charset=utf-8")
    public ResponseObject<PageResponse<EntityNfConfigDictPo>> page(
            @PathVariable("productId") String productId, @ModelAttribute ListPageWithKeywordQuery query) {
        return new ResponseObject<PageResponse<EntityNfConfigDictPo>>()
                .success(nfConfigAppService.page(productId, query));
    }

    /**
     * 新增 NF 配置字典项。
     *
     * @param productId 产品 ID
     * @param request   请求体
     * @return 新增后的数据
     */
    @PostMapping(consumes = "application/json", produces = "application/json; charset=utf-8")
    public ResponseObject<EntityNfConfigDictPo> create(
            @PathVariable("productId") String productId, @RequestBody EntityNfConfigDictPo request) {
        return new ResponseObject<EntityNfConfigDictPo>().success(nfConfigAppService.create(productId, request));
    }

    /**
     * 更新 NF 配置字典项。
     *
     * @param productId  产品 ID
     * @param nfConfigId 配置 ID
     * @param request    请求体
     * @return 更新后的数据
     */
    @PutMapping(value = "/{nfConfigId}", consumes = "application/json", produces = "application/json; charset=utf-8")
    public ResponseObject<EntityNfConfigDictPo> update(
            @PathVariable("productId") String productId,
            @PathVariable("nfConfigId") String nfConfigId,
            @RequestBody EntityNfConfigDictPo request) {
        return new ResponseObject<EntityNfConfigDictPo>()
                .success(nfConfigAppService.update(productId, nfConfigId, request));
    }

    /**
     * 禁用 NF 配置字典项。
     *
     * @param productId  产品 ID
     * @param nfConfigId 配置 ID
     * @return 操作结果
     */
    @DeleteMapping(value = "/{nfConfigId}", produces = "application/json; charset=utf-8")
    public ResponseObject<Void> disable(
            @PathVariable("productId") String productId, @PathVariable("nfConfigId") String nfConfigId) {
        nfConfigAppService.disable(productId, nfConfigId);
        return new ResponseObject<Void>().success("已禁用");
    }

    /**
     * 导入 NF 配置字典（文件上传）。
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
        return new ResponseObject<BatchImportResult>()
                .success(nfConfigAppService.importCsv(productId, file.getBytes()));
    }

    /**
     * 下载导入模板。
     *
     * @return 模板文件
     */
    @GetMapping(value = "/import-templates", produces = ExcelHelper.XLSX_CONTENT_TYPE)
    public ResponseEntity<byte[]> importTemplate() {
        return CsvDownload.attachment(nfConfigAppService.templateCsv(), "nf-configs-template.xlsx");
    }

    /**
     * 导出 NF 配置字典。
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
        return CsvDownload.attachment(nfConfigAppService.exportCsv(productId, page, size), "nf-configs.xlsx");
    }
}
