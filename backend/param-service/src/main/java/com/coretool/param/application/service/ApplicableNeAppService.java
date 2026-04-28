package com.coretool.param.application.service;

import com.coretool.param.application.support.ImportResultCollector;
import com.coretool.param.domain.config.ne.ApplicableNe;
import com.coretool.param.domain.config.ne.service.ApplicableNeDomainService;
import com.coretool.param.domain.config.ne.repository.ApplicableNeRepository;
import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.domain.support.IdGenerator;
import com.coretool.param.infrastructure.persistence.assembly.ApplicableNeAssembler;
import com.coretool.param.infrastructure.persistence.entity.EntityApplicableNeDictPo;
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
public class ApplicableNeAppService {

    private static final String INSTRUCTION = ExcelInstructions.ID_CREATE_UPDATE_HINT;
    /**
     * 导入/导出字段以页面展示为准：不包含状态、审计字段。
     *
     * <p>兼容：导入时若上传文件额外包含“状态”列，也会被解析；若缺失则默认启用。
     */
    private static final List<String> HEADERS_CN = List.of("ID", "适用网元名称", "网元类型描述", "产品形态");

    @Resource
    private ApplicableNeRepository applicableNeRepository;

    @Resource
    private OperationLogAppService operationLogAppService;

    private ApplicableNeDomainService domainService;

    /**
     * 分页查询适用网元字典（产品维度）。
     *
     * @param productId 产品 ID
     * @param q 分页与名称关键字（页码、页大小、关键字见 ListPageWithKeywordQuery）
     * @return 分页结果
     */
    public PageResponse<EntityApplicableNeDictPo> page(
            String productId, ListPageWithKeywordQuery q) {
        var slice =
                applicableNeRepository.pageByProduct(
                        productId, q.getPage(), q.getSize(), q.getKeyword());
        PageResponse<EntityApplicableNeDictPo> resp = new PageResponse<>();
        resp.setRecords(
                slice.getRecords().stream()
                        .map(ApplicableNeAssembler::toPo)
                        .collect(Collectors.toList()));
        resp.setTotal(slice.getTotal());
        resp.setPage(q.getPage());
        resp.setSize(q.getSize());
        return resp;
    }

