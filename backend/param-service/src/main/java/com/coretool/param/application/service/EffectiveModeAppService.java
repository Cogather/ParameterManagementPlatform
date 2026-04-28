package com.coretool.param.application.service;

import com.coretool.param.application.support.ImportResultCollector;
import com.coretool.param.domain.config.effectivemode.EffectiveMode;
import com.coretool.param.domain.config.effectivemode.service.EffectiveModeDomainService;
import com.coretool.param.domain.config.effectivemode.repository.EffectiveModeRepository;
import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.domain.support.IdGenerator;
import com.coretool.param.infrastructure.persistence.assembly.EffectiveModeAssembler;
import com.coretool.param.infrastructure.persistence.entity.EntityEffectiveModeDictPo;
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
public class EffectiveModeAppService {

    private static final String INSTRUCTION = ExcelInstructions.ID_CREATE_UPDATE_HINT;
    /**
     * 导入/导出字段以页面展示为准：不包含状态、审计字段。
     *
     * <p>兼容：导入时若上传文件额外包含“状态”列，也会被解析；若缺失则默认启用。
     */
    private static final List<String> HEADERS_CN = List.of("ID", "生效方式（中文）", "生效方式（英文）", "生效方式描述");

    @Resource
    private EffectiveModeRepository effectiveModeRepository;

    @Resource
    private OperationLogAppService operationLogAppService;

    private EffectiveModeDomainService domainService;

    /**
     * 分页查询生效方式字典（产品维度）。
     *
     * @param productId 产品 ID
     * @param q 分页与名称关键字（页码、页大小、关键字见 ListPageWithKeywordQuery）
     * @return 分页结果
     */
    public PageResponse<EntityEffectiveModeDictPo> page(
            String productId, ListPageWithKeywordQuery q) {
        var slice =
                effectiveModeRepository.pageByProduct(
                        productId, q.getPage(), q.getSize(), q.getKeyword());
        PageResponse<EntityEffectiveModeDictPo> resp = new PageResponse<>();
        resp.setRecords(
                slice.getRecords().stream()
                        .map(EffectiveModeAssembler::toPo)
                        .collect(Collectors.toList()));
        resp.setTotal(slice.getTotal());
        resp.setPage(q.getPage());
        resp.setSize(q.getSize());
        return resp;
    }

