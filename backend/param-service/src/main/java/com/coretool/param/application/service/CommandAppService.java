package com.coretool.param.application.service;

import com.coretool.param.application.support.ImportResultCollector;
import com.coretool.param.domain.command.Command;
import com.coretool.param.domain.command.service.CommandDomainService;
import com.coretool.param.domain.command.repository.CommandRepository;
import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.domain.support.IdGenerator;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.infrastructure.persistence.assembly.CommandAssembler;
import com.coretool.param.infrastructure.persistence.entity.EntityCommandMappingPo;
import com.coretool.param.infrastructure.util.ExcelHelper;
import com.coretool.param.infrastructure.util.ExcelInstructions;
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
public class CommandAppService {

    private static final String INSTRUCTION = ExcelInstructions.ID_CREATE_UPDATE_HINT;
    /**
     * 导入/导出字段以页面展示为准：不包含状态、审计字段。
     *
     * <p>兼容：导入时若上传文件额外包含“状态”列，也会被解析；若缺失则默认启用。
     */
    private static final List<String> HEADERS_CN = List.of("命令ID", "命令", "责任人(英文逗号)");

    private final CommandRepository commandRepository;
    private final CommandDomainService domainService;
    private final OperationLogAppService operationLogAppService;

    /**
     * 构造函数。
     *
     * @param commandRepository      命令仓储
     * @param operationLogAppService 操作日志应用服务
     */
    public CommandAppService(
            CommandRepository commandRepository, OperationLogAppService operationLogAppService) {
        this.commandRepository = commandRepository;
        this.domainService = new CommandDomainService(commandRepository);
        this.operationLogAppService = operationLogAppService;
    }

    /**
     * 分页查询命令（产品维度）。
     *
     * @param productId 产品 ID
     * @param q 分页与名称关键字（页码、页大小、关键字见 ListPageWithKeywordQuery）
     * @return 分页结果
     */
    public PageResponse<EntityCommandMappingPo> page(
            String productId, ListPageWithKeywordQuery q) {
        PageSlice<Command> slice =
                commandRepository.pageByProduct(
                        productId, q.getPage(), q.getSize(), q.getKeyword());
        PageResponse<EntityCommandMappingPo> resp = new PageResponse<>();
        resp.setRecords(
                slice.getRecords().stream().map(CommandAssembler::toPo).collect(Collectors.toList()));
        resp.setTotal(slice.getTotal());
        resp.setPage(slice.getPage());
        resp.setSize(slice.getSize());
        return resp;
    }