    /**
     * 新增适用网元字典项（若存在同名禁用项则复用并启用）。
     *
     * @param productId 产品 ID
     * @param input     请求体
     * @return 新增后的数据
     */
    @Transactional
    public EntityApplicableNeDictPo create(String productId, EntityApplicableNeDictPo input) {
        if (input == null) {
            throw new DomainRuleException("请求体不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        String name = input.getNeTypeNameCn() == null ? "" : input.getNeTypeNameCn().trim();
        ApplicableNe disabled = applicableNeRepository.findDisabledByNameInProduct(productId, name).orElse(null);
        if (disabled != null) {
            ApplicableNe before =
                    ApplicableNe.rehydrate(
                            new ApplicableNe.Snapshot(
                                    disabled.getOwnedProductId(),
                                    disabled.getNeTypeId(),
                                    disabled.getNeTypeNameCn(),
                                    disabled.getNeTypeDescription(),
                                    disabled.getNeTypeStatus(),
                                    disabled.getProductForm(),
                                    disabled.getCreatorId(),
                                    disabled.getCreationTimestamp(),
                                    disabled.getUpdaterId(),
                                    disabled.getUpdateTimestamp()));
            disabled.applyPatch(
                    new ApplicableNe.Patch(
                            name,
                            input.getNeTypeDescription(),
                            1,
                            input.getProductForm(),
                            StringUtils.defaultIfBlank(input.getUpdaterId(), "system"),
                            now));
            applicableNeRepository.update(disabled);
            String opR = StringUtils.defaultIfBlank(input.getUpdaterId(), "system");
            operationLogAppService.logApplicableNeUpdate(before, disabled, opR);
            return ApplicableNeAssembler.toPo(disabled);
        }

        if (StringUtils.isBlank(input.getNeTypeId())) {
            input.setNeTypeId(IdGenerator.neTypeId());
        }
        input.setNeTypeStatus(1);
        ApplicableNe ne =
                ensureDomain()
                        .createNew(
                                new ApplicableNeDomainService.CreateCommand(
                                        productId,
                                        input.getNeTypeId(),
                                        input.getNeTypeNameCn(),
                                        input.getNeTypeDescription(),
                                        input.getNeTypeStatus(),
                                        input.getProductForm(),
                                        input.getCreatorId(),
                                        input.getUpdaterId(),
                                        now));
        applicableNeRepository.insert(ne);
        EntityApplicableNeDictPo out = ApplicableNeAssembler.toPo(ne);
        String w =
                StringUtils.defaultIfBlank(
                        StringUtils.firstNonBlank(
                                input.getCreatorId(), input.getUpdaterId(), ne.getCreatorId()),
                        "system");
        operationLogAppService.logApplicableNeCreate(productId, out, w);
        return out;
    }

    /**
     * 更新适用网元字典项。
     *
     * @param productId 产品 ID
     * @param neTypeId  网元类型 ID
     * @param input     请求体
     * @return 更新后的数据
     */
    @Transactional
    public EntityApplicableNeDictPo update(String productId, String neTypeId, EntityApplicableNeDictPo input) {
        LocalDateTime now = LocalDateTime.now();
        ApplicableNe before =
                applicableNeRepository
                        .findByNeTypeId(neTypeId)
                        .filter(n -> n.belongsToProduct(productId))
                        .orElseThrow(() -> new DomainRuleException("适用网元不存在或不属于该产品"));
        ApplicableNe existing = ensureDomain().requireOwned(productId, neTypeId);
        String nameCn = input == null ? null : input.getNeTypeNameCn();
        if (StringUtils.isNotBlank(nameCn) && !StringUtils.equals(nameCn, existing.getNeTypeNameCn())) {
            if (applicableNeRepository.existsSameNameInProduct(productId, nameCn, neTypeId)) {
                throw new DomainRuleException("NE_NAME_DUPLICATE: 同一产品下网元名称已存在");
            }
        }
        if (StringUtils.isNotBlank(nameCn)
                || (input != null && input.getNeTypeDescription() != null)
                || (input != null && input.getNeTypeStatus() != null)
                || (input != null && input.getProductForm() != null)
                || StringUtils.isNotBlank(input == null ? null : input.getUpdaterId())) {
            existing.applyPatch(
                    new ApplicableNe.Patch(
                            nameCn,
                            input == null ? null : input.getNeTypeDescription(),
                            input == null ? null : input.getNeTypeStatus(),
                            input == null ? null : input.getProductForm(),
                            input == null ? null : input.getUpdaterId(),
                            now));
        } else {
            existing.touch(now);
        }
        applicableNeRepository.update(existing);
        String opU = StringUtils.defaultIfBlank(input == null ? null : input.getUpdaterId(), "system");
        operationLogAppService.logApplicableNeUpdate(before, existing, opU);
        return ApplicableNeAssembler.toPo(existing);
    }

    /**
     * 禁用适用网元字典项。
     *
     * @param productId 产品 ID
     * @param neTypeId  网元类型 ID
     */
    @Transactional
    public void disable(String productId, String neTypeId) {
        ApplicableNe pre = applicableNeRepository.findByNeTypeId(neTypeId).orElse(null);
        String display = pre == null ? neTypeId : StringUtils.defaultIfBlank(pre.getNeTypeNameCn(), neTypeId);
        ApplicableNe existing = ensureDomain().disable(productId, neTypeId, LocalDateTime.now());
        applicableNeRepository.update(existing);
        String opD = StringUtils.defaultIfBlank(existing.getUpdaterId(), "system");
        operationLogAppService.logDictRowDelete(
                new OperationLogAppService.LogDictRowDeleteInput(
                        OperationLogAppService.BIZ_TABLE_ENTITY_APPLICABLE_NE_DICT,
                        productId,
                        null,
                        neTypeId,
                        display,
                        opD));
    }

    /**
     * 导入适用网元字典（Excel 第一张表）。
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
                return emptyResult();
            }
            int headerIdx = ExcelHelper.detectHeaderRowIndex(rows);
            Map<String, Integer> idx = ExcelHelper.headerIndex(rows.get(headerIdx));
            ImportResultCollector c = new ImportResultCollector();
            int dataRows = rows.size() - headerIdx - 1;
            for (int i = headerIdx + 1; i < rows.size(); i++) {
                int line = i + 1;
                try {
                    List<String> cols = rows.get(i);
                    EntityApplicableNeDictPo po = new EntityApplicableNeDictPo();
                    po.setNeTypeId(StringUtils.trimToNull(col(cols, idx, "ID")));
                    po.setNeTypeNameCn(col(cols, idx, "适用网元名称"));
                    po.setNeTypeDescription(StringUtils.trimToNull(col(cols, idx, "网元类型描述")));
                    po.setNeTypeStatus(parseIntDefault(col(cols, idx, "状态(1启用0未启用)"), 1));
                    po.setProductForm(StringUtils.trimToNull(col(cols, idx, "产品形态")));
                    if (StringUtils.isBlank(po.getNeTypeId())) {
                        create(productId, po);
                    } else {
                        ApplicableNe ex = applicableNeRepository.findByNeTypeId(po.getNeTypeId()).orElse(null);
                        if (ex == null) {
                            create(productId, po);
                            c.success(line);
                            continue;
                        }
                        ensureDomain().requireOwned(productId, po.getNeTypeId());
                        po.setUpdaterId("system");
                        update(productId, po.getNeTypeId(), po);
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
     * 导出适用网元字典（XLSX）。
     *
     * @param productId 产品 ID
     * @param page      页码（从 1 开始）
     * @param size      页大小
     * @return XLSX 字节
     */
    public byte[] exportCsv(String productId, int page, int size) {
        PageResponse<EntityApplicableNeDictPo> data =
                page(productId, ListPageWithKeywordQuery.of(page, size, null));
        List<List<String>> rows =
                data.getRecords().stream()
                        .map(
                                r ->
                                        List.of(
                                                StringUtils.defaultString(r.getNeTypeId()),
                                                StringUtils.defaultString(r.getNeTypeNameCn()),
                                                StringUtils.defaultString(r.getNeTypeDescription()),
                                                StringUtils.defaultString(r.getProductForm())))
                        .collect(Collectors.toList());
        return ExcelHelper.buildWorkbook("适用网元", INSTRUCTION, HEADERS_CN, rows);
    }

    /**
     * 获取适用网元字典导入模板（XLSX）。
     *
     * @return 模板 XLSX 字节
     */
    public byte[] templateCsv() {
        return ExcelHelper.buildTemplate("适用网元", INSTRUCTION, HEADERS_CN);
    }

    private ApplicableNeDomainService ensureDomain() {
        if (domainService == null) {
            domainService = new ApplicableNeDomainService(applicableNeRepository);
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

    private static BatchImportResult emptyResult() {
        BatchImportResult empty = new BatchImportResult();
        empty.setTotalRows(0);
        empty.setSuccessCount(0);
        empty.setFailureCount(0);
        empty.setSuccessRowNumbers(List.of());
        empty.setFailures(List.of());
        return empty;
    }
}
