package com.coretool.param.application.service;

import com.coretool.param.application.support.ImportResultCollector;
import com.coretool.param.domain.config.nf.NfConfigEntry;
import com.coretool.param.domain.config.nf.service.NfConfigDomainService;
import com.coretool.param.domain.config.nf.repository.NfConfigRepository;
import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.domain.support.IdGenerator;
import com.coretool.param.infrastructure.persistence.assembly.NfConfigEntryAssembler;
import com.coretool.param.infrastructure.persistence.entity.EntityNfConfigDictPo;
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
public class NfConfigAppService {

    private static final String INSTRUCTION = ExcelInstructions.ID_CREATE_UPDATE_HINT;
    /**
     * 导入/导出字段以页面展示为准：不包含状态、审计字段。
     *
     * <p>兼容：导入时若上传文件额外包含“状态”列，也会被解析；若缺失则默认启用。
     */
    private static final List<String> HEADERS_CN = List.of("ID", "nf 名称", "nf 配置描述");

    @Resource
    private NfConfigRepository nfConfigRepository;

    @Resource
    private OperationLogAppService operationLogAppService;

    private NfConfigDomainService domainService;

    /**
     * 分页查询 NF 配置字典（产品维度）。
     *
     * @param productId 产品 ID
     * @param q 分页与名称关键字（页码、页大小、关键字见 ListPageWithKeywordQuery）
     * @return 分页结果
     */
    public PageResponse<EntityNfConfigDictPo> page(
            String productId, ListPageWithKeywordQuery q) {
        var slice =
                nfConfigRepository.pageByProduct(
                        productId, q.getPage(), q.getSize(), q.getKeyword());
        PageResponse<EntityNfConfigDictPo> resp = new PageResponse<>();
        resp.setRecords(
                slice.getRecords().stream()
                        .map(NfConfigEntryAssembler::toPo)
                        .collect(Collectors.toList()));
        resp.setTotal(slice.getTotal());
        resp.setPage(q.getPage());
        resp.setSize(q.getSize());
        return resp;
    }

