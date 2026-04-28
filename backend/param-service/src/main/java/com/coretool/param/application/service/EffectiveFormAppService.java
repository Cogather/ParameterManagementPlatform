package com.coretool.param.application.service;

import com.coretool.param.application.support.ImportResultCollector;
import com.coretool.param.domain.config.effectiveform.EffectiveForm;
import com.coretool.param.domain.config.effectiveform.service.EffectiveFormDomainService;
import com.coretool.param.domain.config.effectiveform.repository.EffectiveFormRepository;
import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.domain.support.IdGenerator;
import com.coretool.param.infrastructure.persistence.assembly.EffectiveFormAssembler;
import com.coretool.param.infrastructure.persistence.entity.EntityEffectiveFormDictPo;
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
public class EffectiveFormAppService {

    private static final String INSTRUCTION = ExcelInstructions.ID_CREATE_UPDATE_HINT;
    /**
     * 导入/导出字段以页面展示为准：不包含状态、审计字段。
     *
     * <p>兼容：导入时若上传文件额外包含“状态”列，也会被解析；若缺失则默认启用。
     */
    private static final List<String> HEADERS_CN = List.of("ID", "生效形态（中文）", "生效形态（英文）", "生效形态描述");

    @Resource
    private EffectiveFormRepository effectiveFormRepository;

    @Resource
    private OperationLogAppService operationLogAppService;

    private EffectiveFormDomainService domainService;

    /**
     * 分页查询生效形态字典（产品维度）。
     *
     * @param productId 产品 ID
     * @param q 分页与名称关键字（页码、页大小、关键字见 ListPageWithKeywordQuery）
     * @return 分页结果
     */
    public PageResponse<EntityEffectiveFormDictPo> page(
            String productId, ListPageWithKeywordQuery q) {
        var slice =
                effectiveFormRepository.pageByProduct(
                        productId, q.getPage(), q.getSize(), q.getKeyword());
        PageResponse<EntityEffectiveFormDictPo> resp = new PageResponse<>();
        resp.setRecords(
                slice.getRecords().stream()
                        .map(EffectiveFormAssembler::toPo)
                        .collect(Collectors.toList()));
        resp.setTotal(slice.getTotal());
        resp.setPage(q.getPage());
        resp.setSize(q.getSize());
        return resp;
    }