    /**
     * 新增生效方式字典项（若存在同名禁用项则复用并启用）。
     *
     * @param productId 产品 ID
     * @param input     请求体
     * @return 新增后的数据
     */
    @Transactional
    public EntityEffectiveModeDictPo create(String productId, EntityEffectiveModeDictPo input) {
        if (input == null) {
            throw new DomainRuleException("请求体不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        String nameCn = input.getEffectiveModeNameCn() == null ? "" : input.getEffectiveModeNameCn().trim();
        EffectiveMode disabled = effectiveModeRepository.findDisabledByNameCnInProduct(productId, nameCn).orElse(null);
        if (disabled != null) {
            EffectiveMode before =
                    EffectiveMode.rehydrate(
                            new EffectiveMode.Snapshot(
                                    disabled.getOwnedProductId(),
                                    disabled.getEffectiveModeId(),
                                    disabled.getEffectiveModeNameCn(),
                                    disabled.getEffectiveModeNameEn(),
                                    disabled.getEffectiveModeDescription(),
                                    disabled.getEffectiveModeStatus(),
                                    disabled.getCreatorId(),
                                    disabled.getCreationTimestamp(),
                                    disabled.getUpdaterId(),
                                    disabled.getUpdateTimestamp()));
            disabled.applyPatch(
                    new EffectiveMode.Patch(
                            nameCn,
                            input.getEffectiveModeNameEn(),
                            input.getEffectiveModeDescription(),
                            1,
                            StringUtils.defaultIfBlank(input.getUpdaterId(), "system"),
                            now));
            effectiveModeRepository.update(disabled);
            String opR = StringUtils.defaultIfBlank(input.getUpdaterId(), "system");
            operationLogAppService.logEffectiveModeUpdate(before, disabled, opR);
            return EffectiveModeAssembler.toPo(disabled);
        }

        if (StringUtils.isBlank(input.getEffectiveModeId())) {
            input.setEffectiveModeId(IdGenerator.effectiveModeId());
        }
        input.setEffectiveModeStatus(1);
        EffectiveMode m =
                ensureDomain()
                        .createNew(
                                new EffectiveModeDomainService.CreateCommand(
                                        productId,
                                        input.getEffectiveModeId(),
                                        input.getEffectiveModeNameCn(),
                                        input.getEffectiveModeNameEn(),
                                        input.getEffectiveModeDescription(),
                                        input.getEffectiveModeStatus(),
                                        input.getCreatorId(),
                                        input.getUpdaterId(),
                                        now));
        effectiveModeRepository.insert(m);
        EntityEffectiveModeDictPo out = EffectiveModeAssembler.toPo(m);
        String w =
                StringUtils.defaultIfBlank(
                        StringUtils.firstNonBlank(
                                input.getCreatorId(), input.getUpdaterId(), m.getCreatorId()),
                        "system");
        operationLogAppService.logEffectiveModeCreate(productId, out, w);
        return out;
    }

    /**
     * 更新生效方式字典项。
     *
     * @param productId        产品 ID
     * @param effectiveModeId  生效方式 ID
     * @param input            请求体
     * @return 更新后的数据
     */
    @Transactional
    public EntityEffectiveModeDictPo update(
            String productId, String effectiveModeId, EntityEffectiveModeDictPo input) {
        LocalDateTime now = LocalDateTime.now();
        EffectiveMode before =
                effectiveModeRepository
                        .findById(effectiveModeId)
                        .filter(m -> m.belongsToProduct(productId))
                        .orElseThrow(() -> new DomainRuleException("生效方式不存在或不属于该产品"));
        EffectiveMode existing =
                ensureDomain()
                        .updateExisting(
                                new EffectiveModeDomainService.UpdateCommand(
                                        productId,
                                        effectiveModeId,
                                        input == null ? null : input.getEffectiveModeNameCn(),
                                        input == null ? null : input.getEffectiveModeNameEn(),
                                        input == null ? null : input.getEffectiveModeDescription(),
                                        input == null ? null : input.getEffectiveModeStatus(),
                                        input == null ? null : input.getUpdaterId(),
                                        now));
        effectiveModeRepository.update(existing);
        String opU = StringUtils.defaultIfBlank(input == null ? null : input.getUpdaterId(), "system");
        operationLogAppService.logEffectiveModeUpdate(before, existing, opU);
        return EffectiveModeAssembler.toPo(existing);
    }

    /**
     * 禁用生效方式字典项。
     *
     * @param productId        产品 ID
     * @param effectiveModeId  生效方式 ID
     */
    @Transactional
    public void disable(String productId, String effectiveModeId) {
        EffectiveMode pre = effectiveModeRepository.findById(effectiveModeId).orElse(null);
        String display =
                pre == null
                        ? effectiveModeId
                        : StringUtils.defaultIfBlank(pre.getEffectiveModeNameCn(), effectiveModeId);
        EffectiveMode e = ensureDomain().disable(productId, effectiveModeId, LocalDateTime.now());
        effectiveModeRepository.update(e);
        String opD = StringUtils.defaultIfBlank(e.getUpdaterId(), "system");
        operationLogAppService.logDictRowDelete(
                new OperationLogAppService.LogDictRowDeleteInput(
                        OperationLogAppService.BIZ_TABLE_ENTITY_EFFECTIVE_MODE_DICT,
                        productId,
                        null,
                        effectiveModeId,
                        display,
                        opD));
    }

    /**
     * 导出生效方式字典（Excel 第一张表）。
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
                    EntityEffectiveModeDictPo po = new EntityEffectiveModeDictPo();
                    po.setEffectiveModeId(StringUtils.trimToNull(col(cols, idx, "ID")));
                    po.setEffectiveModeNameCn(col(cols, idx, "生效方式（中文）"));
                    po.setEffectiveModeNameEn(col(cols, idx, "生效方式（英文）"));
                    po.setEffectiveModeDescription(StringUtils.trimToNull(col(cols, idx, "生效方式描述")));
                    po.setEffectiveModeStatus(parseIntDefault(col(cols, idx, "状态(1启用0未启用)"), 1));
                    if (StringUtils.isBlank(po.getEffectiveModeId())) {
                        create(productId, po);
                    } else {
                        EffectiveMode ex = effectiveModeRepository.findById(po.getEffectiveModeId()).orElse(null);
                        if (ex == null) {
                            create(productId, po);
                            c.success(line);
                            continue;
                        }
                        ensureDomain().requireOwned(productId, po.getEffectiveModeId());
                        po.setUpdaterId("system");
                        update(productId, po.getEffectiveModeId(), po);
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
     * 导出生效方式字典（XLSX）。
     *
     * @param productId 产品 ID
     * @param page      页码（从 1 开始）
     * @param size      页大小
     * @return XLSX 字节
     */
    public byte[] exportCsv(String productId, int page, int size) {
        PageResponse<EntityEffectiveModeDictPo> data =
                page(productId, ListPageWithKeywordQuery.of(page, size, null));
        List<List<String>> rows =
                data.getRecords().stream()
                        .map(
                                r ->
                                        List.of(
                                                StringUtils.defaultString(r.getEffectiveModeId()),
                                                StringUtils.defaultString(r.getEffectiveModeNameCn()),
                                                StringUtils.defaultString(r.getEffectiveModeNameEn()),
                                                StringUtils.defaultString(r.getEffectiveModeDescription())))
                        .collect(Collectors.toList());
        return ExcelHelper.buildWorkbook("生效方式", INSTRUCTION, HEADERS_CN, rows);
    }

    /**
     * 获取生效方式字典导入模板（XLSX）。
     *
     * @return 模板 XLSX 字节
     */
    public byte[] templateCsv() {
        return ExcelHelper.buildTemplate("生效方式", INSTRUCTION, HEADERS_CN);
    }

    private EffectiveModeDomainService ensureDomain() {
        if (domainService == null) {
            domainService = new EffectiveModeDomainService(effectiveModeRepository);
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