    /**
     * 新增命令（若存在同名禁用项则复用并启用）。
     *
     * @param productId 产品 ID
     * @param input     请求体
     * @return 新增后的数据
     */
    @Transactional
    public EntityCommandMappingPo create(String productId, EntityCommandMappingPo input) {
        if (input == null) {
            throw new DomainRuleException("请求体不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        String name = input.getCommandName() == null ? "" : input.getCommandName().trim();
        // 新增时若与“已删除/未启用”的同名记录冲突，则直接恢复该记录（启用）并更新字段。
        Command disabled = commandRepository.findDisabledByNameInProduct(productId, name).orElse(null);
        if (disabled != null) {
            String op = StringUtils.defaultIfBlank(input.getUpdaterId(), "system");
            String oldName = disabled.getCommandName();
            String oldOwner = disabled.getOwnerList();
            Command before =
                    Command.rehydrate(
                            new Command.Snapshot(
                                    disabled.getOwnedProductId(),
                                    disabled.getCommandId(),
                                    oldName,
                                    disabled.getCreatorId(),
                                    disabled.getCreationTimestamp(),
                                    disabled.getUpdaterId(),
                                    disabled.getUpdateTimestamp(),
                                    oldOwner,
                                    disabled.getCommandStatus()));
            disabled.applyEditablePatch(
                    new Command.EditablePatch(name, input.getOwnerList(), 1, op, now));
            commandRepository.update(disabled);
            operationLogAppService.logCommandUpdate(before, disabled, op);
            return CommandAssembler.toPo(disabled);
        }

        if (StringUtils.isBlank(input.getCommandId())) {
            input.setCommandId(IdGenerator.commandId());
        }
        input.setCommandStatus(1);
        Command c =
                domainService.createNew(
                        new CommandDomainService.CreateCommand(
                                productId,
                                input.getCommandId(),
                                input.getCommandName(),
                                input.getOwnerList(),
                                input.getCommandStatus(),
                                input.getCreatorId(),
                                input.getUpdaterId(),
                                now));
        commandRepository.insert(c);
        EntityCommandMappingPo out = CommandAssembler.toPo(c);
        String who =
                StringUtils.defaultIfBlank(
                        StringUtils.firstNonBlank(
                                input.getCreatorId(), input.getUpdaterId(), c.getCreatorId()),
                        "system");
        operationLogAppService.logCommandCreate(productId, out, who);
        return out;
    }

    /**
     * 更新命令。
     *
     * @param productId 产品 ID
     * @param commandId 命令 ID
     * @param input     请求体
     * @return 更新后的数据
     */
    @Transactional
    public EntityCommandMappingPo update(String productId, String commandId, EntityCommandMappingPo input) {
        Command before =
                commandRepository
                        .findById(commandId)
                        .filter(c -> c.belongsToProduct(productId))
                        .orElseThrow(() -> new DomainRuleException("命令不存在或不属于该产品"));
        Command existing =
                domainService.updateExisting(
                        new CommandDomainService.UpdateCommand(
                                productId,
                                commandId,
                                input == null ? null : input.getCommandName(),
                                input == null ? null : input.getOwnerList(),
                                input == null ? null : input.getCommandStatus(),
                                input == null ? null : input.getUpdaterId(),
                                LocalDateTime.now()));
        commandRepository.update(existing);
        String op = StringUtils.defaultIfBlank(input == null ? null : input.getUpdaterId(), "system");
        operationLogAppService.logCommandUpdate(before, existing, op);
        return CommandAssembler.toPo(existing);
    }

    /**
     * 禁用命令。
     *
     * @param productId 产品 ID
     * @param commandId 命令 ID
     */
    @Transactional
    public void disable(String productId, String commandId) {
        Command pre = commandRepository.findById(commandId).orElse(null);
        String displayName = pre == null ? "" : pre.getCommandName();
        Command existing = domainService.disable(productId, commandId, LocalDateTime.now());
        commandRepository.update(existing);
        String op = StringUtils.defaultIfBlank(existing.getUpdaterId(), "system");
        operationLogAppService.logCommandDelete(productId, commandId, displayName, op);
    }

    /**
     * 导入命令（Excel 第一张表）。
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
                    EntityCommandMappingPo po = new EntityCommandMappingPo();
                    po.setCommandId(StringUtils.trimToNull(col(cols, idx, "命令ID")));
                    po.setCommandName(col(cols, idx, "命令"));
                    po.setOwnerList(col(cols, idx, "责任人(英文逗号)"));
                    po.setCommandStatus(parseIntDefault(col(cols, idx, "状态(1启用0未启用)"), 1));
                    if (StringUtils.isBlank(po.getCommandId())) {
                        create(productId, po);
                    } else {
                        Command ex = commandRepository.findById(po.getCommandId()).orElse(null);
                        if (ex == null) {
                            create(productId, po);
                            c.success(line);
                            continue;
                        }
                        domainService.requireOwned(productId, po.getCommandId());
                        po.setUpdaterId("system");
                        update(productId, po.getCommandId(), po);
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
     * 导出命令（XLSX）。
     *
     * @param productId 产品 ID
     * @param page      页码（从 1 开始）
     * @param size      页大小
     * @param keyword   关键字（可选）
     * @return XLSX 字节
     */
    public byte[] exportExcel(String productId, int page, int size, String keyword) {
        PageResponse<EntityCommandMappingPo> data =
                page(productId, ListPageWithKeywordQuery.of(page, size, keyword));
        List<List<String>> rows =
                data.getRecords().stream()
                        .map(
                                r ->
                                        List.of(
                                                StringUtils.defaultString(r.getCommandId()),
                                                StringUtils.defaultString(r.getCommandName()),
                                                StringUtils.defaultString(r.getOwnerList())))
                        .collect(Collectors.toList());
        return ExcelHelper.buildWorkbook("命令", INSTRUCTION, HEADERS_CN, rows);
    }

    /**
     * 获取命令导入模板（XLSX）。
     *
     * @return 模板 XLSX 字节
     */
    public byte[] importTemplate() {
        return ExcelHelper.buildTemplate("命令", INSTRUCTION, HEADERS_CN);
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

