package com.coretool.param.ui.controller;

import com.coretool.param.application.service.ParameterAppService;
import com.coretool.param.infrastructure.persistence.entity.SystemParameterPo;
import com.coretool.param.infrastructure.util.ExcelHelper;
import com.coretool.param.ui.response.AvailableBitsData;
import com.coretool.param.ui.response.AvailableSequencesData;
import com.coretool.param.ui.response.BatchImportResult;
import com.coretool.param.ui.response.PageResponse;
import com.coretool.param.ui.response.ResponseObject;
import com.coretool.param.ui.vo.AvailableBitsQuery;
import com.coretool.param.ui.vo.BaselineCountPayload;
import com.coretool.param.ui.vo.ParameterCommandFilterQuery;
import com.coretool.param.ui.vo.ParameterPageQuery;
import com.coretool.param.ui.vo.ParameterSaveRequest;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 参数管理接口（版本维度）。
 *
 * @since 2026-04-24
 */
@RestController
@RequestMapping("/api/v1/products/{productId}/versions/{versionId}/parameters")
public class ParameterController {

    private final ParameterAppService parameterAppService;

    /**
     * 构造控制器。
     *
     * @param parameterAppService 参数应用服务
     */
    public ParameterController(ParameterAppService parameterAppService) {
        this.parameterAppService = parameterAppService;
    }

    /**
     * 分页查询参数列表。
     *
     * @param productId 路径中的产品 ID
     * @param versionId 路径中的版本 ID
     * @param query 参数分页与命令/类型筛选，字段见 ParameterPageQuery
     * @return 分页结果
     */
    @GetMapping(produces = "application/json; charset=utf-8")
    public ResponseObject<PageResponse<SystemParameterPo>> page(
            @PathVariable("productId") String productId,
            @PathVariable("versionId") String versionId,
            @ModelAttribute ParameterPageQuery query) {
        return new ResponseObject<PageResponse<SystemParameterPo>>()
                .success(parameterAppService.page(productId, versionId, query));
    }

    /**
     * 查询可用序号集合。
     *
     * @param productId 路径中的产品 ID
     * @param versionId 路径中的版本 ID
     * @param filter 命令与类型筛选，字段见 ParameterCommandFilterQuery
     * @return 可用序号数据
     */
    @GetMapping(value = "/available-sequences", produces = "application/json; charset=utf-8")
    public ResponseObject<AvailableSequencesData> availableSequences(
            @PathVariable("productId") String productId,
            @PathVariable("versionId") String versionId,
            @ModelAttribute ParameterCommandFilterQuery filter) {
        String typeKey =
                CommandTypeQueryParam.requireTypeKey(
                        filter.getCommandTypeId(), filter.getCommandTypeCode());
        return new ResponseObject<AvailableSequencesData>()
                .success(
                        parameterAppService.availableSequences(
                                productId, versionId, filter.getCommandId(), typeKey));
    }

    /**
     * 查询指定参数序号下的可用 BIT 集合。
     *
     * @param productId 路径中的产品 ID
     * @param versionId 路径中的版本 ID
     * @param query 命令、类型与参数序号，字段见 AvailableBitsQuery
     * @return 可用 BIT 数据
     */
    @GetMapping(value = "/available-bits", produces = "application/json; charset=utf-8")
    public ResponseObject<AvailableBitsData> availableBits(
            @PathVariable("productId") String productId,
            @PathVariable("versionId") String versionId,
            @ModelAttribute AvailableBitsQuery query) {
        String typeKey = CommandTypeQueryParam.requireTypeKey(query.getCommandTypeId(), query.getCommandTypeCode());
        return new ResponseObject<AvailableBitsData>()
                .success(
                        parameterAppService.availableBits(
                                productId, versionId, query.getCommandId(), typeKey, query.getSequence()));
    }

    /**
     * 统计指定版本中基线参数数量。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @return 基线数量
     */
    @GetMapping(value = "/baseline-count", produces = "application/json; charset=utf-8")
    public ResponseObject<BaselineCountPayload> baselineCount(
            @PathVariable("productId") String productId, @PathVariable("versionId") String versionId) {
        long n = parameterAppService.countBaselineInVersion(productId, versionId);
        return new ResponseObject<BaselineCountPayload>().success(new BaselineCountPayload(n));
    }

    /**
     * 新增参数。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @param request   保存请求
     * @return 新增后的数据
     */
    @PostMapping(consumes = "application/json", produces = "application/json; charset=utf-8")
    public ResponseObject<SystemParameterPo> create(
            @PathVariable("productId") String productId,
            @PathVariable("versionId") String versionId,
            @RequestBody ParameterSaveRequest request) {
        return new ResponseObject<SystemParameterPo>().success(parameterAppService.create(productId, versionId, request));
    }

