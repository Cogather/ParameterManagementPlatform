package com.coretool.param.ui.controller;

import com.coretool.param.application.service.CommandAppService;
import com.coretool.param.infrastructure.persistence.entity.EntityCommandMappingPo;
import com.coretool.param.infrastructure.util.ExcelHelper;
import com.coretool.param.ui.response.BatchImportResult;
import com.coretool.param.ui.response.PageResponse;
import com.coretool.param.ui.response.ResponseObject;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import com.coretool.param.ui.vo.ListPageWithKeywordQuery;

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
 * 命令管理接口。
 *
 * @since 2026-04-24
 */
@RestController
@RequestMapping("/api/v1/products/{productId}/commands")
@Tag(name = "CommandController", description = "命令管理")
public class CommandController {

    private final CommandAppService commandAppService;

    /**
     * 构造控制器。
     *
     * @param commandAppService 命令应用服务
     */
    public CommandController(CommandAppService commandAppService) {
        this.commandAppService = commandAppService;
    }

    /**
     * 分页查询命令。
     *
     * @param productId 路径中的产品 ID
     * @param query 分页与名称关键字，字段见 ListPageWithKeywordQuery
     * @return 分页结果
     */
    @Operation(summary = "分页查询命令")
    @GetMapping(produces = "application/json; charset=utf-8")
    public ResponseObject<PageResponse<EntityCommandMappingPo>> page(
            @PathVariable("productId") String productId, @ModelAttribute ListPageWithKeywordQuery query) {
        return new ResponseObject<PageResponse<EntityCommandMappingPo>>()
                .success(commandAppService.page(productId, query));
    }

    /**
     * 创建命令。
     *
     * @param productId 产品 ID
     * @param request   请求体
     * @return 新增后的数据
     */
    @Operation(summary = "创建命令")
    @PostMapping(consumes = "application/json", produces = "application/json; charset=utf-8")
    public ResponseObject<EntityCommandMappingPo> create(
            @PathVariable("productId") String productId, @RequestBody EntityCommandMappingPo request) {
        return new ResponseObject<EntityCommandMappingPo>().success(commandAppService.create(productId, request));
    }

    /**
     * 更新命令。
     *
     * @param productId 产品 ID
     * @param commandId 命令 ID
     * @param request   请求体
     * @return 更新后的数据
     */
    @Operation(summary = "更新命令")
    @PutMapping(value = "/{commandId}", consumes = "application/json", produces = "application/json; charset=utf-8")
    public ResponseObject<EntityCommandMappingPo> update(
            @PathVariable("productId") String productId,
            @PathVariable("commandId") String commandId,
            @RequestBody EntityCommandMappingPo request) {
        return new ResponseObject<EntityCommandMappingPo>()
                .success(commandAppService.update(productId, commandId, request));
    }

    /**
     * 禁用命令。
     *
     * @param productId 产品 ID
     * @param commandId 命令 ID
     * @return 操作结果
     */
    @Operation(summary = "禁用命令")
    @DeleteMapping(value = "/{commandId}", produces = "application/json; charset=utf-8")
    public ResponseObject<Void> disable(
            @PathVariable("productId") String productId, @PathVariable("commandId") String commandId) {
        commandAppService.disable(productId, commandId);
        return new ResponseObject<Void>().success("已禁用");
    }

    /**
     * 导入命令（文件上传）。
     *
     * @param productId 产品 ID
     * @param file      上传文件
     * @return 导入结果
     * @throws Exception 文件解析或导入异常
     */
    @Operation(summary = "导入命令")
    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "application/json; charset=utf-8")
    public ResponseObject<BatchImportResult> importFile(
            @PathVariable("productId") String productId, @RequestPart("file") MultipartFile file) throws Exception {
        return new ResponseObject<BatchImportResult>()
                .success(commandAppService.importExcel(productId, file.getBytes()));
    }

    /**
     * 下载导入模板。
     *
     * @return 模板文件
     */
    @Operation(summary = "导入模板")
    @GetMapping(value = "/import-templates", produces = ExcelHelper.XLSX_CONTENT_TYPE)
    public ResponseEntity<byte[]> importTemplate() {
        return CsvDownload.attachment(commandAppService.importTemplate(), "commands-import-template.xlsx");
    }

    /**
     * 导出命令。
     *
     * @param productId 产品 ID
     * @param page      页码（从 1 开始）
     * @param size      页大小
     * @param keyword   关键字（可选）
     * @return 导出文件
     */
    @Operation(summary = "导出命令")
    @GetMapping(value = "/export", produces = ExcelHelper.XLSX_CONTENT_TYPE)
    public ResponseEntity<byte[]> export(
            @PathVariable("productId") String productId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "5000") int size,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return CsvDownload.attachment(commandAppService.exportExcel(productId, page, size, keyword), "commands.xlsx");
    }
}

