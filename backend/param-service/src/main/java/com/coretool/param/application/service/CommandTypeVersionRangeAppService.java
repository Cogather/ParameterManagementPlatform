package com.coretool.param.application.service;

import com.coretool.param.application.support.ImportResultCollector;
import com.coretool.param.domain.command.CommandTypeVersionRange;
import com.coretool.param.domain.command.service.CommandTypeDefinitionDomainService;
import com.coretool.param.domain.command.service.CommandTypeVersionRangeDomainService;
import com.coretool.param.domain.command.repository.CommandTypeDefinitionRepository;
import com.coretool.param.domain.command.repository.CommandTypeVersionRangeRepository;
import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.domain.support.IdGenerator;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.infrastructure.persistence.assembly.CommandTypeVersionRangeAssembler;
import com.coretool.param.infrastructure.persistence.entity.CommandTypeVersionRangePo;
import com.coretool.param.infrastructure.util.ExcelHelper;
import com.coretool.param.infrastructure.util.ExcelInstructions;
import com.coretool.param.ui.exception.BizException;
import com.coretool.param.ui.response.BatchImportResult;
import com.coretool.param.ui.response.PageResponse;
import com.coretool.param.ui.vo.ListPageWithTypeFilterQuery;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommandTypeVersionRangeAppService {

    private static final String INSTRUCTION = ExcelInstructions.ID_CREATE_UPDATE_HINT;
    /**
     * 导入/导出字段以页面展示为准：不包含状态、审计字段。
     *
     * <p>兼容：导入时若上传文件额外包含“状态”列，也会被解析；若缺失则默认启用。
     */
    private static final List<String> HEADERS_CN =
            List.of("归属命令ID", "类型ID", "区段ID", "起始序号", "结束序号", "说明", "区段划分类型", "归属版本ID");

    private final CommandTypeVersionRangeRepository rangeRepository;
    private final CommandTypeVersionRangeDomainService domainService;
    private final OperationLogAppService operationLogAppService;

    /**
     * 构造函数。
     *
     * @param rangeRepository        区段仓储
     * @param typeRepository         类型定义仓储（用于归属校验）
     * @param operationLogAppService 操作日志应用服务
     */
    public CommandTypeVersionRangeAppService(
            CommandTypeVersionRangeRepository rangeRepository,
            CommandTypeDefinitionRepository typeRepository,
            OperationLogAppService operationLogAppService) {
        this.rangeRepository = rangeRepository;
        this.domainService =
                new CommandTypeVersionRangeDomainService(
                        rangeRepository, new CommandTypeDefinitionDomainService(typeRepository));
        this.operationLogAppService = operationLogAppService;
    }

    /**
     * 分页查询版本区段（产品维度）。
     *
     * @param productId 产品 ID
     * @param q 分页与类型过滤（页码、页大小、类型 ID 见 ListPageWithTypeFilterQuery）
     * @return 分页结果
     */
    public PageResponse<CommandTypeVersionRangePo> page(
            String productId, ListPageWithTypeFilterQuery q) {
        PageSlice<CommandTypeVersionRange> slice =
                rangeRepository.pageByProduct(
                        productId, q.getPage(), q.getSize(), q.getOwnedTypeId());
        PageResponse<CommandTypeVersionRangePo> resp = new PageResponse<>();
        resp.setRecords(
                slice.getRecords().stream()
                        .map(CommandTypeVersionRangeAssembler::toPo)
                        .collect(Collectors.toList()));
        resp.setTotal(slice.getTotal());
        resp.setPage(slice.getPage());
        resp.setSize(slice.getSize());
        return resp;
    }

    /**
     * 新增版本区段。
     *
     * @param productId 产品 ID
     * @param input     请求体
     * @return 新增后的数据
     */
    @Transactional
    public CommandTypeVersionRangePo create(String productId, CommandTypeVersionRangePo input) {
        if (input == null) {
            throw new DomainRuleException("请求体不能为空");
        }
        if (StringUtils.isBlank(input.getVersionRangeId())) {
            input.setVersionRangeId(IdGenerator.versionRangeId());
        }
        if (input.getRangeStatus() == null) {
            input.setRangeStatus(1);
        }
        LocalDateTime now = LocalDateTime.now();
        var r =
                domainService.createNew(
                        new CommandTypeVersionRangeDomainService.CreateCommand(
                                productId,
                                input.getOwnedCommandId(),
                                input.getOwnedTypeId(),
                                input.getVersionRangeId(),
                                input.getStartIndex(),
                                input.getEndIndex(),
                                input.getRangeDescription(),
                                input.getRangeType(),
                                input.getOwnedVersionOrBusinessId(),
                                input.getRangeStatus(),
                                input.getCreatorId(),
                                input.getUpdaterId(),
                                now));
        rangeRepository.insert(r);
        CommandTypeVersionRangePo out = CommandTypeVersionRangeAssembler.toPo(r);
        String who =
                StringUtils.defaultIfBlank(
                        StringUtils.firstNonBlank(
                                input.getCreatorId(), input.getUpdaterId(), r.getCreatorId()),
                        "system");
        operationLogAppService.logRangeCreate(productId, out, who);
        return out;
    }

    /**
     * 更新版本区段。
     *
     * @param productId 产品 ID
     * @param rangeId   区段 ID
     * @param input     请求体
     * @return 更新后的数据
     */
    @Transactional
    public CommandTypeVersionRangePo update(String productId, String rangeId, CommandTypeVersionRangePo input) {
        CommandTypeVersionRange before =
                rangeRepository
                        .findById(rangeId)
                        .filter(x -> x.belongsToProduct(productId))
                        .orElseThrow(() -> new DomainRuleException("版本区段不存在或不属于该产品"));
        var existing =
                domainService.updateExisting(
                        new CommandTypeVersionRangeDomainService.UpdateCommand(
                                productId,
                                rangeId,
                                input == null ? null : input.getStartIndex(),
                                input == null ? null : input.getEndIndex(),
                                input == null ? null : input.getRangeDescription(),
                                input == null ? null : input.getRangeType(),
                                input == null ? null : input.getOwnedVersionOrBusinessId(),
                                input == null ? null : input.getRangeStatus(),
                                input == null ? null : input.getUpdaterId(),
                                LocalDateTime.now()));
        rangeRepository.update(existing);
        String op = StringUtils.defaultIfBlank(input == null ? null : input.getUpdaterId(), "system");
        operationLogAppService.logRangeUpdate(before, existing, op);
        return CommandTypeVersionRangeAssembler.toPo(existing);
    }

    /**
     * 禁用版本区段。
     *
     * @param productId 产品 ID
     * @param rangeId   区段 ID
     */
    @Transactional
    public void disable(String productId, String rangeId) {
        CommandTypeVersionRange pre = rangeRepository.findById(rangeId).orElse(null);
        String label = pre == null ? rangeId : StringUtils.defaultIfBlank(pre.getRangeDescription(), rangeId);
        var disabled = domainService.disable(productId, rangeId, LocalDateTime.now());
        rangeRepository.update(disabled);
        String op = StringUtils.defaultIfBlank(disabled.getUpdaterId(), "system");
        operationLogAppService.logRangeDelete(productId, rangeId, label, op);
    }

    /**
     * 导入版本区段（Excel 第一张表）。
     *
     * @param productId 产品 ID
     * @param bytes     文件内容
     * @return 导入结果
     */
    public BatchImportResult importExcel(String productId, byte[] bytes) {
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
                    CommandTypeVersionRangePo po = new CommandTypeVersionRangePo();
                    po.setOwnedCommandId(StringUtils.trimToNull(col(cols, idx, "归属命令ID")));
                    po.setOwnedTypeId(StringUtils.trimToNull(col(cols, idx, "类型ID")));
                    po.setVersionRangeId(StringUtils.trimToNull(col(cols, idx, "区段ID")));
                    po.setStartIndex(parseIntNullable(col(cols, idx, "起始序号")));
                    po.setEndIndex(parseIntNullable(col(cols, idx, "结束序号")));
                    po.setRangeDescription(StringUtils.trimToNull(col(cols, idx, "说明")));
                    po.setRangeType(StringUtils.trimToNull(col(cols, idx, "区段划分类型")));
                    po.setOwnedVersionOrBusinessId(StringUtils.trimToNull(col(cols, idx, "归属版本ID")));
                    po.setRangeStatus(parseIntDefault(col(cols, idx, "状态(1启用0未启用)"), 1));
                    if (StringUtils.isAnyBlank(
                            po.getOwnedCommandId(), po.getOwnedTypeId(), po.getOwnedVersionOrBusinessId())) {
                        throw new BizException("归属命令ID、类型ID、归属版本ID不能为空");
                    }
                    if (StringUtils.isBlank(po.getVersionRangeId())) {
                        create(productId, po);
                    } else {
                        CommandTypeVersionRange ex = rangeRepository.findById(po.getVersionRangeId()).orElse(null);
                        if (ex == null) {
                            create(productId, po);
                            c.success(line);
                            continue;
                        }
                        if (!ex.belongsToProduct(productId)) {
                            throw new BizException("区段ID与其他产品冲突");
                        }
                        po.setUpdaterId("system");
                        update(productId, po.getVersionRangeId(), po);
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
     * 导出版本区段（XLSX）。
     *
     * @param productId   产品 ID
     * @param page        页码（从 1 开始）
     * @param size        页大小
     * @param ownedTypeId 归属类型 ID（可选）
     * @return XLSX 字节
     */
    public byte[] exportExcel(String productId, int page, int size, String ownedTypeId) {
        PageResponse<CommandTypeVersionRangePo> data =
                page(productId, ListPageWithTypeFilterQuery.of(page, size, ownedTypeId));
        List<List<String>> rows =
                data.getRecords().stream()
                        .map(
                                r ->
                                        List.of(
                                                StringUtils.defaultString(r.getOwnedCommandId()),
                                                StringUtils.defaultString(r.getOwnedTypeId()),
                                                StringUtils.defaultString(r.getVersionRangeId()),
                                                r.getStartIndex() == null ? "" : String.valueOf(r.getStartIndex()),
                                                r.getEndIndex() == null ? "" : String.valueOf(r.getEndIndex()),
                                                StringUtils.defaultString(r.getRangeDescription()),
                                                StringUtils.defaultString(r.getRangeType()),
                                                StringUtils.defaultString(r.getOwnedVersionOrBusinessId())))
                        .collect(Collectors.toList());
        return ExcelHelper.buildWorkbook("版本区段", INSTRUCTION, HEADERS_CN, rows);
    }

    /**
     * 获取版本区段导入模板（XLSX）。
     *
     * @return 模板 XLSX 字节
     */
    public byte[] importTemplate() {
        return ExcelHelper.buildTemplate("版本区段", INSTRUCTION, HEADERS_CN);
    }

    // overlap / 归属校验等领域规则已下沉至 domain Service

    private static int parseIntDefault(String s, int def) {
        if (StringUtils.isBlank(s)) {
            return def;
        }
        return Integer.parseInt(s.trim());
    }

    private static Integer parseIntNullable(String s) {
        if (StringUtils.isBlank(s)) {
            return null;
        }
        return Integer.valueOf(s.trim());
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