    /**
     * 更新参数。
     *
     * @param productId    产品 ID
     * @param versionId    版本 ID
     * @param parameterId  参数 ID
     * @param request      保存请求
     * @return 更新后的数据
     */
    @PutMapping(value = "/{parameterId}", consumes = "application/json", produces = "application/json; charset=utf-8")
    public ResponseObject<SystemParameterPo> update(
            @PathVariable("productId") String productId,
            @PathVariable("versionId") String versionId,
            @PathVariable("parameterId") Integer parameterId,
            @RequestBody ParameterSaveRequest request) {
        return new ResponseObject<SystemParameterPo>()
                .success(parameterAppService.update(productId, versionId, parameterId, request));
    }

    /**
     * 删除参数。
     *
     * @param productId   产品 ID
     * @param versionId   版本 ID
     * @param parameterId 参数 ID
     * @return 操作结果
     */
    @DeleteMapping(value = "/{parameterId}", produces = "application/json; charset=utf-8")
    public ResponseObject<Void> delete(
            @PathVariable("productId") String productId,
            @PathVariable("versionId") String versionId,
            @PathVariable("parameterId") Integer parameterId) {
        parameterAppService.delete(productId, versionId, parameterId);
        return new ResponseObject<Void>().success("已删除");
    }

    /**
     * 导入参数（文件上传）。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @param file      上传文件
     * @return 导入结果
     * @throws Exception 文件解析或导入异常
     */
    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "application/json; charset=utf-8")
    public ResponseObject<BatchImportResult> importFile(
            @PathVariable("productId") String productId,
            @PathVariable("versionId") String versionId,
            @RequestParam("mode") String mode,
            @RequestParam("commandId") String commandId,
            @RequestParam(value = "commandTypeCode", required = false) String commandTypeCode,
            @RequestPart("file") MultipartFile file)
            throws Exception {
        return new ResponseObject<BatchImportResult>()
                .success(
                        parameterAppService.importParameters(
                                productId, versionId, mode, commandId, commandTypeCode, file.getBytes()));
    }

    /**
     * 导出参数。
     *
     * @param productId 路径中的产品 ID
     * @param versionId 路径中的版本 ID
     * @param filter 命令与类型筛选，字段见 ParameterCommandFilterQuery
     * @return 导出文件
     */
    @GetMapping(value = "/export", produces = ExcelHelper.XLSX_CONTENT_TYPE)
    public ResponseEntity<byte[]> export(
            @PathVariable("productId") String productId,
            @PathVariable("versionId") String versionId,
            @ModelAttribute ParameterCommandFilterQuery filter) {
        String typeKey =
                CommandTypeQueryParam.optionalTypeKey(
                        filter.getCommandTypeId(), filter.getCommandTypeCode());
        return CsvDownload.attachment(
                parameterAppService.export(
                        productId, versionId, filter.getCommandId(), typeKey),
                "parameters.xlsx");
    }

    /**
     * 下载导入模板。
     *
     * @return 模板文件
     */
    @GetMapping(value = "/import-templates", produces = ExcelHelper.XLSX_CONTENT_TYPE)
    public ResponseEntity<byte[]> importTemplate() {
        return CsvDownload.attachment(parameterAppService.importTemplate(), "parameters-import-template.xlsx");
    }

    /**
     * 将指定参数设为基线。
     *
     * @param productId   产品 ID
     * @param versionId   版本 ID
     * @param parameterId 参数 ID
     * @return 操作结果
     */
    @PostMapping(value = "/{parameterId}/baseline", produces = "application/json; charset=utf-8")
    public ResponseObject<Void> baseline(
            @PathVariable("productId") String productId,
            @PathVariable("versionId") String versionId,
            @PathVariable("parameterId") Integer parameterId) {
        parameterAppService.baseline(productId, versionId, parameterId);
        return new ResponseObject<Void>().success("已基线");
    }

    /**
     * 解锁基线（恢复为可编辑/可删除）。
     *
     * @param productId   产品 ID
     * @param versionId   版本 ID
     * @param parameterId 参数 ID
     * @return 操作结果
     */
    @PostMapping(value = "/{parameterId}/unbaseline", produces = "application/json; charset=utf-8")
    public ResponseObject<Void> unbaseline(
            @PathVariable("productId") String productId,
            @PathVariable("versionId") String versionId,
            @PathVariable("parameterId") Integer parameterId) {
        parameterAppService.unbaseline(productId, versionId, parameterId);
        return new ResponseObject<Void>().success("已解锁");
    }
}