    /**
     * 新增生效形态字典项（若存在同名禁用项则复用并启用）。
     *
     * @param productId 产品 ID
     * @param input     请求体
     * @return 新增后的数据
     */
    @Transactional
    public EntityEffectiveFormDictPo create(String productId, EntityEffectiveFormDictPo input) {
        if (input == null) {
            throw new DomainRuleException("请求体不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        String nameCn = input.getEffectiveFormNameCn() == null ? "" : input.getEffectiveFormNameCn().trim();
        EffectiveForm disabled = effectiveFormRepository.findDisabledByNameCnInProduct(productId, nameCn).orElse(null);
        if (disabled != null) {
            EffectiveForm before =
                    EffectiveForm.rehydrate(
                            new EffectiveForm.Snapshot(
                                    disabled.getOwnedProductId(),
                                    disabled.getEffectiveFormId(),
                                    disabled.getEffectiveFormNameCn(),
                                    disabled.getEffectiveFormNameEn(),
                                    disabled.getEffectiveFormDescription(),
                                    disabled.getEffectiveFormStatus(),
                                    disabled.getCreatorId(),
                                    disabled.getCreationTimestamp(),
                                    disabled.getUpdaterId(),
                                    disabled.getUpdateTimestamp()));
            disabled.applyPatch(
                    new EffectiveForm.Patch(
                            nameCn,
                            input.getEffectiveFormNameEn(),
                            input.getEffectiveFormDescription(),
                            1,
                            StringUtils.defaultIfBlank(input.getUpdaterId(), "system"),
                            now));
            effectiveFormRepository.update(disabled);
            String opR = StringUtils.defaultIfBlank(input.getUpdaterId(), "system");
            operationLogAppService.logEffectiveFormUpdate(before, disabled, opR);
            return EffectiveFormAssembler.toPo(disabled);
        }

        if (StringUtils.isBlank(input.getEffectiveFormId())) {
            input.setEffectiveFormId(IdGenerator.effectiveFormId());
        }
        input.setEffectiveFormStatus(1);
        EffectiveForm f =
                ensureDomain()
                        .createNew(
                                new EffectiveFormDomainService.CreateCommand(
                                        productId,
                                        input.getEffectiveFormId(),
                                        input.getEffectiveFormNameCn(),
                                        input.getEffectiveFormNameEn(),
                                        input.getEffectiveFormDescription(),
                                        input.getEffectiveFormStatus(),
                                        input.getCreatorId(),
                                        input.getUpdaterId(),
                                        now));
        effectiveFormRepository.insert(f);
        EntityEffectiveFormDictPo out = EffectiveFormAssembler.toPo(f);
        String w =
                StringUtils.defaultIfBlank(
                        StringUtils.firstNonBlank(
                                input.getCreatorId(), input.getUpdaterId(), f.getCreatorId()),
                        "system");
        operationLogAppService.logEffectiveFormCreate(productId, out, w);
        return out;
    }

    /**
     * 更新生效形态字典项。
     *
     * @param productId        产品 ID
     * @param effectiveFormId  生效形态 ID
     * @param input            请求体
     * @return 更新后的数据
     */
    @Transactional
    public EntityEffectiveFormDictPo update(
            String productId, String effectiveFormId, EntityEffectiveFormDictPo input) {
        LocalDateTime now = LocalDateTime.now();
        EffectiveForm before =
                effectiveFormRepository
                        .findById(effectiveFormId)
                        .filter(x -> x.belongsToProduct(productId))
                        .orElseThrow(() -> new DomainRuleException("生效形态不存在或不属于该产品"));
        EffectiveForm existing =
                ensureDomain()
                        .updateExisting(
                                new EffectiveFormDomainService.UpdateCommand(
                                        productId,
                                        effectiveFormId,
                                        input == null ? null : input.getEffectiveFormNameCn(),
                                        input == null ? null : input.getEffectiveFormNameEn(),
                                        input == null ? null : input.getEffectiveFormDescription(),
                                        input == null ? null : input.getEffectiveFormStatus(),
                                        input == null ? null : input.getUpdaterId(),
                                        now));
        effectiveFormRepository.update(existing);
        String opU = StringUtils.defaultIfBlank(input == null ? null : input.getUpdaterId(), "system");
        operationLogAppService.logEffectiveFormUpdate(before, existing, opU);
        return EffectiveFormAssembler.toPo(existing);
    }

    /**
     * 禁用生效形态字典项。
     *
     * @param productId        产品 ID
     * @param effectiveFormId  生效形态 ID
     */
    @Transactional
    public void disable(String productId, String effectiveFormId) {
        EffectiveForm pre = effectiveFormRepository.findById(effectiveFormId).orElse(null);
        String display =
                pre == null
                        ? effectiveFormId
                        : StringUtils.defaultIfBlank(pre.getEffectiveFormNameCn(), effectiveFormId);
        EffectiveForm e = ensureDomain().disable(productId, effectiveFormId, LocalDateTime.now());
        effectiveFormRepository.update(e);
        String opD = StringUtils.defaultIfBlank(e.getUpdaterId(), "system");
        operationLogAppService.logDictRowDelete(
                new OperationLogAppService.LogDictRowDeleteInput(
                        OperationLogAppService.BIZ_TABLE_ENTITY_EFFECTIVE_FORM_DICT,
                        productId,
                        null,
                        effectiveFormId,
                        display,
                        opD));
    }

    /**
     * 导入生效形态字典（Excel 第一张表）。
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
            Map<String, Integer> idx = ExcelHelper.headerIndex(rows.get(headerIdx));
            ImportResultCollector c = new ImportResultCollector();
            int dataRows = rows.size() - headerIdx - 1;
            for (int i = headerIdx + 1; i < rows.size(); i++) {
                int line = i + 1;
                try {
                    List<String> cols = rows.get(i);
                    EntityEffectiveFormDictPo po = new EntityEffectiveFormDictPo();
                    po.setEffectiveFormId(StringUtils.trimToNull(col(cols, idx, "ID")));
                    po.setEffectiveFormNameCn(col(cols, idx, "生效形态（中文）"));
                    po.setEffectiveFormNameEn(col(cols, idx, "生效形态（英文）"));
                    po.setEffectiveFormDescription(StringUtils.trimToNull(col(cols, idx, "生效形态描述")));
                    po.setEffectiveFormStatus(parseIntDefault(col(cols, idx, "状态(1启用0未启用)"), 1));
                    if (StringUtils.isBlank(po.getEffectiveFormId())) {
                        create(productId, po);
                    } else {
                        EffectiveForm ex = effectiveFormRepository.findById(po.getEffectiveFormId()).orElse(null);
                        if (ex == null) {
                            create(productId, po);
                            c.success(line);
                            continue;
                        }
                        ensureDomain().requireOwned(productId, po.getEffectiveFormId());
                        po.setUpdaterId("system");
                        update(productId, po.getEffectiveFormId(), po);
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
     * 导出生效形态字典（XLSX）。
     *
     * @param productId 产品 ID
     * @param page      页码（从 1 开始）
     * @param size      页大小
     * @return XLSX 字节
     */
    public byte[] exportCsv(String productId, int page, int size) {
        PageResponse<EntityEffectiveFormDictPo> data =
                page(productId, ListPageWithKeywordQuery.of(page, size, null));
        List<List<String>> rows =
                data.getRecords().stream()
                        .map(
                                r ->
                                        List.of(
                                                StringUtils.defaultString(r.getEffectiveFormId()),
                                                StringUtils.defaultString(r.getEffectiveFormNameCn()),
                                                StringUtils.defaultString(r.getEffectiveFormNameEn()),
                                                StringUtils.defaultString(r.getEffectiveFormDescription())))
                        .collect(Collectors.toList());
        return ExcelHelper.buildWorkbook("生效形态", INSTRUCTION, HEADERS_CN, rows);
    }

    /**
     * 获取生效形态字典导入模板（XLSX）。
     *
     * @return 模板 XLSX 字节
     */
    public byte[] templateCsv() {
        return ExcelHelper.buildTemplate("生效形态", INSTRUCTION, HEADERS_CN);
    }

    private EffectiveFormDomainService ensureDomain() {
        if (domainService == null) {
            domainService = new EffectiveFormDomainService(effectiveFormRepository);
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
