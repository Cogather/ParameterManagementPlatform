package com.coretool.param.application.service;

import com.coretool.param.application.support.ImportResultCollector;
import com.coretool.param.domain.config.version.ProductVersion;
import com.coretool.param.domain.config.version.service.ProductVersionDomainService;
import com.coretool.param.domain.config.version.repository.ProductVersionRepository;
import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.domain.support.IdGenerator;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.infrastructure.persistence.assembly.ProductVersionAssembler;
import com.coretool.param.infrastructure.persistence.entity.EntityVersionInfoPo;
import com.coretool.param.infrastructure.util.ExcelHelper;
import com.coretool.param.infrastructure.util.ExcelInstructions;
import com.coretool.param.ui.exception.BizException;
import com.coretool.param.ui.response.BatchImportResult;
import com.coretool.param.ui.response.PageResponse;
import com.coretool.param.ui.vo.ListPageWithKeywordQuery;
import com.coretool.param.ui.vo.VersionBranchRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VersionAppService {

    private static final String INSTRUCTION = ExcelInstructions.ID_CREATE_UPDATE_HINT;
    /**
     * 导入/导出字段以页面展示为准：不包含状态、审计字段。
     *
     * <p>兼容：导入时若上传文件额外包含“状态”列，也会被解析；若缺失则默认启用。
     */
    private static final List<String> HEADERS_CN =
            List.of("版本ID", "版本名称", "版本类型", "支持版本", "版本说明", "版本描述", "责任人");

    private final ProductVersionRepository productVersionRepository;
    private final ProductVersionDomainService domainService;
    private final OperationLogAppService operationLogAppService;

    /**
     * 构造函数。
     *
     * @param productVersionRepository 产品版本仓储
     * @param operationLogAppService   操作日志应用服务
     */
    public VersionAppService(
            ProductVersionRepository productVersionRepository, OperationLogAppService operationLogAppService) {
        this.productVersionRepository = productVersionRepository;
        this.domainService = new ProductVersionDomainService(productVersionRepository);
        this.operationLogAppService = operationLogAppService;
    }

    /**
     * 分页查询产品版本（产品维度）。
     *
     * @param productId 产品 ID
     * @param q         分页
     * @return 分页结果
     */
    public PageResponse<EntityVersionInfoPo> page(String productId, ListPageWithKeywordQuery q) {
        PageSlice<ProductVersion> slice =
                productVersionRepository.pageByProduct(productId, q.getPage(), q.getSize());
        PageResponse<EntityVersionInfoPo> resp = new PageResponse<>();
        resp.setRecords(
                slice.getRecords().stream()
                        .map(ProductVersionAssembler::toPo)
                        .collect(Collectors.toList()));
        resp.setTotal(slice.getTotal());
        resp.setPage(slice.getPage());
        resp.setSize(slice.getSize());
        return resp;
    }

    /**
     * 新增版本（若存在同名禁用项则复用并启用）。
     *
     * @param productId 产品 ID
     * @param input     请求体
     * @return 新增后的数据
     */
    @Transactional
    public EntityVersionInfoPo create(String productId, EntityVersionInfoPo input) {
        if (input == null) {
            throw new DomainRuleException("请求体不能为空");
        }
        String name = input.getVersionName() == null ? "" : input.getVersionName().trim();
        ProductVersion disabled = productVersionRepository.findDisabledByNameInProduct(productId, name).orElse(null);
        if (disabled != null) {
            ProductVersion before =
                    ProductVersion.rehydrate(
                            new ProductVersion.Snapshot(
                                    disabled.getOwnedProductId(),
                                    disabled.getVersionId(),
                                    disabled.getVersionName(),
                                    disabled.getVersionType(),
                                    disabled.getVersionDescription(),
                                    disabled.getBaselineVersionId(),
                                    disabled.getBaselineVersionName(),
                                    disabled.getVersionDesc(),
                                    disabled.getApprover(),
                                    disabled.getIsHidden(),
                                    disabled.getSupportedVersion(),
                                    disabled.getIntroducedProductId(),
                                    disabled.getOwnerList(),
                                    disabled.getVersionStatus(),
                                    disabled.getCreatorId(),
                                    disabled.getCreationTimestamp(),
                                    disabled.getUpdaterId(),
                                    disabled.getUpdateTimestamp()));
            // 恢复已删除版本：启用并更新可编辑字段（名称保持一致即可）
            if (StringUtils.isNotBlank(name) && !StringUtils.equals(name, disabled.getVersionName())) {
                // 理论上不会发生（按同名查找），兜底保持一致
                disabled.rename(name);
            }
            disabled.applyAttributePatch(
                    new ProductVersion.AttributePatch(
                            input.getSupportedVersion(),
                            input.getVersionDescription(),
                            input.getVersionDesc(),
                            1,
                            StringUtils.defaultIfBlank(input.getUpdaterId(), "system"),
                            LocalDateTime.now()));
            productVersionRepository.update(disabled);
            String opR = StringUtils.defaultIfBlank(input.getUpdaterId(), "system");
            operationLogAppService.logVersionUpdate(before, disabled, opR);
            return ProductVersionAssembler.toPo(disabled);
        }
        if (StringUtils.isBlank(input.getVersionId())) {
            input.setVersionId(IdGenerator.versionId());
        }
        input.setVersionStatus(1);
        if (StringUtils.isBlank(input.getVersionType())) {
            input.setVersionType("在研");
        }
        if (StringUtils.isBlank(input.getOwnerList())) {
            input.setOwnerList("system");
        }
        LocalDateTime now = LocalDateTime.now();
        ProductVersion v =
                domainService.createNew(
                        new ProductVersionDomainService.CreateCommand(
                                productId,
                                input.getVersionId(),
                                input.getVersionName(),
                                input.getVersionType(),
                                input.getVersionDescription(),
                                input.getBaselineVersionId(),
                                input.getBaselineVersionName(),
                                input.getVersionDesc(),
                                input.getApprover(),
                                input.getIsHidden(),
                                input.getSupportedVersion(),
                                input.getIntroducedProductId(),
                                input.getOwnerList(),
                                input.getVersionStatus(),
                                input.getCreatorId(),
                                input.getUpdaterId(),
                                now));
        productVersionRepository.insert(v);
        EntityVersionInfoPo out = ProductVersionAssembler.toPo(v);
        String who =
                StringUtils.defaultIfBlank(
                        StringUtils.firstNonBlank(input.getCreatorId(), input.getUpdaterId(), v.getCreatorId()),
                        "system");
        operationLogAppService.logVersionCreate(productId, out, who);
        return out;
    }

    /**
     * 从基线版本分支出新版本。
     *
     * @param productId 产品 ID
     * @param req       分支请求
     * @return 新建的版本数据
     */
    @Transactional
    public EntityVersionInfoPo branch(String productId, VersionBranchRequest req) {
        LocalDateTime now = LocalDateTime.now();
        ProductVersion neo =
                domainService.branchFromBaseline(
                        new ProductVersionDomainService.BranchFromBaselineCommand(
                                productId,
                                req.getBaseVersionId(),
                                req.getNewVersionId(),
                                req.getNewVersionName(),
                                req.getVersionNumber(),
                                req.getVersionDescription(),
                                now));
        productVersionRepository.insert(neo);
        if (Boolean.TRUE.equals(req.getCopyParameters())) {
            // system_parameter 复制在 spec-03 落地
        }
        EntityVersionInfoPo br = ProductVersionAssembler.toPo(neo);
        String w2 = StringUtils.defaultIfBlank(neo.getCreatorId(), "system");
        operationLogAppService.logVersionCreate(productId, br, w2);
        return br;
    }

    /**
     * 更新版本信息。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @param input     请求体
     * @return 更新后的数据
     */
    @Transactional
    public EntityVersionInfoPo update(String productId, String versionId, EntityVersionInfoPo input) {
        ProductVersion before =
                productVersionRepository
                        .findById(versionId)
                        .filter(v -> v.belongsToProduct(productId))
                        .orElseThrow(() -> new DomainRuleException("版本不存在或不属于该产品"));
        ProductVersion existing =
                domainService.updateExisting(
                        new ProductVersionDomainService.UpdateCommand(
                                productId,
                                versionId,
                                input == null ? null : input.getVersionName(),
                                input == null ? null : input.getSupportedVersion(),
                                input == null ? null : input.getVersionDescription(),
                                input == null ? null : input.getVersionDesc(),
                                input == null ? null : input.getVersionStatus(),
                                input == null ? null : input.getUpdaterId(),
                                LocalDateTime.now()));
        productVersionRepository.update(existing);
        String opU = StringUtils.defaultIfBlank(input == null ? null : input.getUpdaterId(), "system");
        operationLogAppService.logVersionUpdate(before, existing, opU);
        return ProductVersionAssembler.toPo(existing);
    }

    /**
     * 禁用版本。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     */
    @Transactional
    public void disable(String productId, String versionId) {
        ProductVersion pre = productVersionRepository.findById(versionId).orElse(null);
        String name = pre == null ? versionId : StringUtils.defaultString(pre.getVersionName());
        ProductVersion existing = domainService.disable(productId, versionId, LocalDateTime.now());
        productVersionRepository.update(existing);
        String opD = StringUtils.defaultIfBlank(existing.getUpdaterId(), "system");
        operationLogAppService.logVersionDelete(productId, versionId, name, opD);
    }

    /**
     * 导入产品版本（Excel 第一张表）。
     *
     * @param productId 产品 ID
     * @param bytes     文件内容
     * @return 导入结果
     */
    public BatchImportResult importCsv(String productId, byte[] bytes) {
        operationLogAppService.beginImportBatch();
        try {
            ExcelHelper.ParsedSheet sheet = ExcelHelper.parseFirstSheet(bytes);
            List<List<String>> rows = sheet.rows();
            if (rows.size() <= 1) {
                BatchImportResult empty = new BatchImportResult();
                empty.setTotalRows(0);
                empty.setSuccessCount(0);
                empty.setFailureCount(0);
                empty.setSuccessRowNumbers(List.of());
                empty.setFailures(List.of());
                return empty;
            }
            int headerIdx = ExcelHelper.detectHeaderRowIndex(rows);
            if (rows.size() <= headerIdx) {
                throw new BizException("表头缺失");
            }
            Map<String, Integer> idx = ExcelHelper.headerIndex(rows.get(headerIdx));
            ImportResultCollector c = new ImportResultCollector();
            int dataRows = rows.size() - headerIdx - 1;
            for (int i = headerIdx + 1; i < rows.size(); i++) {
                int line = i + 1;
                try {
                    List<String> cols = rows.get(i);
                    EntityVersionInfoPo row = new EntityVersionInfoPo();
                    String id = col(cols, idx, "版本ID");
                    row.setVersionId(StringUtils.trimToNull(id));
                    row.setVersionName(col(cols, idx, "版本名称"));
                    row.setVersionType(StringUtils.trimToNull(col(cols, idx, "版本类型")));
                    row.setSupportedVersion(StringUtils.trimToNull(col(cols, idx, "支持版本")));
                    row.setVersionDescription(StringUtils.trimToNull(col(cols, idx, "版本说明")));
                    row.setVersionDesc(StringUtils.trimToNull(col(cols, idx, "版本描述")));
                    row.setOwnerList(StringUtils.trimToNull(col(cols, idx, "责任人")));
                    row.setVersionStatus(parseIntDefault(col(cols, idx, "状态(1启用0未启用)"), 1));
                    if (StringUtils.isBlank(row.getVersionId())) {
                        create(productId, row);
                    } else {
                        productVersionRepository
                                .findById(row.getVersionId())
                                .orElseThrow(() -> new DomainRuleException("版本不存在"));
                        domainService.requireOwned(productId, row.getVersionId());
                        row.setUpdaterId("system");
                        update(productId, row.getVersionId(), row);
                    }
                    c.success(line);
                } catch (Exception ex) {
                    c.failure(line, ex.getMessage() == null ? "处理失败" : ex.getMessage());
                }
            }
            return c.build(dataRows);
        } finally {
            operationLogAppService.endImportBatch();
        }
    }

    /**
     * 导出产品版本（XLSX）。
     *
     * @param productId 产品 ID
     * @param page      页码（从 1 开始）
     * @param size      页大小
     * @return XLSX 字节
     */
    public byte[] exportCsvBody(String productId, int page, int size) {
        PageResponse<EntityVersionInfoPo> data =
                page(productId, ListPageWithKeywordQuery.of(page, size, null));
        List<List<String>> rows =
                data.getRecords().stream()
                        .map(
                                r ->
                                        List.of(
                                                StringUtils.defaultString(r.getVersionId()),
                                                StringUtils.defaultString(r.getVersionName()),
                                                StringUtils.defaultString(r.getVersionType()),
                                                StringUtils.defaultString(r.getSupportedVersion()),
                                                StringUtils.defaultString(r.getVersionDescription()),
                                                StringUtils.defaultString(r.getVersionDesc()),
                                                StringUtils.defaultString(r.getOwnerList())))
                        .collect(Collectors.toList());
        return ExcelHelper.buildWorkbook("产品版本", INSTRUCTION, HEADERS_CN, rows);
    }

    /**
     * 获取产品版本导入模板（XLSX）。
     *
     * @return 模板 XLSX 字节
     */
    public byte[] templateCsv() {
        return ExcelHelper.buildTemplate("产品版本", INSTRUCTION, HEADERS_CN);
    }

    private static int parseIntDefault(String s, int def) {
        if (StringUtils.isBlank(s)) {
            return def;
        }
        return Integer.parseInt(s.trim());
    }

    private static String col(List<String> cols, Map<String, Integer> idx, String headerCn) {
        Integer i = idx.get(headerCn);
        if (i == null || i < 0 || i >= cols.size()) {
            return "";
        }
        return cols.get(i) == null ? "" : cols.get(i).trim();
    }
}