    /**
     * 新增 NF 配置字典项（若存在同名禁用项则复用并启用）。
     *
     * @param productId 产品 ID
     * @param input     请求体
     * @return 新增后的数据
     */
    @Transactional
    public EntityNfConfigDictPo create(String productId, EntityNfConfigDictPo input) {
        if (input == null) {
            throw new DomainRuleException("请求体不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        String name = input.getNfConfigNameCn() == null ? "" : input.getNfConfigNameCn().trim();
        NfConfigEntry disabled = nfConfigRepository.findDisabledByNameInProduct(productId, name).orElse(null);
        if (disabled != null) {
            NfConfigEntry before =
                    NfConfigEntry.rehydrate(
                            new NfConfigEntry.Snapshot(
                                    disabled.getOwnedProductId(),
                                    disabled.getNfConfigId(),
                                    disabled.getNfConfigNameCn(),
                                    disabled.getNfConfigDescription(),
                                    disabled.getNfConfigStatus(),
                                    disabled.getCreatorId(),
                                    disabled.getCreationTimestamp(),
                                    disabled.getUpdaterId(),
                                    disabled.getUpdateTimestamp()));
            disabled.applyPatch(
                    new NfConfigEntry.Patch(
                            name,
                            input.getNfConfigDescription(),
                            1,
                            StringUtils.defaultIfBlank(input.getUpdaterId(), "system"),
                            now));
            nfConfigRepository.update(disabled);
            String opR = StringUtils.defaultIfBlank(input.getUpdaterId(), "system");
            operationLogAppService.logNfConfigUpdate(before, disabled, opR);
            return NfConfigEntryAssembler.toPo(disabled);
        }

        if (StringUtils.isBlank(input.getNfConfigId())) {
            input.setNfConfigId(IdGenerator.nfConfigId());
        }
        input.setNfConfigStatus(1);
        NfConfigEntry entry =
                ensureDomain()
                        .createNew(
                                new NfConfigDomainService.CreateCommand(
                                        productId,
                                        input.getNfConfigId(),
                                        input.getNfConfigNameCn(),
                                        input.getNfConfigDescription(),
                                        input.getNfConfigStatus(),
                                        input.getCreatorId(),
                                        input.getUpdaterId(),
                                        now));
        nfConfigRepository.insert(entry);
        EntityNfConfigDictPo out = NfConfigEntryAssembler.toPo(entry);
        String w =
                StringUtils.defaultIfBlank(
                        StringUtils.firstNonBlank(
                                input.getCreatorId(), input.getUpdaterId(), entry.getCreatorId()),
                        "system");
        operationLogAppService.logNfConfigCreate(productId, out, w);
        return out;
    }

    /**
     * 更新 NF 配置字典项。
     *
     * @param productId  产品 ID
     * @param nfConfigId 配置 ID
     * @param input      请求体
     * @return 更新后的数据
     */
    @Transactional
    public EntityNfConfigDictPo update(String productId, String nfConfigId, EntityNfConfigDictPo input) {
        LocalDateTime now = LocalDateTime.now();
        NfConfigEntry before =
                nfConfigRepository
                        .findById(nfConfigId)
                        .filter(n -> n.belongsToProduct(productId))
                        .orElseThrow(() -> new DomainRuleException("NF 配置不存在或不属于该产品"));
        NfConfigEntry existing =
                ensureDomain()
                        .updateExisting(
                                new NfConfigDomainService.UpdateCommand(
                                        productId,
                                        nfConfigId,
                                        input == null ? null : input.getNfConfigNameCn(),
                                        input == null ? null : input.getNfConfigDescription(),
                                        input == null ? null : input.getNfConfigStatus(),
                                        input == null ? null : input.getUpdaterId(),
                                        now));
        nfConfigRepository.update(existing);
        String opU = StringUtils.defaultIfBlank(input == null ? null : input.getUpdaterId(), "system");
        operationLogAppService.logNfConfigUpdate(before, existing, opU);
        return NfConfigEntryAssembler.toPo(existing);
    }

    /**
     * 禁用 NF 配置字典项。
     *
     * @param productId  产品 ID
     * @param nfConfigId 配置 ID
     */
    @Transactional
    public void disable(String productId, String nfConfigId) {
        NfConfigEntry pre = nfConfigRepository.findById(nfConfigId).orElse(null);
        String display = pre == null ? nfConfigId : StringUtils.defaultIfBlank(pre.getNfConfigNameCn(), nfConfigId);
        NfConfigEntry existing = ensureDomain().disable(productId, nfConfigId, LocalDateTime.now());
        nfConfigRepository.update(existing);
        String opD = StringUtils.defaultIfBlank(existing.getUpdaterId(), "system");
        operationLogAppService.logDictRowDelete(
                new OperationLogAppService.LogDictRowDeleteInput(
                        OperationLogAppService.BIZ_TABLE_ENTITY_NF_CONFIG_DICT,
                        productId,
                        null,
                        nfConfigId,
                        display,
                        opD));
    }

    /**
     * 导入 NF 配置字典（Excel 第一张表）。
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
                BatchImportResult e = new BatchImportResult();
                e.setTotalRows(0);
                e.setSuccessCount(0);
                e.setFailureCount(0);
                e.setSuccessRowNumbers(List.of());
                e.setFailures(List.of());
                return e;
            }
            int headerIdx = ExcelHelper.detectHeaderRowIndex(rows);
            Map<String, Integer> idx = ExcelHelper.headerIndex(rows.get(headerIdx));
            ImportResultCollector c = new ImportResultCollector();
            int dataRows = rows.size() - headerIdx - 1;
            for (int i = headerIdx + 1; i < rows.size(); i++) {
                int line = i + 1;
                try {
                    List<String> cols = rows.get(i);
                    EntityNfConfigDictPo po = new EntityNfConfigDictPo();
                    po.setNfConfigId(StringUtils.trimToNull(col(cols, idx, "ID")));
                    po.setNfConfigNameCn(col(cols, idx, "nf 名称"));
                    po.setNfConfigDescription(StringUtils.trimToNull(col(cols, idx, "nf 配置描述")));
                    po.setNfConfigStatus(parseIntDefault(col(cols, idx, "状态(1启用0未启用)"), 1));
                    if (StringUtils.isBlank(po.getNfConfigId())) {
                        create(productId, po);
                    } else {
                        NfConfigEntry ex = nfConfigRepository.findById(po.getNfConfigId()).orElse(null);
                        if (ex == null) {
                            create(productId, po);
                            c.success(line);
                            continue;
                        }
                        ensureDomain().requireOwned(productId, po.getNfConfigId());
                        po.setUpdaterId("system");
                        update(productId, po.getNfConfigId(), po);
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
     * 导出 NF 配置字典（XLSX）。
     *
     * @param productId 产品 ID
     * @param page      页码（从 1 开始）
     * @param size      页大小
     * @return XLSX 字节
     */
    public byte[] exportCsv(String productId, int page, int size) {
        PageResponse<EntityNfConfigDictPo> data =
                page(productId, ListPageWithKeywordQuery.of(page, size, null));
        List<List<String>> rows =
                data.getRecords().stream()
                        .map(
                                r ->
                                        List.of(
                                                StringUtils.defaultString(r.getNfConfigId()),
                                                StringUtils.defaultString(r.getNfConfigNameCn()),
                                                StringUtils.defaultString(r.getNfConfigDescription())))
                        .collect(Collectors.toList());
        return ExcelHelper.buildWorkbook("NF 配置", INSTRUCTION, HEADERS_CN, rows);
    }

    /**
     * 获取 NF 配置字典导入模板（XLSX）。
     *
     * @return 模板 XLSX 字节
     */
    public byte[] templateCsv() {
        return ExcelHelper.buildTemplate("NF 配置", INSTRUCTION, HEADERS_CN);
    }

    private NfConfigDomainService ensureDomain() {
        if (domainService == null) {
            domainService = new NfConfigDomainService(nfConfigRepository);
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
