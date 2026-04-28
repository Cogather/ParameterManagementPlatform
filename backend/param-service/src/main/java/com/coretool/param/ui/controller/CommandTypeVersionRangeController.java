package com.coretool.param.ui.controller;

import com.coretool.param.application.service.CommandTypeVersionRangeAppService;
import com.coretool.param.infrastructure.persistence.entity.CommandTypeVersionRangePo;
import com.coretool.param.infrastructure.util.ExcelHelper;
import com.coretool.param.ui.response.BatchImportResult;
import com.coretool.param.ui.response.PageResponse;
import com.coretool.param.ui.response.ResponseObject;
import com.coretool.param.ui.vo.ListPageWithTypeFilterQuery;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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
 * 命令类型版本区段接口。
 *
 * @since 2026-04-24
 */
@RestController
@RequestMapping("/api/v1/products/{productId}/command-type-version-ranges")
@Tag(name = "CommandTypeVersionRangeController", description = "类型版本区段")
public class CommandTypeVersionRangeController {

    private final CommandTypeVersionRangeAppService appService;

    /**
     * 构造控制器。
     *
     * @param appService 类型版本区段应用服务
     */
    public CommandTypeVersionRangeController(CommandTypeVersionRangeAppService appService) {
        this.appService = appService;
    }

    /**
     * 分页查询版本区段。
     *
     * @param productId 路径中的产品 ID
     * @param query 分页与类型过滤，字段见 ListPageWithTypeFilterQuery
     * @return 分页结果
     */
    @Operation(summary = "分页查询版本区段")
    @GetMapping(produces = "application/json; charset=utf-8")
    public ResponseObject<PageResponse<CommandTypeVersionRangePo>> page(
            @PathVariable("productId") String productId, @ModelAttribute ListPageWithTypeFilterQuery query) {
        return new ResponseObject<PageResponse<CommandTypeVersionRangePo>>()
                .success(appService.page(productId, query));
    }

    /**
     * 创建版本区段。
     *
     * @param productId 产品 ID
     * @param request   请求体
     * @return 新增后的数据
     */
    @Operation(summary = "创建版本区段")
    @PostMapping(consumes = "application/json", produces = "application/json; charset=utf-8")
    public ResponseObject<CommandTypeVersionRangePo> create(
            @PathVariable("productId") String productId, @RequestBody CommandTypeVersionRangePo request) {
        return new ResponseObject<CommandTypeVersionRangePo>().success(appService.create(productId, request));
    }

    /**
     * 更新版本区段。
     *
     * @param productId 产品 ID
     * @param rangeId   区段 ID
     * @param request   请求体
     * @return 更新后的数据
     */
    @Operation(summary = "更新版本区段")
    @PutMapping(value = "/{rangeId}", consumes = "application/json", produces = "application/json; charset=utf-8")
    public ResponseObject<CommandTypeVersionRangePo> update(
            @PathVariable("productId") String productId,
            @PathVariable("rangeId") String rangeId,
            @RequestBody CommandTypeVersionRangePo request) {
        return new ResponseObject<CommandTypeVersionRangePo>().success(appService.update(productId, rangeId, request));
    }

    /**
     * 禁用版本区段。
     *
     * @param productId 产品 ID
     * @param rangeId   区段 ID
     * @return 操作结果
     */
    @Operation(summary = "禁用版本区段")
    @DeleteMapping(value = "/{rangeId}", produces = "application/json; charset=utf-8")
    public ResponseObject<Void> disable(
            @PathVariable("productId") String productId, @PathVariable("rangeId") String rangeId) {
        appService.disable(productId, rangeId);
        return new ResponseObject<Void>().success("已禁用");
    }

    /**
     * 导入版本区段（文件上传）。
     *
     * @param productId 产品 ID
     * @param file      上传文件
     * @return 导入结果
     * @throws Exception 文件解析或导入异常
     */
    @Operation(summary = "导入版本区段")
    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "application/json; charset=utf-8")
    public ResponseObject<BatchImportResult> importFile(
            @PathVariable("productId") String productId, @RequestPart("file") MultipartFile file) throws Exception {
        return new ResponseObject<BatchImportResult>().success(appService.importExcel(productId, file.getBytes()));
    }

    /**
     * 下载导入模板。
     *
     * @return 模板文件
     */
    @Operation(summary = "导入模板")
    @GetMapping(value = "/import-templates", produces = ExcelHelper.XLSX_CONTENT_TYPE)
    public ResponseEntity<byte[]> importTemplate() {
        return CsvDownload.attachment(appService.importTemplate(), "command-type-version-ranges-import-template.xlsx");
    }

    /**
     * 导出版本区段。
     *
     * @param productId   产品 ID
     * @param page        页码（从 1 开始）
     * @param size        页大小
     * @param ownedTypeId 归属类型 ID（可选）
     * @return 导出文件
     */
    @Operation(summary = "导出版本区段")
    @GetMapping(value = "/export", produces = ExcelHelper.XLSX_CONTENT_TYPE)
    public ResponseEntity<byte[]> export(
            @PathVariable("productId") String productId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "5000") int size,
            @RequestParam(value = "ownedTypeId", required = false) String ownedTypeId) {
        return CsvDownload.attachment(
                appService.exportExcel(productId, page, size, ownedTypeId), "command-type-version-ranges.xlsx");
    }
}

