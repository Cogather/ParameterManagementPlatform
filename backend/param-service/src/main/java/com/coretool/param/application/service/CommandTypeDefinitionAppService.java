package com.coretool.param.application.service;

import com.coretool.param.application.support.ImportResultCollector;
import com.coretool.param.domain.command.CommandTypeDefinition;
import com.coretool.param.domain.command.service.CommandTypeDefinitionDomainService;
import com.coretool.param.domain.command.repository.CommandTypeDefinitionRepository;
import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.domain.support.IdGenerator;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.infrastructure.persistence.assembly.CommandTypeDefinitionAssembler;
import com.coretool.param.infrastructure.persistence.entity.CommandTypeDefinitionPo;
import com.coretool.param.infrastructure.persistence.entity.EntityCommandMappingPo;
import com.coretool.param.infrastructure.persistence.mapper.EntityCommandMappingMapper;
import com.coretool.param.infrastructure.util.ExcelHelper;
import com.coretool.param.infrastructure.util.ExcelInstructions;
import com.coretool.param.ui.exception.BizException;
import com.coretool.param.ui.response.BatchImportResult;
import com.coretool.param.ui.response.PageResponse;
import com.coretool.param.ui.vo.ListPageWithKeywordQuery;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommandTypeDefinitionAppService {

    private static final String INSTRUCTION = ExcelInstructions.ID_CREATE_UPDATE_HINT;
    /**
     * 导入/导出字段以页面展示为准：不包含状态、审计字段。
     *
     * <p>兼容：导入时若上传文件额外包含“状态”列，也会被解析；若缺失则默认启用。
     */
    private static final List<String> HEADERS_CN =
            List.of("归属命令", "类型ID", "类型名称", "类型枚举", "最小序号", "最大序号", "占用序号");

    private final CommandTypeDefinitionRepository repository;
    private final CommandTypeDefinitionDomainService domainService;
    private final OperationLogAppService operationLogAppService;
    private final EntityCommandMappingMapper entityCommandMappingMapper;

    /**
     * 构造函数。
     *
     * @param repository             类型定义仓储
     * @param operationLogAppService 操作日志应用服务
     */
    public CommandTypeDefinitionAppService(
            CommandTypeDefinitionRepository repository,
            OperationLogAppService operationLogAppService,
            EntityCommandMappingMapper entityCommandMappingMapper) {
        this.repository = repository;
        this.domainService = new CommandTypeDefinitionDomainService(repository);
        this.operationLogAppService = operationLogAppService;
        this.entityCommandMappingMapper = entityCommandMappingMapper;
    }

    /**
     * 分页查询类型定义（产品维度）。
     *
     * @param productId 产品 ID
     * @param q 分页与名称关键字（页码、页大小、关键字见 ListPageWithKeywordQuery）
     * @return 分页结果
     */
    public PageResponse<CommandTypeDefinitionPo> page(
            String productId, ListPageWithKeywordQuery q) {
        PageSlice<CommandTypeDefinition> slice =
                repository.pageByProduct(
                        productId, q.getPage(), q.getSize(), q.getKeyword());
        PageResponse<CommandTypeDefinitionPo> resp = new PageResponse<>();
        resp.setRecords(
                slice.getRecords().stream()
                        .map(CommandTypeDefinitionAssembler::toPo)
                        .collect(Collectors.toList()));
        resp.setTotal(slice.getTotal());
        resp.setPage(slice.getPage());
        resp.setSize(slice.getSize());
        return resp;
    }

    /**
     * 新增类型定义（若存在同名禁用项则复用并启用）。
     *
     * @param productId 产品 ID
     * @param input     请求体
     * @return 新增后的数据
     */
    @Transactional
    public CommandTypeDefinitionPo create(String productId, CommandTypeDefinitionPo input) {
        if (input == null) {
            throw new DomainRuleException("请求体不能为空");
        }
        if (StringUtils.isBlank(input.getOwnedCommandId())) {
            throw new DomainRuleException("归属命令ID不能为空");
        }
        String name = input.getCommandTypeName() == null ? "" : input.getCommandTypeName().trim();
        // 新增时若与“已删除/未启用”的同名记录冲突，则直接恢复该记录（启用）并更新字段。
        CommandTypeDefinition disabled = repository.findDisabledByNameInProduct(productId, name).orElse(null);
        if (disabled != null) {
            CommandTypeDefinition before =
                    CommandTypeDefinition.rehydrate(
                            new CommandTypeDefinition.Snapshot(
                                    disabled.getOwnedProductId(),
                                    disabled.getOwnedCommandId(),
                                    disabled.getCommandTypeId(),
                                    disabled.getCommandTypeName(),
                                    disabled.getCommandType(),
                                    disabled.getMinValue(),
                                    disabled.getMaxValue(),
                                    disabled.getOccupiedSerialNumber(),
                                    disabled.getCommandTypeStatus(),
                                    disabled.getCreatorId(),
                                    disabled.getCreationTimestamp(),
                                    disabled.getUpdaterId(),
                                    disabled.getUpdateTimestamp()));
            disabled.applyEditablePatch(
                    new CommandTypeDefinition.EditablePatch(
                            name,
                            input.getCommandType(),
                            input.getMinValue(),
                            input.getMaxValue(),
                            input.getOccupiedSerialNumber(),
                            1,
                            StringUtils.defaultIfBlank(input.getUpdaterId(), "system"),
                            LocalDateTime.now()));
            repository.update(disabled);
            String op = StringUtils.defaultIfBlank(input.getUpdaterId(), "system");
            operationLogAppService.logTypeDefinitionUpdate(before, disabled, op);
            return CommandTypeDefinitionAssembler.toPo(disabled);
        }
        if (StringUtils.isBlank(input.getCommandTypeId())) {
            input.setCommandTypeId(IdGenerator.commandTypeId());
        }
        LocalDateTime now = LocalDateTime.now();
        var t =
                domainService.createNew(
                        new CommandTypeDefinitionDomainService.CreateCommand(
                                productId,
                                input.getOwnedCommandId(),
                                input.getCommandTypeId(),
                                input.getCommandTypeName(),
                                StringUtils.defaultIfBlank(input.getCommandType(), "BIT"),
                                input.getMinValue(),
                                input.getMaxValue(),
                                input.getOccupiedSerialNumber(),
                                input.getCommandTypeStatus(),
                                input.getCreatorId(),
                                input.getUpdaterId(),
                                now));
        repository.insert(t);
        CommandTypeDefinitionPo out = CommandTypeDefinitionAssembler.toPo(t);
        String who =
                StringUtils.defaultIfBlank(
                        StringUtils.firstNonBlank(
                                input.getCreatorId(), input.getUpdaterId(), t.getCreatorId()),
                        "system");
        operationLogAppService.logTypeDefinitionCreate(productId, out, who);
        return out;
    }

    /**
     * 更新类型定义。
     *
     * @param productId     产品 ID
     * @param commandTypeId 类型 ID
     * @param input         请求体
     * @return 更新后的数据
     */
    @Transactional
    public CommandTypeDefinitionPo update(String productId, String commandTypeId, CommandTypeDefinitionPo input) {
        CommandTypeDefinition before =
                repository
                        .findById(commandTypeId)
                        .filter(t -> t.belongsToProduct(productId))
                        .orElseThrow(() -> new DomainRuleException("类型定义不存在或不属于该产品"));
        var existing =
                domainService.updateExisting(
                        new CommandTypeDefinitionDomainService.UpdateCommand(
                                productId,
                                commandTypeId,
                                input == null ? null : input.getCommandTypeName(),
                                input == null ? null : input.getCommandType(),
                                input == null ? null : input.getMinValue(),
                                input == null ? null : input.getMaxValue(),
                                input == null ? null : input.getOccupiedSerialNumber(),
                                input == null ? null : input.getCommandTypeStatus(),
                                input == null ? null : input.getUpdaterId(),
                                LocalDateTime.now()));
        repository.update(existing);
        String op = StringUtils.defaultIfBlank(input == null ? null : input.getUpdaterId(), "system");
        operationLogAppService.logTypeDefinitionUpdate(before, existing, op);
        return CommandTypeDefinitionAssembler.toPo(existing);
    }

    /**
     * 导入类型定义（Excel 第一张表）。
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
            Map<String, String> commandIdByName = loadCommandIdByName(productId);
            ImportResultCollector c = new ImportResultCollector();
            int dataRows = rows.size() - headerIdx - 1;
            for (int i = headerIdx + 1; i < rows.size(); i++) {
                int line = i + 1;
                try {
                    List<String> cols = rows.get(i);
                    CommandTypeDefinitionPo po = new CommandTypeDefinitionPo();
                    String cmdCell = colAny(cols, idx, "归属命令", "归属命令ID");
                    po.setOwnedCommandId(resolveCommandId(commandIdByName, cmdCell));
                    po.setCommandTypeId(StringUtils.trimToNull(col(cols, idx, "类型ID")));
                    po.setCommandTypeName(col(cols, idx, "类型名称"));
                    po.setCommandType(StringUtils.trimToNull(col(cols, idx, "类型枚举")));
                    po.setMinValue(parseIntNullable(col(cols, idx, "最小序号")));
                    po.setMaxValue(parseIntNullable(col(cols, idx, "最大序号")));
                    po.setOccupiedSerialNumber(StringUtils.trimToNull(col(cols, idx, "占用序号")));
                    po.setCommandTypeStatus(parseIntDefault(col(cols, idx, "状态(1启用0未启用)"), 1));
                    if (StringUtils.isBlank(po.getOwnedCommandId()) || StringUtils.isBlank(po.getCommandTypeName())) {
                        throw new BizException("归属命令与类型名称不能为空");
                    }
                    CommandTypeDefinition ex =
                            StringUtils.isNotBlank(po.getCommandTypeId())
                                    ? repository.findById(po.getCommandTypeId()).orElse(null)
                                    : null;
                    if (ex == null) {
                        create(productId, po);
                    } else {
                        if (!ex.belongsToProduct(productId)) {
                            throw new BizException("类型ID与其他产品冲突");
                        }
                        po.setUpdaterId("system");
                        update(productId, po.getCommandTypeId(), po);
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
     * 导出类型定义（XLSX）。
     *
     * @param productId 产品 ID
     * @param page      页码（从 1 开始）
     * @param size      页大小
     * @param keyword   关键字（可选）
     * @return XLSX 字节
     */
    public byte[] exportExcel(String productId, int page, int size, String keyword) {
        PageResponse<CommandTypeDefinitionPo> data =
                page(productId, ListPageWithKeywordQuery.of(page, size, keyword));
        Map<String, String> commandNameById =
                loadCommandNameById(
                        productId,
                        data.getRecords().stream()
                                .map(CommandTypeDefinitionPo::getOwnedCommandId)
                                .filter(StringUtils::isNotBlank)
                                .collect(Collectors.toSet()));
        List<List<String>> rows =
                data.getRecords().stream()
                        .map(
                                r ->
                                        List.of(
                                                StringUtils.defaultIfBlank(
                                                        commandNameById.get(StringUtils.defaultString(r.getOwnedCommandId()).trim()),
                                                        StringUtils.defaultString(r.getOwnedCommandId())),
                                                StringUtils.defaultString(r.getCommandTypeId()),
                                                StringUtils.defaultString(r.getCommandTypeName()),
                                                StringUtils.defaultString(r.getCommandType()),
                                                r.getMinValue() == null ? "" : String.valueOf(r.getMinValue()),
                                                r.getMaxValue() == null ? "" : String.valueOf(r.getMaxValue()),
                                                StringUtils.defaultString(r.getOccupiedSerialNumber())))
                        .collect(Collectors.toList());
        return ExcelHelper.buildWorkbook("类型定义", INSTRUCTION, HEADERS_CN, rows);
    }

    /**
     * 获取类型定义导入模板（XLSX）。
     *
     * @return 模板 XLSX 字节
     */
    public byte[] importTemplate() {
        return ExcelHelper.buildTemplate("类型定义", INSTRUCTION, HEADERS_CN);
    }

    private Map<String, String> loadCommandIdByName(String productId) {
        List<EntityCommandMappingPo> cmds =
                entityCommandMappingMapper.selectList(
                        new LambdaQueryWrapper<EntityCommandMappingPo>()
                                .eq(EntityCommandMappingPo::getOwnedProductId, productId)
                                .ne(EntityCommandMappingPo::getCommandStatus, 0));
        return cmds.stream()
                .filter(c -> c != null && StringUtils.isNotBlank(c.getCommandName()) && StringUtils.isNotBlank(c.getCommandId()))
                .collect(Collectors.toMap(
                        c -> c.getCommandName().trim(),
                        c -> c.getCommandId().trim(),
                        (a, b) -> a));
    }

    private Map<String, String> loadCommandNameById(String productId, java.util.Set<String> commandIds) {
        if (commandIds == null || commandIds.isEmpty()) {
            return Map.of();
        }
        List<EntityCommandMappingPo> cmds =
                entityCommandMappingMapper.selectList(
                        new LambdaQueryWrapper<EntityCommandMappingPo>()
                                .eq(EntityCommandMappingPo::getOwnedProductId, productId)
                                .in(EntityCommandMappingPo::getCommandId, commandIds));
        return cmds.stream()
                .filter(c -> c != null && StringUtils.isNotBlank(c.getCommandId()))
                .collect(Collectors.toMap(
                        c -> c.getCommandId().trim(),
                        c -> StringUtils.defaultIfBlank(c.getCommandName(), c.getCommandId()).trim(),
                        (a, b) -> a));
    }

    private static String resolveCommandId(Map<String, String> commandIdByName, String cell) {
        String v = StringUtils.defaultString(cell).trim();
        if (v.isEmpty()) {
            return null;
        }
        String id = commandIdByName.get(v);
        if (StringUtils.isBlank(id)) {
            throw new BizException("归属命令不存在（请填写命令名称）: " + v);
        }
        return id;
    }

    private static String colAny(List<String> cols, Map<String, Integer> idx, String... headerCns) {
        for (String h : headerCns) {
            if (h == null) continue;
            Integer i = idx.get(h);
            if (i != null && i >= 0 && i < cols.size()) {
                String v = cols.get(i);
                return v == null ? "" : v.trim();
            }
        }
        return "";
    }

    /**
     * 校验并获取指定类型定义（确保归属指定产品）。
     *
     * @param productId     产品 ID
     * @param commandTypeId 类型 ID
     * @return 类型定义领域对象
     */
    public CommandTypeDefinition requireOwned(String productId, String commandTypeId) {
        return domainService.requireOwned(productId, commandTypeId);
    }

    private static Integer parseIntNullable(String s) {
        if (StringUtils.isBlank(s)) {
            return null;
        }
        return Integer.valueOf(s.trim());
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
