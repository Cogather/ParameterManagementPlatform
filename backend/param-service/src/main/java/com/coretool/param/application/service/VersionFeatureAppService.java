package com.coretool.param.application.service;

import com.coretool.param.application.support.ImportResultCollector;
import com.coretool.param.domain.config.version.ProductVersion;
import com.coretool.param.domain.config.version.repository.ProductVersionRepository;
import com.coretool.param.domain.support.IdGenerator;
import com.coretool.param.domain.config.versionfeature.VersionFeature;
import com.coretool.param.domain.config.versionfeature.repository.VersionFeatureRepository;
import com.coretool.param.domain.config.versionfeature.service.VersionFeatureDomainService;
import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.infrastructure.persistence.assembly.VersionFeatureAssembler;
import com.coretool.param.infrastructure.persistence.entity.VersionFeatureDictPo;
import com.coretool.param.infrastructure.util.ExcelHelper;
import com.coretool.param.infrastructure.util.ExcelInstructions;
import com.coretool.param.ui.response.BatchImportResult;
import com.coretool.param.ui.response.PageResponse;
import com.coretool.param.ui.vo.ListPageWithKeywordQuery;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VersionFeatureAppService {

    private static final String INSTRUCTION = ExcelInstructions.ID_CREATE_UPDATE_HINT;
    /**
     * 导入/导出字段以页面展示为准：不包含状态、审计字段；特性编码由后端生成，不出现在 Excel。
     *
     * <p>兼容：导入时若上传文件额外包含“状态”列，也会被解析；若缺失则默认启用。
     */
    private static final List<String> HEADERS_CN =
            List.of("ID", "中文名称", "英文名称", "引入类型", "继承/引用版本 ID");

    @Resource
    private VersionFeatureRepository featureRepository;

    @Resource
    private ProductVersionRepository productVersionRepository;

    @Resource
    private OperationLogAppService operationLogAppService;

    private VersionFeatureDomainService domainService;

    /**
     * 分页查询版本特性（产品+版本维度）。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @param q 分页与名称关键字（页码、页大小、关键字见 ListPageWithKeywordQuery）
     * @return 分页结果
     */
    public PageResponse<VersionFeatureDictPo> page(
            String productId, String versionId, ListPageWithKeywordQuery q) {
        requireVersionOwned(productId, versionId);
        var slice =
                featureRepository.pageByProductAndVersion(
                        productId, versionId, q.getPage(), q.getSize(), q.getKeyword());
        PageResponse<VersionFeatureDictPo> resp = new PageResponse<>();
        resp.setRecords(
                slice.getRecords().stream()
                        .map(VersionFeatureAssembler::toPo)
                        .collect(Collectors.toList()));
        resp.setTotal(slice.getTotal());
        resp.setPage(q.getPage());
        resp.setSize(q.getSize());
        return resp;
    }

    /**
     * 新增版本特性（若存在同名禁用项则复用并启用）。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @param input     请求体
     * @return 新增后的数据
     */
    @Transactional
    public VersionFeatureDictPo create(String productId, String versionId, VersionFeatureDictPo input) {
        requireVersionOwned(productId, versionId);
        if (input == null) {
            throw new DomainRuleException("请求体不能为空");
        }
        String nameCn = input.getFeatureNameCn() == null ? "" : input.getFeatureNameCn().trim();
        VersionFeature disabled = featureRepository.findDisabledByNameCnInScope(productId, versionId, nameCn).orElse(null);
        if (disabled != null) {
            VersionFeature before =
                    VersionFeature.rehydrate(
                            new VersionFeature.Snapshot(
                                    disabled.getOwnedProductPbiId(),
                                    disabled.getOwnedVersionId(),
                                    disabled.getFeatureId(),
                                    disabled.getFeatureCode(),
                                    disabled.getFeatureNameCn(),
                                    disabled.getFeatureNameEn(),
                                    disabled.getIntroduceType(),
                                    disabled.getInheritReferenceVersionId(),
                                    disabled.getFeatureStatus(),
                                    disabled.getCreatorId(),
                                    disabled.getCreationTimestamp(),
                                    disabled.getUpdaterId(),
                                    disabled.getUpdateTimestamp()));
            disabled.applyPatch(
                    new VersionFeature.Patch(
                            null,
                            nameCn,
                            input.getFeatureNameEn(),
                            input.getIntroduceType(),
                            input.getInheritReferenceVersionId(),
                            1,
                            StringUtils.defaultIfBlank(input.getUpdaterId(), "system"),
                            LocalDateTime.now()));
            featureRepository.update(disabled);
            String opR = StringUtils.defaultIfBlank(input.getUpdaterId(), "system");
            operationLogAppService.logVersionFeatureUpdate(before, disabled, opR);
            return VersionFeatureAssembler.toPo(disabled);
        }
        if (StringUtils.isBlank(input.getFeatureId())) {
            input.setFeatureId(IdGenerator.featureId());
        }
        if (StringUtils.isBlank(input.getFeatureCode())) {
            input.setFeatureCode(IdGenerator.featureCode());
        }
        input.setFeatureStatus(1);
        LocalDateTime now = LocalDateTime.now();
        VersionFeature f =
                ensureDomain()
                        .createNew(
                                new VersionFeatureDomainService.CreateCommand(
                                        productId,
                                        versionId,
                                        input.getFeatureId(),
                                        input.getFeatureCode(),
                                        input.getFeatureNameCn(),
                                        input.getFeatureNameEn(),
                                        input.getIntroduceType(),
                                        input.getInheritReferenceVersionId(),
                                        input.getFeatureStatus(),
                                        input.getCreatorId(),
                                        input.getUpdaterId(),
                                        now));
        featureRepository.insert(f);
        VersionFeatureDictPo out = VersionFeatureAssembler.toPo(f);
        String w =
                StringUtils.defaultIfBlank(
                        StringUtils.firstNonBlank(
                                input.getCreatorId(), input.getUpdaterId(), f.getCreatorId()),
                        "system");
        operationLogAppService.logVersionFeatureCreate(productId, versionId, out, w);
        return out;
    }

    /**
     * 更新版本特性。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @param featureId 特性 ID
     * @param input     请求体
     * @return 更新后的数据
     */
    @Transactional
    public VersionFeatureDictPo update(
            String productId, String versionId, String featureId, VersionFeatureDictPo input) {
        LocalDateTime now = LocalDateTime.now();
        VersionFeature before =
                featureRepository
                        .findByFeatureId(featureId)
                        .filter(x -> x.belongsToProductAndVersion(productId, versionId))
                        .orElseThrow(() -> new DomainRuleException("版本特性不存在或不属于该版本"));
        VersionFeature existing =
                ensureDomain()
                        .updateExisting(
                                new VersionFeatureDomainService.UpdateCommand(
                                        productId,
                                        versionId,
                                        featureId,
                                        input == null ? null : input.getFeatureNameCn(),
                                        input == null ? null : input.getFeatureNameEn(),
                                        input == null ? null : input.getIntroduceType(),
                                        input == null ? null : input.getInheritReferenceVersionId(),
                                        input == null ? null : input.getFeatureStatus(),
                                        input == null ? null : input.getUpdaterId(),
                                        now));
        featureRepository.update(existing);
        String opU = StringUtils.defaultIfBlank(input == null ? null : input.getUpdaterId(), "system");
        operationLogAppService.logVersionFeatureUpdate(before, existing, opU);
        return VersionFeatureAssembler.toPo(existing);
    }

    /**
     * 禁用版本特性。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @param featureId 特性 ID
     */
    @Transactional
    public void disable(String productId, String versionId, String featureId) {
        VersionFeature pre = featureRepository.findByFeatureId(featureId).orElse(null);
        String display =
                pre == null
                        ? featureId
                        : StringUtils.defaultIfBlank(pre.getFeatureNameCn(), featureId);
        VersionFeature e = ensureDomain().disable(productId, versionId, featureId, LocalDateTime.now());
        featureRepository.update(e);
        String opD = StringUtils.defaultIfBlank(e.getUpdaterId(), "system");
        operationLogAppService.logDictRowDelete(
                new OperationLogAppService.LogDictRowDeleteInput(
                        OperationLogAppService.BIZ_TABLE_VERSION_FEATURE_DICT,
                        productId,
                        versionId,
                        featureId,
                        display,
                        opD));
    }

    /**
     * 导入版本特性（Excel 第一张表）。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @param bytes     文件内容
     * @return 导入结果
     */
    public BatchImportResult importCsv(String productId, String versionId, byte[] bytes) {
        requireVersionOwned(productId, versionId);
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
            Map<String, Integer> idx = ExcelHelper.headerIndex(rows.get(headerIdx));
            ImportResultCollector c = new ImportResultCollector();
            int dataRows = rows.size() - headerIdx - 1;
            for (int i = headerIdx + 1; i < rows.size(); i++) {
                int line = i + 1;
                try {
                    List<String> cols = rows.get(i);
                    VersionFeatureDictPo po = new VersionFeatureDictPo();
                    po.setFeatureId(StringUtils.trimToNull(col(cols, idx, "ID")));
                    po.setFeatureCode(col(cols, idx, "特性 ID"));
                    po.setFeatureNameCn(col(cols, idx, "中文名称"));
                    po.setFeatureNameEn(col(cols, idx, "英文名称"));
                    po.setIntroduceType(col(cols, idx, "引入类型"));
                    po.setInheritReferenceVersionId(StringUtils.trimToNull(col(cols, idx, "继承/引用版本 ID")));
                    po.setFeatureStatus(parseIntDefault(col(cols, idx, "状态(1启用0未启用)"), 1));
                    if (StringUtils.isBlank(po.getFeatureId())) {
                        create(productId, versionId, po);
                    } else {
                        VersionFeature ex = featureRepository.findByFeatureId(po.getFeatureId()).orElse(null);
                        if (ex == null) {
                            create(productId, versionId, po);
                            c.success(line);
                            continue;
                        }
                        ensureDomain().requireOwned(productId, versionId, po.getFeatureId());
                        po.setUpdaterId("system");
                        update(productId, versionId, po.getFeatureId(), po);
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
     * 导出版本特性（XLSX）。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @param page      页码（从 1 开始）
     * @param size      页大小
     * @return XLSX 字节
     */
    public byte[] exportCsv(String productId, String versionId, int page, int size) {
        PageResponse<VersionFeatureDictPo> data =
                page(productId, versionId, ListPageWithKeywordQuery.of(page, size, null));
        List<List<String>> rows =
                data.getRecords().stream()
                        .map(
                                r ->
                                        List.of(
                                                StringUtils.defaultString(r.getFeatureId()),
                                                StringUtils.defaultString(r.getFeatureNameCn()),
                                                StringUtils.defaultString(r.getFeatureNameEn()),
                                                StringUtils.defaultString(r.getIntroduceType()),
                                                StringUtils.defaultString(r.getInheritReferenceVersionId())))
                        .collect(Collectors.toList());
        return ExcelHelper.buildWorkbook("版本特性", INSTRUCTION, HEADERS_CN, rows);
    }

    /**
     * 获取版本特性导入模板（XLSX）。
     *
     * @return 模板 XLSX 字节
     */
    public byte[] templateCsv() {
        return ExcelHelper.buildTemplate("版本特性", INSTRUCTION, HEADERS_CN);
    }

    /**
     * 校验指定版本是否存在且归属指定产品。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     */
    private void requireVersionOwned(String productId, String versionId) {
        ProductVersion v =
                productVersionRepository
                        .findById(versionId)
                        .orElseThrow(() -> new DomainRuleException("版本不存在或不属于该产品"));
        if (!v.belongsToProduct(productId)) {
            throw new DomainRuleException("版本不存在或不属于该产品");
        }
    }

    private VersionFeatureDomainService ensureDomain() {
        if (domainService == null) {
            domainService = new VersionFeatureDomainService(featureRepository);
        }
        return domainService;
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
