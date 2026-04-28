package com.coretool.param.application.service;

import com.coretool.param.application.support.ImportResultCollector;
import com.coretool.param.domain.config.keyword.ChangeSourceKeyword;
import com.coretool.param.domain.config.keyword.repository.ChangeSourceKeywordRepository;
import com.coretool.param.domain.config.keyword.service.ChangeSourceKeywordDomainService;
import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.domain.support.IdGenerator;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.infrastructure.persistence.assembly.ChangeSourceKeywordAssembler;
import com.coretool.param.infrastructure.persistence.entity.ConfigChangeSourceKeywordPo;
import com.coretool.param.infrastructure.util.ExcelHelper;
import com.coretool.param.infrastructure.util.ExcelInstructions;
import com.coretool.param.ui.exception.BizException;
import com.coretool.param.ui.response.BatchImportResult;
import com.coretool.param.ui.response.PageResponse;
import com.coretool.param.ui.vo.ListPageWithKeywordQuery;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChangeSourceKeywordAppService {

    private static final String INSTRUCTION = ExcelInstructions.ID_CREATE_UPDATE_HINT;
    /**
     * 导入/导出字段以页面展示为准：不包含状态、审计字段。
     *
     * <p>兼容：导入时若上传文件额外包含“状态”列，也会被解析；若缺失则默认启用。
     */
    private static final List<String> HEADERS_CN = List.of("关键字 ID", "关键字正则字段", "原因");

    private final ChangeSourceKeywordRepository keywordRepository;
    private final ChangeSourceKeywordDomainService domainService;
    private final OperationLogAppService operationLogAppService;

    /**
     * 构造函数。
     *
     * @param keywordRepository      变更来源关键字仓储
     * @param operationLogAppService 操作日志应用服务
     */
    public ChangeSourceKeywordAppService(
            ChangeSourceKeywordRepository keywordRepository, OperationLogAppService operationLogAppService) {
        this.keywordRepository = keywordRepository;
        this.domainService = new ChangeSourceKeywordDomainService(keywordRepository);
        this.operationLogAppService = operationLogAppService;
    }

    /**
     * 分页查询变更来源关键字字典（产品维度）。
     *
     * @param productId 产品 ID
     * @param q 分页与名称关键字（页码、页大小、关键字见 ListPageWithKeywordQuery）
     * @return 分页结果
     */
    public PageResponse<ConfigChangeSourceKeywordPo> page(
            String productId, ListPageWithKeywordQuery q) {
        PageSlice<ChangeSourceKeyword> slice =
                keywordRepository.pageByProduct(
                        productId, q.getPage(), q.getSize(), q.getKeyword());
        PageResponse<ConfigChangeSourceKeywordPo> resp = new PageResponse<>();
        resp.setRecords(
                slice.getRecords().stream()
                        .map(ChangeSourceKeywordAssembler::toPo)
                        .collect(Collectors.toList()));
        resp.setTotal(slice.getTotal());
        resp.setPage(slice.getPage());
        resp.setSize(slice.getSize());
        return resp;
    }

    /**
     * 新增变更来源关键字字典项。
     *
     * @param productId 产品 ID
     * @param input     请求体
     * @return 新增后的数据
     */
    @Transactional
    public ConfigChangeSourceKeywordPo create(String productId, ConfigChangeSourceKeywordPo input) {
        if (input == null) {
            throw new DomainRuleException("请求体不能为空");
        }
        if (StringUtils.isBlank(input.getKeywordId())) {
            input.setKeywordId(IdGenerator.keywordId());
        }
        input.setKeywordStatus(1);
        LocalDateTime now = LocalDateTime.now();
        ChangeSourceKeyword k =
                domainService.createNew(
                        new ChangeSourceKeywordDomainService.CreateCommand(
                                productId,
                                input.getKeywordId(),
                                input.getKeywordRegex(),
                                input.getReason(),
                                input.getKeywordStatus(),
                                input.getCreatorId(),
                                input.getUpdaterId(),
                                now));
        keywordRepository.insert(k);
        ConfigChangeSourceKeywordPo out = ChangeSourceKeywordAssembler.toPo(k);
        String w =
                StringUtils.defaultIfBlank(
                        StringUtils.firstNonBlank(
                                input.getCreatorId(), input.getUpdaterId(), k.getCreatorId()),
                        "system");
        operationLogAppService.logChangeKeywordCreate(productId, out, w);
        return out;
    }

    /**
     * 更新变更来源关键字字典项。
     *
     * @param productId 产品 ID
     * @param keywordId 关键字 ID
     * @param input     请求体
     * @return 更新后的数据
     */
    @Transactional
    public ConfigChangeSourceKeywordPo update(
            String productId, String keywordId, ConfigChangeSourceKeywordPo input) {
        ChangeSourceKeyword before =
                keywordRepository
                        .findByKeywordId(keywordId)
                        .filter(x -> x.belongsToProduct(productId))
                        .orElseThrow(() -> new DomainRuleException("关键字不存在或不属于该产品"));
        ChangeSourceKeyword existing =
                domainService.updateExisting(
                        new ChangeSourceKeywordDomainService.UpdateCommand(
                                productId,
                                keywordId,
                                input == null ? null : input.getKeywordRegex(),
                                input == null ? null : input.getReason(),
                                input == null ? null : input.getKeywordStatus(),
                                input == null ? null : input.getUpdaterId(),
                                LocalDateTime.now()));
        keywordRepository.update(existing);
        String opU = StringUtils.defaultIfBlank(input == null ? null : input.getUpdaterId(), "system");
        operationLogAppService.logChangeKeywordUpdate(before, existing, opU);
        return ChangeSourceKeywordAssembler.toPo(existing);
    }

    /**
     * 禁用变更来源关键字字典项。
     *
     * @param productId 产品 ID
     * @param keywordId 关键字 ID
     */
    @Transactional
    public void disable(String productId, String keywordId) {
        ChangeSourceKeyword pre = keywordRepository.findByKeywordId(keywordId).orElse(null);
        String display = pre == null ? keywordId : StringUtils.defaultIfBlank(pre.getReason(), keywordId);
        ChangeSourceKeyword existing = domainService.disable(productId, keywordId, LocalDateTime.now());
        keywordRepository.update(existing);
        String opD = StringUtils.defaultIfBlank(existing.getUpdaterId(), "system");
        operationLogAppService.logDictRowDelete(
                new OperationLogAppService.LogDictRowDeleteInput(
                        OperationLogAppService.BIZ_TABLE_CONFIG_CHANGE_SOURCE_KEYWORD,
                        productId,
                        null,
                        keywordId,
                        display,
                        opD));
    }

    /**
     * 导入变更来源关键字字典（Excel 第一张表）。
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
                    upsertFromRow(productId, rows.get(i), idx);
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
     * 导出变更来源关键字字典（XLSX）。
     *
     * @param productId 产品 ID
     * @param page      页码（从 1 开始）
     * @param size      页大小
     * @return XLSX 字节
     */
    public byte[] exportCsv(String productId, int page, int size) {
        PageResponse<ConfigChangeSourceKeywordPo> data =
                page(productId, ListPageWithKeywordQuery.of(page, size, null));
        List<List<String>> rows =
                data.getRecords().stream()
                        .map(
                                r ->
                                        List.of(
                                                StringUtils.defaultString(r.getKeywordId()),
                                                StringUtils.defaultString(r.getKeywordRegex()),
                                                StringUtils.defaultString(r.getReason())))
                        .collect(Collectors.toList());
        return ExcelHelper.buildWorkbook("变更来源关键字", INSTRUCTION, HEADERS_CN, rows);
    }

    /**
     * 获取变更来源关键字字典导入模板（XLSX）。
     *
     * @return 模板 XLSX 字节
     */
    public byte[] templateCsv() {
        return ExcelHelper.buildTemplate("变更来源关键字", INSTRUCTION, HEADERS_CN);
    }

    private void upsertFromRow(String productId, List<String> cols, Map<String, Integer> idx) {
        ConfigChangeSourceKeywordPo po = new ConfigChangeSourceKeywordPo();
        po.setKeywordId(StringUtils.trimToNull(col(cols, idx, "关键字 ID")));
        po.setKeywordRegex(col(cols, idx, "关键字正则字段"));
        po.setReason(StringUtils.trimToNull(col(cols, idx, "原因")));
        po.setKeywordStatus(parseIntDefault(col(cols, idx, "状态(1启用0未启用)"), 1));
        if (StringUtils.isBlank(po.getKeywordId())) {
            create(productId, po);
        } else {
            keywordRepository
                    .findByKeywordId(po.getKeywordId())
                    .orElseThrow(() -> new DomainRuleException("关键字不存在"));
            domainService.requireOwned(productId, po.getKeywordId());
            po.setUpdaterId("system");
            update(productId, po.getKeywordId(), po);
        }
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
