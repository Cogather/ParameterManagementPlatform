/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coretool.param.application.support.ImportResultCollector;
import com.coretool.param.application.support.ParameterDefaults;
import com.coretool.param.application.support.RequestOperatorIds;
import com.coretool.param.domain.config.keyword.repository.ChangeSourceKeywordRepository;
import com.coretool.param.domain.exception.BlacklistViolationException;
import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.domain.parameter.ChangeSourceBlacklistPolicy;
import com.coretool.param.domain.parameter.ParameterAllocationDomainService;
import com.coretool.param.domain.parameter.ParameterBaselinePolicy;
import com.coretool.param.domain.parameter.ParameterCode;
import com.coretool.param.domain.parameter.ParameterSaveInvariant;
import com.coretool.param.domain.support.IdGenerator;
import com.coretool.param.infrastructure.persistence.assembly.ParameterAssembler;
import com.coretool.param.infrastructure.persistence.entity.CommandTypeDefinitionPo;
import com.coretool.param.infrastructure.persistence.entity.CommandTypeVersionRangePo;
import com.coretool.param.infrastructure.persistence.entity.ConfigChangeDescriptionPo;
import com.coretool.param.infrastructure.persistence.entity.EntityCommandMappingPo;
import com.coretool.param.infrastructure.persistence.entity.EntityVersionInfoPo;
import com.coretool.param.infrastructure.persistence.entity.SystemParameterPo;
import com.coretool.param.infrastructure.persistence.mapper.CommandTypeDefinitionMapper;
import com.coretool.param.infrastructure.persistence.mapper.CommandTypeVersionRangeMapper;
import com.coretool.param.infrastructure.persistence.mapper.ConfigChangeDescriptionMapper;
import com.coretool.param.infrastructure.persistence.mapper.EntityCommandMappingMapper;
import com.coretool.param.infrastructure.persistence.mapper.EntityVersionInfoMapper;
import com.coretool.param.infrastructure.persistence.mapper.SystemParameterMapper;
import com.coretool.param.infrastructure.util.ExcelHelper;
import com.coretool.param.infrastructure.util.ExcelInstructions;
import com.coretool.param.ui.response.AvailableBitsData;
import com.coretool.param.ui.response.AvailableSequencesData;
import com.coretool.param.ui.response.BatchImportResult;
import com.coretool.param.ui.response.PageResponse;
import com.coretool.param.ui.vo.ParameterPageQuery;
import com.coretool.param.ui.vo.ParameterSaveRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 应用服务「ParameterAppService」，编排用例与事务边界。
 *
 * @since 2026-04-28
 */

@Service
public class ParameterAppService {

    private static final int FALLBACK_RANGE_MIN = 1;
    private static final int FALLBACK_RANGE_MAX = 32;

    private final ParameterAllocationDomainService allocation = new ParameterAllocationDomainService();
    private final SystemParameterMapper systemParameterMapper;
    private final ConfigChangeDescriptionMapper configChangeDescriptionMapper;
    private final CommandTypeVersionRangeMapper commandTypeVersionRangeMapper;
    private final CommandTypeDefinitionMapper commandTypeDefinitionMapper;
    private final EntityVersionInfoMapper entityVersionInfoMapper;
    private final ChangeSourceKeywordRepository changeSourceKeywordRepository;
    private final ConfigChangeTypeAppService configChangeTypeAppService;
    private final OperationLogAppService operationLogAppService;
    private final EntityCommandMappingMapper entityCommandMappingMapper;

    /**
     * 构造应用服务（依赖分两组 {@link ParameterAppPersistenceMappers} / {@link ParameterAppCollaboration}，单组形参 ≤5）。
     *
     * @param persistence    持久化 Mapper 分组
     * @param collaboration  协作依赖（仓储、操作日志等）
     */
    public ParameterAppService(
            ParameterAppPersistenceMappers persistence, ParameterAppCollaboration collaboration) {
        this.systemParameterMapper = persistence.systemParameterMapper();
        this.configChangeDescriptionMapper = persistence.configChangeDescriptionMapper();
        this.commandTypeVersionRangeMapper = persistence.commandTypeVersionRangeMapper();
        this.commandTypeDefinitionMapper = persistence.commandTypeDefinitionMapper();
        this.entityVersionInfoMapper = persistence.entityVersionInfoMapper();
        this.changeSourceKeywordRepository = collaboration.changeSourceKeywordRepository();
        this.configChangeTypeAppService = collaboration.configChangeTypeAppService();
        this.operationLogAppService = collaboration.operationLogAppService();
        this.entityCommandMappingMapper = collaboration.entityCommandMappingMapper();
    }

    /**
     * 统计指定版本已基线参数数量。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @return 基线数量
     */
    public long countBaselineInVersion(String productId, String versionId) {
        Long n =
                systemParameterMapper.selectCount(
                        new LambdaQueryWrapper<SystemParameterPo>()
                                .eq(SystemParameterPo::getOwnedProductId, productId)
                                .eq(SystemParameterPo::getOwnedVersionId, versionId)
                                .eq(SystemParameterPo::getDataStatus, ParameterBaselinePolicy.STATUS_BASELINE_LOCKED));
        return n != null ? n : 0L;
    }

    /**
     * 统计产品维度已基线参数数量（不区分版本，版本下拉 ALL 视图）。
     *
     * @param productId 产品 ID
     * @return 基线数量
     */
    public long countBaselineInProduct(String productId) {
        Long n =
                systemParameterMapper.selectCount(
                        new LambdaQueryWrapper<SystemParameterPo>()
                                .eq(SystemParameterPo::getOwnedProductId, productId)
                                .eq(SystemParameterPo::getDataStatus, ParameterBaselinePolicy.STATUS_BASELINE_LOCKED));
        return n != null ? n : 0L;
    }

    /**
     * 分页查询版本维度参数列表。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @param query     查询条件与分页
     * @return 分页结果
     */
    public PageResponse<SystemParameterPo> page(
            String productId, String versionId, ParameterPageQuery query) {
        String commandTypeCode =
                optionalCommandTypeKey(query.getCommandTypeId(), query.getCommandTypeCode());
        int page = query.getPage();
        int size = query.getSize();
        Page<SystemParameterPo> p = new Page<>(page, size);
        LambdaQueryWrapper<SystemParameterPo> w =
                new LambdaQueryWrapper<SystemParameterPo>()
                        .eq(SystemParameterPo::getOwnedProductId, productId)
                        .eq(SystemParameterPo::getOwnedVersionId, versionId)
                        .orderByDesc(SystemParameterPo::getParameterId);
        if (StringUtils.isNotBlank(query.getCommandId())) {
            w.eq(SystemParameterPo::getOwnedCommandId, query.getCommandId());
        }
        if (StringUtils.isNotBlank(commandTypeCode)) {
            w.likeRight(SystemParameterPo::getParameterCode, commandTypeCode + "_");
        }
        Page<SystemParameterPo> result = systemParameterMapper.selectPage(p, w);
        PageResponse<SystemParameterPo> resp = new PageResponse<>();
        resp.setRecords(result.getRecords());
        resp.setTotal(result.getTotal());
        resp.setPage(page);
        resp.setSize(size);
        return resp;
    }

    /**
     * 解析类型筛选键：与 spec-03 一致，优先 commandTypeId，否则 commandTypeCode；可全空。
     *
     * @param commandTypeId   类型 ID（可选）
     * @param commandTypeCode 类型编码（可选）
     * @return 类型键，二者均为空时返回 null
     */
    private static String optionalCommandTypeKey(String commandTypeId, String commandTypeCode) {
        if (StringUtils.isNotBlank(commandTypeId)) {
            return commandTypeId.trim();
        }
        return commandTypeCode == null ? null : commandTypeCode.trim();
    }

    /**
     * 分页查询产品维度参数列表（同一产品下全部版本）。
     *
     * @param productId 产品 ID
     * @param query 全产品参数分页与筛选（命令、类型、页码、页大小等见 ParameterPageQuery）
     * @return 分页结果
     */
    public PageResponse<SystemParameterPo> pageByProduct(String productId, ParameterPageQuery query) {
        String commandTypeCode =
                optionalCommandTypeKey(query.getCommandTypeId(), query.getCommandTypeCode());
        int page = query.getPage();
        int size = query.getSize();
        Page<SystemParameterPo> p = new Page<>(page, size);
        LambdaQueryWrapper<SystemParameterPo> w =
                new LambdaQueryWrapper<SystemParameterPo>()
                        .eq(SystemParameterPo::getOwnedProductId, productId)
                        .orderByDesc(SystemParameterPo::getOwnedVersionId)
                        .orderByDesc(SystemParameterPo::getParameterId);
        if (StringUtils.isNotBlank(query.getCommandId())) {
            w.eq(SystemParameterPo::getOwnedCommandId, query.getCommandId());
        }
        if (StringUtils.isNotBlank(commandTypeCode)) {
            w.likeRight(SystemParameterPo::getParameterCode, commandTypeCode + "_");
        }
        Page<SystemParameterPo> result = systemParameterMapper.selectPage(p, w);
        PageResponse<SystemParameterPo> resp = new PageResponse<>();
        resp.setRecords(result.getRecords());
        resp.setTotal(result.getTotal());
        resp.setPage(page);
        resp.setSize(size);
        return resp;
    }

    /**
     * 查询可用参数序号集合。
     *
     * @param productId       产品 ID
     * @param versionId       版本 ID
     * @param commandId       命令 ID
     * @param commandTypeCode 类型键（必填）
     * @return 可用序号数据
     * @throws DomainRuleException commandTypeCode 为空时
     */
    public AvailableSequencesData availableSequences(
            String productId, String versionId, String commandId, String commandTypeCode) {
        if (StringUtils.isBlank(commandTypeCode)) {
            throw new DomainRuleException("commandTypeCode 必填");
        }
        int[] range = resolveSequenceRange(productId, versionId, commandId, commandTypeCode);
        String typeEnum = resolveTypeEnumForAllocation(productId, commandId, commandTypeCode);
        List<SystemParameterPo> all = loadParametersForCommand(productId, versionId, commandId);
        List<ParameterAllocationDomainService.ParameterSnapshot> snapshots =
                all.stream().map(ParameterAssembler::toSnapshot).collect(Collectors.toList());
        List<ParameterAllocationDomainService.SequenceAvailability> seq =
                allocation.computeAvailableSequences(
                        range[0], range[1], typeEnum, commandId, snapshots);
        AvailableSequencesData data = new AvailableSequencesData();
        List<AvailableSequencesData.SequenceItem> items = new ArrayList<>();
        for (ParameterAllocationDomainService.SequenceAvailability s : seq) {
            AvailableSequencesData.SequenceItem it = new AvailableSequencesData.SequenceItem();
            it.setSequence(s.sequence());
            it.setAvailability(s.availability());
            items.add(it);
        }
        data.setSequences(items);
        return data;
    }

    /**
     * 查询指定序号下的可用 BIT 集合。
     *
     * @param productId       产品 ID
     * @param versionId       版本 ID
     * @param commandId       命令 ID
     * @param commandTypeCode 类型键（必填）
     * @param sequence        参数序号
     * @return 可用 BIT 数据
     * @throws DomainRuleException commandTypeCode 为空时
     */
    public AvailableBitsData availableBits(
            String productId,
            String versionId,
            String commandId,
            String commandTypeCode,
            int sequence) {
        if (StringUtils.isBlank(commandTypeCode)) {
            throw new DomainRuleException("commandTypeCode 必填");
        }
        String typeEnum = resolveTypeEnumForAllocation(productId, commandId, commandTypeCode);
        List<SystemParameterPo> all = loadParametersForCommand(productId, versionId, commandId);
        List<ParameterAllocationDomainService.ParameterSnapshot> snapshots =
                all.stream().map(ParameterAssembler::toSnapshot).collect(Collectors.toList());
        List<Integer> bits =
                allocation.computeAvailableBitIndexes(sequence, typeEnum, commandId, snapshots);
        AvailableBitsData d = new AvailableBitsData();
        d.setSequence(sequence);
        d.setAvailableBitIndexes(bits);
        return d;
    }

    /**
     * 新增参数（版本维度）。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @param request   保存请求（包含 main 与 changeDescriptions）
     * @return 新增后的参数
     * @throws DomainRuleException       请求体或业务校验失败时
     * @throws BlacklistViolationException 变更来源命中黑名单时
     */
    @Transactional
    public SystemParameterPo create(String productId, String versionId, ParameterSaveRequest request) {
        if (request == null || request.getMain() == null) {
            throw new DomainRuleException("请求体或 main 不能为空");
        }
        SystemParameterPo main = request.getMain();
        main.setOwnedProductId(productId);
        main.setOwnedVersionId(versionId);
        if (StringUtils.isBlank(main.getOwnedCommandId())) {
            throw new DomainRuleException("owned_command_id 必填");
        }
        ParameterDefaults.applyForCreate(main);
        validateAndApplyBlacklist(productId, main);
        ParameterSaveInvariant.assertSequenceMatchesCode(main.getParameterCode(), main.getParameterSequence());
        validateChangeDescriptions(true, request.getChangeDescriptions());
        List<SystemParameterPo> existing = loadParametersForCommand(productId, versionId, main.getOwnedCommandId());
        List<ParameterSaveInvariant.ParameterRowForBitCheck> bitRows = toBitRows(existing);
        bitRows.add(
                new ParameterSaveInvariant.ParameterRowForBitCheck(
                        null, main.getParameterCode(), main.getBitUsage()));
        ParameterSaveInvariant.assertBitDisjointAcrossVersionCommand(null, bitRows);
        LocalDateTime now = LocalDateTime.now();
        main.setCreationTimestamp(now);
        main.setUpdateTimestamp(now);
        systemParameterMapper.insert(main);
        Integer pid = main.getParameterId();
        insertChangeDescriptions(pid, request.getChangeDescriptions(), now);
        String who = StringUtils.defaultIfBlank(main.getCreatorId(), "system");
        operationLogAppService.logSystemParameterCreate(main, who);
        return main;
    }

    /**
     * 更新参数（版本维度）。
     *
     * @param productId    产品 ID
     * @param versionId    版本 ID
     * @param parameterId  参数 ID
     * @param request      保存请求（包含 main 与 changeDescriptions）
     * @return 更新后的参数
     * @throws DomainRuleException       请求体、基线锁定或业务校验失败时
     * @throws BlacklistViolationException 变更来源命中黑名单时
     */
    @Transactional
    public SystemParameterPo update(
            String productId, String versionId, Integer parameterId, ParameterSaveRequest request) {
        if (request == null || request.getMain() == null) {
            throw new DomainRuleException("请求体或 main 不能为空");
        }
        SystemParameterPo existing = requireParameter(productId, versionId, parameterId);
        ParameterAssembler.toDomain(existing).assertWritable();
        SystemParameterPo main = request.getMain();
        main.setParameterId(parameterId);
        main.setOwnedProductId(productId);
        main.setOwnedVersionId(versionId);
        if (StringUtils.isBlank(main.getOwnedCommandId())) {
            main.setOwnedCommandId(existing.getOwnedCommandId());
        }
        ParameterDefaults.applyForCreate(main);
        validateAndApplyBlacklist(productId, main);
        ParameterSaveInvariant.assertSequenceMatchesCode(main.getParameterCode(), main.getParameterSequence());
        validateChangeDescriptions(false, request.getChangeDescriptions());
        List<SystemParameterPo> peers = loadParametersForCommand(productId, versionId, main.getOwnedCommandId());
        List<ParameterSaveInvariant.ParameterRowForBitCheck> bitRows = new ArrayList<>();
        for (SystemParameterPo p : peers) {
            if (parameterId.equals(p.getParameterId())) {
                continue;
            }
            bitRows.add(
                    new ParameterSaveInvariant.ParameterRowForBitCheck(
                            p.getParameterId(), p.getParameterCode(), p.getBitUsage()));
        }
        bitRows.add(
                new ParameterSaveInvariant.ParameterRowForBitCheck(
                        parameterId, main.getParameterCode(), main.getBitUsage()));
        ParameterSaveInvariant.assertBitDisjointAcrossVersionCommand(null, bitRows);
        LocalDateTime now = LocalDateTime.now();
        main.setCreationTimestamp(existing.getCreationTimestamp());
        main.setUpdateTimestamp(now);
        systemParameterMapper.updateById(main);
        deleteDescriptionsByParameter(parameterId);
        insertChangeDescriptions(parameterId, request.getChangeDescriptions(), now);
        String opM = StringUtils.defaultIfBlank(main.getUpdaterId(), "system");
        operationLogAppService.logSystemParameterUpdate(existing, main, opM);
        return main;
    }

    /**
     * 删除参数（版本维度）。
     *
     * @param productId   产品 ID
     * @param versionId   版本 ID
     * @param parameterId 参数 ID
     * @throws DomainRuleException 参数不存在、不在当前版本或已基线锁定时
     */
    @Transactional
    public void delete(String productId, String versionId, Integer parameterId) {
        delete(productId, versionId, parameterId, null);
    }

    /**
     * 删除参数（版本维度，可携带请求侧操作人）。
     *
     * @param productId       产品 ID
     * @param versionId       版本 ID
     * @param parameterId     参数 ID
     * @param requestOperator 请求中的操作人（可为 {@code null}）
     * @throws DomainRuleException 参数不存在、不在当前版本或已基线锁定时
     */
    @Transactional
    public void delete(String productId, String versionId, Integer parameterId, String requestOperator) {
        SystemParameterPo existing = requireParameter(productId, versionId, parameterId);
        ParameterAssembler.toDomain(existing).assertWritable();
        String opD = RequestOperatorIds.operationLogOperator(requestOperator, existing.getUpdaterId());
        operationLogAppService.logSystemParameterDelete(existing, opD);
        deleteDescriptionsByParameter(parameterId);
        systemParameterMapper.deleteById(parameterId);
    }

    /**
     * 将参数设为基线锁定状态。
     *
     * @param productId   产品 ID
     * @param versionId   版本 ID
     * @param parameterId 参数 ID
     * @throws DomainRuleException 参数不存在或不在当前版本下时
     */
    @Transactional
    public void baseline(String productId, String versionId, Integer parameterId) {
        SystemParameterPo existing = requireParameter(productId, versionId, parameterId);
        SystemParameterPo before = new SystemParameterPo();
        BeanUtils.copyProperties(existing, before);
        existing.setDataStatus(ParameterBaselinePolicy.STATUS_BASELINE_LOCKED);
        existing.setUpdateTimestamp(LocalDateTime.now());
        systemParameterMapper.updateById(existing);
        String opB = StringUtils.defaultIfBlank(existing.getUpdaterId(), "system");
        operationLogAppService.logSystemParameterUpdate(before, existing, opB);
    }

    /**
     * 解锁基线（将参数从“已基线”恢复为可写状态）。
     *
     * @param productId   产品 ID
     * @param versionId   版本 ID
     * @param parameterId 参数 ID
     * @throws DomainRuleException 参数不存在或不在当前版本下时
     */
    @Transactional
    public void unbaseline(String productId, String versionId, Integer parameterId) {
        SystemParameterPo existing = requireParameter(productId, versionId, parameterId);
        if (!ParameterBaselinePolicy.isBaselineLocked(existing.getDataStatus())) {
            return;
        }
        SystemParameterPo before = new SystemParameterPo();
        BeanUtils.copyProperties(existing, before);
        existing.setDataStatus("");
        existing.setUpdateTimestamp(LocalDateTime.now());
        systemParameterMapper.updateById(existing);
        String opU = StringUtils.defaultIfBlank(existing.getUpdaterId(), "system");
        operationLogAppService.logSystemParameterUpdate(before, existing, opU);
    }

    /**
     * 导出参数（XLSX）。
     *
     * @param productId       产品 ID
     * @param versionId       版本 ID
     * @param commandId       命令 ID（可选）
     * @param commandTypeCode 类型键（可选）
     * @return XLSX 字节
     */
    public byte[] export(String productId, String versionId, String commandId, String commandTypeCode) {
        LambdaQueryWrapper<SystemParameterPo> w =
                new LambdaQueryWrapper<SystemParameterPo>()
                        .eq(SystemParameterPo::getOwnedProductId, productId)
                        .eq(SystemParameterPo::getOwnedVersionId, versionId)
                        .orderByAsc(SystemParameterPo::getParameterId);
        if (StringUtils.isNotBlank(commandId)) {
            w.eq(SystemParameterPo::getOwnedCommandId, commandId);
        }
        if (StringUtils.isNotBlank(commandTypeCode)) {
            w.likeRight(SystemParameterPo::getParameterCode, commandTypeCode + "_");
        }
        List<SystemParameterPo> list = systemParameterMapper.selectList(w);
        List<String> headers = parameterExportHeadersZh();
        Map<String, String> commandNameById = loadCommandNameMap(productId, list);
        Map<Integer, ConfigChangeDescriptionPo> changeByPid = loadFirstChangeByParameterId(list);
        List<List<String>> rows = new ArrayList<>();
        for (SystemParameterPo po : list) {
            ConfigChangeDescriptionPo ch =
                    po.getParameterId() == null ? null : changeByPid.get(po.getParameterId());
            rows.add(buildParameterExportRow(po, commandNameById, ch));
        }
        return ExcelHelper.buildWorkbook("parameters", ExcelInstructions.PARAMETER_IMPORT_EXPORT_HINT, headers, rows);
    }

    /**
     * 获取参数导入模板（XLSX）。
     *
     * @return 模板 XLSX 字节
     */
    public byte[] importTemplate() {
        List<String> headers = parameterExportHeadersZh();
        return ExcelHelper.buildTemplate("parameters", ExcelInstructions.PARAMETER_IMPORT_EXPORT_HINT, headers);
    }

    /**
     * 参数导出/导入模板的中文表头（与页面列一致）。
     *
     * @return 表头列名列表
     */
    private static List<String> parameterExportHeadersZh() {
        return List.of(
                "参数ID",
                "参数名称（中）",
                "参数名称（英）",
                "归属命令",
                "参数编码",
                "序号",
                "取值范围",
                "BIT 占用",
                "取值说明（中）",
                "取值说明（英）",
                "应用场景（中）",
                "应用场景（英）",
                "参数默认值",
                "参数推荐值",
                "适用网元",
                "所属特性",
                "业务分类",
                "立即生效",
                "生效方式（中）",
                "生效方式（英）",
                "项目组",
                "归属模块",
                "变更来源",
                "版本号",
                "引入版本",
                "参数含义（中）",
                "参数含义（英）",
                "影响说明（中）",
                "影响说明（英）",
                "配置举例（中）",
                "配置举例（英）",
                "关联参数描述（中）",
                "关联参数描述（英）",
                "备注",
                "枚举值（中）",
                "枚举值（英）",
                "参数单位（中）",
                "参数单位（英）",
                "参数范围",
                "数据状态",
                "变更类型",
                "变更原因（中）",
                "变更影响（中）",
                "变更原因（英）",
                "变更影响（英）",
                "导出 delta",
                "不导出原因");
    }

    /**
     * 批量解析参数行中的命令 ID 为命令名称。
     *
     * @param productId 产品 ID
     * @param list      参数行
     * @return commandId → 命令名称
     */
    private Map<String, String> loadCommandNameMap(String productId, List<SystemParameterPo> list) {
        Set<String> ids = new HashSet<>();
        for (SystemParameterPo p : list) {
            String cid = StringUtils.defaultString(p.getOwnedCommandId()).trim();
            if (!cid.isEmpty()) {
                ids.add(cid);
            }
        }
        Map<String, String> out = new HashMap<>();
        if (ids.isEmpty()) {
            return out;
        }
        List<EntityCommandMappingPo> cmds =
                entityCommandMappingMapper.selectList(
                        new LambdaQueryWrapper<EntityCommandMappingPo>()
                                .eq(EntityCommandMappingPo::getOwnedProductId, productId)
                                .in(EntityCommandMappingPo::getCommandId, ids));
        for (EntityCommandMappingPo c : cmds) {
            if (c == null || StringUtils.isBlank(c.getCommandId())) {
                continue;
            }
            out.put(
                    c.getCommandId().trim(),
                    StringUtils.defaultIfBlank(c.getCommandName(), c.getCommandId()).trim());
        }
        return out;
    }

    /**
     * 构造单行参数导出单元格。
     *
     * @param po              参数主表行
     * @param commandNameById 命令 ID → 名称
     * @param ch              首条变更说明（可为 null）
     * @return 导出列值列表
     */
    private List<String> buildParameterExportRow(
            SystemParameterPo po, Map<String, String> commandNameById, ConfigChangeDescriptionPo ch) {
        String cmdId = StringUtils.defaultString(po.getOwnedCommandId()).trim();
        String cmdName = cmdId.isEmpty() ? "" : StringUtils.defaultIfBlank(commandNameById.get(cmdId), cmdId);
        String idCell = po.getParameterId() == null ? "" : String.valueOf(po.getParameterId());
        List<String> row = new ArrayList<>(parameterExportMainCells(po, cmdName, idCell));
        row.addAll(parameterExportChangeCells(ch));
        return row;
    }

    private List<String> parameterExportMainCells(SystemParameterPo po, String cmdName, String idCell) {
        return List.of(
                idCell, nz(po.getParameterNameCn()), nz(po.getParameterNameEn()), cmdName, nz(po.getParameterCode()),
                po.getParameterSequence() == null ? "" : String.valueOf(po.getParameterSequence()),
                nz(po.getValueRange()), nz(po.getBitUsage()), nz(po.getValueDescriptionCn()),
                nz(po.getValueDescriptionEn()), nz(po.getApplicationScenarioCn()), nz(po.getApplicationScenarioEn()),
                nz(po.getParameterDefaultValue()), nz(po.getParameterRecommendedValue()), nz(po.getApplicableNe()),
                nz(po.getFeature()), nz(po.getBusinessClassification()), nz(po.getTakeEffectImmediately()),
                nz(po.getEffectiveModeCn()), nz(po.getEffectiveModeEn()), nz(po.getProjectTeam()),
                nz(po.getBelongingModule()), nz(po.getChangeSource()), nz(po.getPatchVersion()),
                nz(po.getIntroducedVersion()), nz(po.getParameterDescriptionCn()), nz(po.getParameterDescriptionEn()),
                nz(po.getImpactDescriptionCn()), nz(po.getImpactDescriptionEn()), nz(po.getConfigurationExampleCn()),
                nz(po.getConfigurationExampleEn()), nz(po.getRelatedParameterDescriptionCn()),
                nz(po.getRelatedParameterDescriptionEn()), nz(po.getRemark()), nz(po.getEnumerationValuesCn()),
                nz(po.getEnumerationValuesEn()), nz(po.getParameterUnitCn()), nz(po.getParameterUnitEn()),
                nz(po.getParameterRange()), nz(po.getDataStatus()));
    }

    private static List<String> parameterExportChangeCells(ConfigChangeDescriptionPo ch) {
        if (ch == null) {
            return List.of("", "", "", "", "", "", "");
        }
        return List.of(
                nz(ch.getChangeType()), nz(ch.getChangeReasonCn()), nz(ch.getChangeImpactCn()),
                nz(ch.getChangeReasonEn()), nz(ch.getChangeImpactEn()), nz(ch.getExportDelta()),
                nz(ch.getNoExportReason()));
    }

    /**
     * 为导出填充「首条」变更说明：按更新时间倒序取一条。
     *
     * @param list 参数行列表
     * @return parameterId → 变更说明
     */
    private Map<Integer, ConfigChangeDescriptionPo> loadFirstChangeByParameterId(List<SystemParameterPo> list) {
        Map<Integer, ConfigChangeDescriptionPo> out = new HashMap<>();
        if (list == null || list.isEmpty()) {
            return out;
        }
        List<Integer> ids = new ArrayList<>();
        for (SystemParameterPo p : list) {
            if (p != null && p.getParameterId() != null) {
                ids.add(p.getParameterId());
            }
        }
        if (ids.isEmpty()) {
            return out;
        }
        List<ConfigChangeDescriptionPo> all =
                configChangeDescriptionMapper.selectList(
                        new LambdaQueryWrapper<ConfigChangeDescriptionPo>()
                                .in(ConfigChangeDescriptionPo::getParameterId, ids)
                                .orderByDesc(ConfigChangeDescriptionPo::getUpdateTimestamp)
                                .orderByDesc(ConfigChangeDescriptionPo::getChangeDescriptionId));
        for (ConfigChangeDescriptionPo d : all) {
            if (d == null || d.getParameterId() == null) {
                continue;
            }
            out.putIfAbsent(d.getParameterId(), d);
        }
        return out;
    }

    /**
     * 导入参数：按行落库，汇总成功/失败；与导出表头及新增表单主字段一致（含首条变更说明各列时可同步写入子表）。
     *
     * @param productId       产品 ID
     * @param versionId       版本 ID
     * @param mode            导入模式：FULL 或 INCREMENTAL
     * @param commandId       命令 ID（必填）
     * @param commandTypeCode 类型键（可选，用于缩小全量删除作用域）
     * @param fileBytes       Excel 文件字节
     * @return 导入结果
     * @throws DomainRuleException 文件无内容、表头缺失、mode 非法或 commandId 为空时
     */
    @Transactional
    public BatchImportResult importParameters(String productId, String versionId, String mode, String commandId,
            String commandTypeCode, byte[] fileBytes) {
        ExcelHelper.ParsedSheet sheet = ExcelHelper.parseFirstSheet(fileBytes);
        List<List<String>> rows = sheet.rows();
        if (rows.isEmpty()) {
            throw new DomainRuleException("文件无内容");
        }
        int headerIdx = ExcelHelper.detectHeaderRowIndex(rows);
        if (rows.size() <= headerIdx) {
            throw new DomainRuleException("表头缺失");
        }
        ImportSheetColumns cols = ImportSheetColumns.fromHeader(ExcelHelper.headerIndex(rows.get(headerIdx)));
        ImportResultCollector c = new ImportResultCollector();
        int dataRows = rows.size() - headerIdx - 1;
        String importMode = validateImportMode(mode);
        validateCommandId(commandId);
        applyFullImportIfNeeded(productId, versionId, commandId, commandTypeCode, importMode);
        List<SystemParameterPo> peers = loadParametersForCommand(productId, versionId, commandId);
        for (int i = headerIdx + 1; i < rows.size(); i++) {
            peers = handleImportParameterRow(productId, versionId, commandId, peers, rows.get(i), i + 1,
                    cols, cols.colCode, c);
        }
        return c.build(dataRows);
    }

    private static String validateImportMode(String mode) {
        String importMode = mode == null ? "" : mode.trim().toUpperCase();
        if (!"FULL".equals(importMode) && !"INCREMENTAL".equals(importMode)) {
            throw new DomainRuleException("mode 仅支持 FULL / INCREMENTAL");
        }
        return importMode;
    }

    private static void validateCommandId(String commandId) {
        if (StringUtils.isBlank(commandId)) {
            throw new DomainRuleException("commandId 必填");
        }
    }

    private void applyFullImportIfNeeded(String productId, String versionId, String commandId,
            String commandTypeCode, String importMode) {
        if (!"FULL".equals(importMode)) {
            return;
        }
        List<SystemParameterPo> scope = filterScopeByCommandTypePrefix(
                loadParametersForCommand(productId, versionId, commandId), commandTypeCode);
        purgeFullImportScope(scope);
    }

    /**
     * 按类型键前缀过滤导入全量作用域内的既有参数。
     *
     * @param scopeExisting   命令下既有参数
     * @param commandTypeCode 类型键（空则不过滤）
     * @return 过滤后的列表
     */
    private static List<SystemParameterPo> filterScopeByCommandTypePrefix(
            List<SystemParameterPo> scopeExisting, String commandTypeCode) {
        if (StringUtils.isBlank(commandTypeCode)) {
            return scopeExisting;
        }
        String prefix = commandTypeCode.trim() + "_";
        return scopeExisting.stream()
                .filter(p -> StringUtils.defaultString(p.getParameterCode()).startsWith(prefix))
                .toList();
    }

    /**
     * 全量导入前删除作用域内既有参数及变更说明。
     *
     * @param scopeExisting 待清理参数列表
     */
    private void purgeFullImportScope(List<SystemParameterPo> scopeExisting) {
        for (SystemParameterPo p : scopeExisting) {
            Integer pid = p.getParameterId();
            if (pid == null) {
                continue;
            }
            String opD = StringUtils.defaultIfBlank(p.getUpdaterId(), "system");
            operationLogAppService.logSystemParameterDelete(p, opD);
            deleteDescriptionsByParameter(pid);
            systemParameterMapper.deleteById(pid);
        }
    }

    /**
     * 处理导入文件中的一行参数数据。
     *
     * @param productId        产品 ID
     * @param versionId        版本 ID
     * @param commandId        命令 ID
     * @param peersForBitCheck 当前命令下用于 BIT 校验的同行参数
     * @param line             当前行单元格
     * @param dataRowNumber    Excel 行号（用于结果反馈）
     * @param cols             列映射
     * @param colCode          参数编码列索引
     * @param c                导入结果收集器
     * @return 更新后的同行参数列表
     */
    private List<SystemParameterPo> handleImportParameterRow(
            String productId,
            String versionId,
            String commandId,
            List<SystemParameterPo> peersForBitCheck,
            List<String> line,
            int dataRowNumber,
            ImportSheetColumns cols,
            int colCode,
            ImportResultCollector c) {
        String code = cell(line, colCode);
        if (StringUtils.isBlank(code)) {
            c.failure(dataRowNumber, "parameter_code 为空");
            return peersForBitCheck;
        }
        try {
            return applyImportRowWithMatchedDecision(
                    productId,
                    versionId,
                    commandId,
                    peersForBitCheck,
                    line,
                    dataRowNumber,
                    cols,
                    code,
                    c);
        } catch (BlacklistViolationException e) {
            c.failure(dataRowNumber, e.getMessage());
            return peersForBitCheck;
        } catch (DomainRuleException e) {
            c.failure(dataRowNumber, e.getMessage());
            return peersForBitCheck;
        }
    }

    /**
     * 根据是否匹配既有参数决定新建或更新导入行。
     *
     * @param productId        产品 ID
     * @param versionId        版本 ID
     * @param commandId        命令 ID
     * @param peersForBitCheck 同行参数（BIT 校验）
     * @param line             当前行
     * @param dataRowNumber    行号
     * @param cols             列映射
     * @param code             参数编码
     * @param c                结果收集器
     * @return 更新后的同行参数列表
     */
    private List<SystemParameterPo> applyImportRowWithMatchedDecision(
            String productId,
            String versionId,
            String commandId,
            List<SystemParameterPo> peersForBitCheck,
            List<String> line,
            int dataRowNumber,
            ImportSheetColumns cols,
            String code,
            ImportResultCollector c) {
        SystemParameterPo fromSheet = new SystemParameterPo();
        cols.applyMainFromLine(productId, versionId, commandId, code, fromSheet, line);
        SystemParameterPo matched = findImportMatch(peersForBitCheck, fromSheet);
        if (matched != null && ParameterBaselinePolicy.isBaselineLocked(matched.getDataStatus())) {
            c.failure(dataRowNumber, "已基线参数不会做更改，已跳过");
            return peersForBitCheck;
        }
        if (matched == null) {
            return importParameterRowCreate(
                    productId,
                    versionId,
                    commandId,
                    peersForBitCheck,
                    line,
                    dataRowNumber,
                    cols,
                    fromSheet,
                    c);
        }
        return importParameterRowUpdate(
                productId, versionId, commandId, line, dataRowNumber, cols, matched, code, c);
    }

    /**
     * 导入场景新建参数行。
     *
     * @param productId        产品 ID
     * @param versionId        版本 ID
     * @param commandId        命令 ID
     * @param peersForBitCheck 同行参数
     * @param line             当前行
     * @param dataRowNumber    行号
     * @param cols             列映射
     * @param incoming         从表解析的主表对象
     * @param c                结果收集器
     * @return 刷新后的同行参数列表
     * @throws DomainRuleException       业务校验失败时
     * @throws BlacklistViolationException 黑名单命中时
     */
    private List<SystemParameterPo> importParameterRowCreate(
            String productId,
            String versionId,
            String commandId,
            List<SystemParameterPo> peersForBitCheck,
            List<String> line,
            int dataRowNumber,
            ImportSheetColumns cols,
            SystemParameterPo incoming,
            ImportResultCollector c) {
        ParameterDefaults.applyForCreate(incoming);
        applyOptionalString(line, cols.colDataStatus, incoming::setDataStatus);
        validateAndApplyBlacklist(productId, incoming);
        ParameterSaveInvariant.assertSequenceMatchesCode(
                incoming.getParameterCode(), incoming.getParameterSequence());
        List<ParameterSaveInvariant.ParameterRowForBitCheck> bitRows = toBitRows(peersForBitCheck);
        bitRows.add(
                new ParameterSaveInvariant.ParameterRowForBitCheck(
                        null, incoming.getParameterCode(), incoming.getBitUsage()));
        ParameterSaveInvariant.assertBitDisjointAcrossVersionCommand(null, bitRows);
        LocalDateTime now = LocalDateTime.now();
        incoming.setCreationTimestamp(now);
        incoming.setUpdateTimestamp(now);
        systemParameterMapper.insert(incoming);
        String who = StringUtils.defaultIfBlank(incoming.getCreatorId(), "system");
        operationLogAppService.logSystemParameterCreate(incoming, who);
        importReplaceChangeDescriptionIfPresent(true, incoming.getParameterId(), cols, line, now);
        c.success(dataRowNumber);
        return loadParametersForCommand(productId, versionId, commandId);
    }

    /**
     * 导入场景更新已匹配参数行。
     *
     * @param productId     产品 ID
     * @param versionId     版本 ID
     * @param commandId     命令 ID
     * @param line          当前行
     * @param dataRowNumber 行号
     * @param cols          列映射
     * @param matched       匹配到的既有参数
     * @param code          参数编码
     * @param c             结果收集器
     * @return 刷新后的同行参数列表
     * @throws DomainRuleException       业务校验失败时
     * @throws BlacklistViolationException 黑名单命中时
     */
    private List<SystemParameterPo> importParameterRowUpdate(
            String productId,
            String versionId,
            String commandId,
            List<String> line,
            int dataRowNumber,
            ImportSheetColumns cols,
            SystemParameterPo matched,
            String code,
            ImportResultCollector c) {
        SystemParameterPo incoming = new SystemParameterPo();
        BeanUtils.copyProperties(matched, incoming);
        cols.applyMainFromLine(productId, versionId, commandId, code, incoming, line);
        applyOptionalString(line, cols.colDataStatus, incoming::setDataStatus);
        validateAndApplyBlacklist(productId, incoming);
        ParameterSaveInvariant.assertSequenceMatchesCode(
                incoming.getParameterCode(), incoming.getParameterSequence());
        SystemParameterPo before = new SystemParameterPo();
        BeanUtils.copyProperties(matched, before);
        incoming.setParameterId(matched.getParameterId());
        incoming.setCreationTimestamp(matched.getCreationTimestamp());
        LocalDateTime now = LocalDateTime.now();
        incoming.setUpdateTimestamp(now);
        List<SystemParameterPo> peers = loadParametersForCommand(productId, versionId, commandId);
        List<ParameterSaveInvariant.ParameterRowForBitCheck> bitRows =
                bitRowsForUpdateExcludingSelf(peers, incoming);
        bitRows.add(
                new ParameterSaveInvariant.ParameterRowForBitCheck(
                        incoming.getParameterId(), incoming.getParameterCode(), incoming.getBitUsage()));
        ParameterSaveInvariant.assertBitDisjointAcrossVersionCommand(null, bitRows);
        systemParameterMapper.updateById(incoming);
        importReplaceChangeDescriptionIfPresent(false, incoming.getParameterId(), cols, line, now);
        String opU = StringUtils.defaultIfBlank(incoming.getUpdaterId(), "system");
        operationLogAppService.logSystemParameterUpdate(before, incoming, opU);
        c.success(dataRowNumber);
        return loadParametersForCommand(productId, versionId, commandId);
    }

    /**
     * 更新导入时构造 BIT 校验行（排除自身）。
     *
     * @param peers    命令下全部参数
     * @param incoming 待更新参数
     * @return BIT 校验行列表
     */
    private static List<ParameterSaveInvariant.ParameterRowForBitCheck> bitRowsForUpdateExcludingSelf(
            List<SystemParameterPo> peers, SystemParameterPo incoming) {
        List<ParameterSaveInvariant.ParameterRowForBitCheck> bitRows = new ArrayList<>();
        for (SystemParameterPo p : peers) {
            if (incoming.getParameterId().equals(p.getParameterId())) {
                continue;
            }
            bitRows.add(
                    new ParameterSaveInvariant.ParameterRowForBitCheck(
                            p.getParameterId(), p.getParameterCode(), p.getBitUsage()));
        }
        return bitRows;
    }

    /**
     * 导入行若含变更说明列则替换该参数下的变更说明子表。
     *
     * @param isNewParameter 是否为新建参数
     * @param parameterId    参数 ID
     * @param cols           列映射
     * @param line           当前行
     * @param now            写入时间戳
     * @throws DomainRuleException 变更说明校验失败时
     */
    private void importReplaceChangeDescriptionIfPresent(
            boolean isNewParameter,
            Integer parameterId,
            ImportSheetColumns cols,
            List<String> line,
            LocalDateTime now) {
        if (parameterId == null || cols == null || !cols.hasChangePayload(line)) {
            return;
        }
        ConfigChangeDescriptionPo d = new ConfigChangeDescriptionPo();
        d.setChangeType(trimCell(line, cols.colChType));
        d.setChangeReasonCn(trimCell(line, cols.colChReasonCn));
        d.setChangeImpactCn(trimCell(line, cols.colChImpactCn));
        d.setChangeReasonEn(trimCell(line, cols.colChReasonEn));
        d.setChangeImpactEn(trimCell(line, cols.colChImpactEn));
        d.setExportDelta(trimCell(line, cols.colExportDelta));
        d.setNoExportReason(trimCell(line, cols.colNoExportReason));
        validateChangeDescriptions(isNewParameter, List.of(d));
        deleteDescriptionsByParameter(parameterId);
        insertChangeDescriptions(parameterId, List.of(d), now);
    }

    /**
     * 读取并 trim 指定列单元格。
     *
     * @param line 行数据
     * @param col  列索引（负表示空串）
     * @return 单元格文本
     */
    private static String trimCell(List<String> line, int col) {
        if (col < 0) {
            return "";
        }
        return StringUtils.defaultString(cell(line, col)).trim();
    }

    /**
     * 与导出表头/字段映射一致，供导入解析主表与变更说明子表列。
     */
    private static final class ImportSheetColumns {
        private final int colCode;
        private final int colName;
        private final int colNameEn;
        private final int colSeq;
        private final int colBit;
        private final int colCs;
        private final int colValueRange;
        private final int colValueDescCn;
        private final int colValueDescEn;
        private final int colSceneCn;
        private final int colSceneEn;
        private final int colDef;
        private final int colRec;
        private final int colNe;
        private final int colFeature;
        private final int colBiz;
        private final int colImmediate;
        private final int colEmCn;
        private final int colEmEn;
        private final int colTeam;
        private final int colModule;
        private final int colPatch;
        private final int colIntroVer;
        private final int colDescCn;
        private final int colDescEn;
        private final int colImpactCn;
        private final int colImpactEn;
        private final int colExCn;
        private final int colExEn;
        private final int colRelCn;
        private final int colRelEn;
        private final int colRemark;
        private final int colEnumCn;
        private final int colEnumEn;
        private final int colUnitCn;
        private final int colUnitEn;
        private final int colPr;
        private final int colDataStatus;
        private final int colChType;
        private final int colChReasonCn;
        private final int colChImpactCn;
        private final int colChReasonEn;
        private final int colChImpactEn;
        private final int colExportDelta;
        private final int colNoExportReason;

        private ImportSheetColumns(
                int colCode,
                int colName,
                int colNameEn,
                int colSeq,
                int colBit,
                int colCs,
                int colValueRange,
                int colValueDescCn,
                int colValueDescEn,
                int colSceneCn,
                int colSceneEn,
                int colDef,
                int colRec,
                int colNe,
                int colFeature,
                int colBiz,
                int colImmediate,
                int colEmCn,
                int colEmEn,
                int colTeam,
                int colModule,
                int colPatch,
                int colIntroVer,
                int colDescCn,
                int colDescEn,
                int colImpactCn,
                int colImpactEn,
                int colExCn,
                int colExEn,
                int colRelCn,
                int colRelEn,
                int colRemark,
                int colEnumCn,
                int colEnumEn,
                int colUnitCn,
                int colUnitEn,
                int colPr,
                int colDataStatus,
                int colChType,
                int colChReasonCn,
                int colChImpactCn,
                int colChReasonEn,
                int colChImpactEn,
                int colExportDelta,
                int colNoExportReason) {
            this.colCode = colCode;
            this.colName = colName;
            this.colNameEn = colNameEn;
            this.colSeq = colSeq;
            this.colBit = colBit;
            this.colCs = colCs;
            this.colValueRange = colValueRange;
            this.colValueDescCn = colValueDescCn;
            this.colValueDescEn = colValueDescEn;
            this.colSceneCn = colSceneCn;
            this.colSceneEn = colSceneEn;
            this.colDef = colDef;
            this.colRec = colRec;
            this.colNe = colNe;
            this.colFeature = colFeature;
            this.colBiz = colBiz;
            this.colImmediate = colImmediate;
            this.colEmCn = colEmCn;
            this.colEmEn = colEmEn;
            this.colTeam = colTeam;
            this.colModule = colModule;
            this.colPatch = colPatch;
            this.colIntroVer = colIntroVer;
            this.colDescCn = colDescCn;
            this.colDescEn = colDescEn;
            this.colImpactCn = colImpactCn;
            this.colImpactEn = colImpactEn;
            this.colExCn = colExCn;
            this.colExEn = colExEn;
            this.colRelCn = colRelCn;
            this.colRelEn = colRelEn;
            this.colRemark = colRemark;
            this.colEnumCn = colEnumCn;
            this.colEnumEn = colEnumEn;
            this.colUnitCn = colUnitCn;
            this.colUnitEn = colUnitEn;
            this.colPr = colPr;
            this.colDataStatus = colDataStatus;
            this.colChType = colChType;
            this.colChReasonCn = colChReasonCn;
            this.colChImpactCn = colChImpactCn;
            this.colChReasonEn = colChReasonEn;
            this.colChImpactEn = colChImpactEn;
            this.colExportDelta = colExportDelta;
            this.colNoExportReason = colNoExportReason;
        }

        /**
         * 根据表头映射构造列索引。
         *
         * @param hi 表头名 → 列索引
         * @return 列映射对象
         */
        private static ImportSheetColumns fromHeader(Map<String, Integer> hi) {
            return new ImportSheetColumns(
                    findColumn(hi, "parameter_code", "参数编码"),
                    findColumn(hi, "parameter_name_cn", "参数名称（中）", "参数名称"),
                    findColumn(hi, "parameter_name_en", "参数名称（英）"),
                    findColumn(hi, "parameter_sequence", "序号"),
                    findColumn(hi, "bit_usage", "BIT 占用"),
                    findColumn(hi, "change_source", "变更来源"),
                    findColumn(hi, "value_range", "取值范围"),
                    findColumn(hi, "value_description_cn", "取值说明（中）"),
                    findColumn(hi, "value_description_en", "取值说明（英）"),
                    findColumn(hi, "application_scenario_cn", "应用场景（中）"),
                    findColumn(hi, "application_scenario_en", "应用场景（英）"),
                    findColumn(hi, "parameter_default_value", "参数默认值"),
                    findColumn(hi, "parameter_recommended_value", "参数推荐值"),
                    findColumn(hi, "applicable_ne", "适用网元"),
                    findColumn(hi, "feature", "所属特性"),
                    findColumn(hi, "business_classification", "业务分类"),
                    findColumn(hi, "take_effect_immediately", "立即生效"),
                    findColumn(hi, "effective_mode_cn", "生效方式（中）"),
                    findColumn(hi, "effective_mode_en", "生效方式（英）"),
                    findColumn(hi, "project_team", "项目组"),
                    findColumn(hi, "belonging_module", "归属模块"),
                    findColumn(hi, "patch_version", "版本号"),
                    findColumn(hi, "introduced_version", "引入版本"),
                    findColumn(hi, "parameter_description_cn", "参数含义（中）"),
                    findColumn(hi, "parameter_description_en", "参数含义（英）"),
                    findColumn(hi, "impact_description_cn", "影响说明（中）"),
                    findColumn(hi, "impact_description_en", "影响说明（英）"),
                    findColumn(hi, "configuration_example_cn", "配置举例（中）"),
                    findColumn(hi, "configuration_example_en", "配置举例（英）"),
                    findColumn(hi, "related_parameter_description_cn", "关联参数描述（中）"),
                    findColumn(hi, "related_parameter_description_en", "关联参数描述（英）"),
                    findColumn(hi, "remark", "备注"),
                    findColumn(hi, "enumeration_values_cn", "枚举值（中）"),
                    findColumn(hi, "enumeration_values_en", "枚举值（英）"),
                    findColumn(hi, "parameter_unit_cn", "参数单位（中）"),
                    findColumn(hi, "parameter_unit_en", "参数单位（英）"),
                    findColumn(hi, "parameter_range", "参数范围"),
                    findColumn(hi, "data_status", "数据状态"),
                    findColumn(hi, "变更类型"),
                    findColumn(hi, "变更原因（中）"),
                    findColumn(hi, "变更影响（中）"),
                    findColumn(hi, "变更原因（英）"),
                    findColumn(hi, "变更影响（英）"),
                    findColumn(hi, "export_delta", "导出 delta", "导出delta"),
                    findColumn(hi, "不导出原因", "no_export_reason"));
        }

        /**
         * 判断当前行是否包含任一变更说明列内容。
         *
         * @param line 当前行
         * @return 是否含变更说明载荷
         */
        private boolean hasChangePayload(List<String> line) {
            return StringUtils.isNotBlank(trimCell(line, colChType))
                    || StringUtils.isNotBlank(trimCell(line, colChReasonCn))
                    || StringUtils.isNotBlank(trimCell(line, colChImpactCn))
                    || StringUtils.isNotBlank(trimCell(line, colChReasonEn))
                    || StringUtils.isNotBlank(trimCell(line, colChImpactEn))
                    || StringUtils.isNotBlank(trimCell(line, colExportDelta))
                    || StringUtils.isNotBlank(trimCell(line, colNoExportReason));
        }

        /**
         * 从导入行填充参数主表字段。
         *
         * @param productId     产品 ID
         * @param versionId     版本 ID
         * @param commandId     命令 ID
         * @param parameterCode 参数编码
         * @param target        待填充主表对象
         * @param line          当前行
         * @throws DomainRuleException parameter_sequence 非法时
         */
        private void applyMainFromLine(String productId, String versionId, String commandId, String parameterCode,
                SystemParameterPo target, List<String> line) {
            applyMainKeysFromLine(productId, versionId, commandId, parameterCode, target, line);
            applyMainOptionalFieldsFromLine(target, line);
        }

        private void applyMainKeysFromLine(String productId, String versionId, String commandId, String parameterCode,
                SystemParameterPo target, List<String> line) {
            target.setOwnedProductId(productId);
            target.setOwnedVersionId(versionId);
            target.setOwnedCommandId(commandId);
            target.setParameterCode(parameterCode);
            applyOptionalString(line, colName, target::setParameterNameCn);
            applyOptionalString(line, colNameEn, target::setParameterNameEn);
            if (colSeq >= 0) {
                String s = cell(line, colSeq);
                if (StringUtils.isNotBlank(s)) {
                    try {
                        target.setParameterSequence(Integer.parseInt(s.trim()));
                    } catch (NumberFormatException e) {
                        throw new DomainRuleException("parameter_sequence 须为整数");
                    }
                }
            }
            if (target.getParameterSequence() == null) {
                target.setParameterSequence(ParameterCode.parse(parameterCode).sequence());
            }
        }

        private void applyMainOptionalFieldsFromLine(SystemParameterPo target, List<String> line) {
            applyOptionalString(line, colBit, target::setBitUsage);
            applyOptionalString(line, colCs, target::setChangeSource);
            applyOptionalString(line, colValueRange, target::setValueRange);
            applyOptionalString(line, colValueDescCn, target::setValueDescriptionCn);
            applyOptionalString(line, colValueDescEn, target::setValueDescriptionEn);
            applyOptionalString(line, colSceneCn, target::setApplicationScenarioCn);
            applyOptionalString(line, colSceneEn, target::setApplicationScenarioEn);
            applyOptionalString(line, colDef, target::setParameterDefaultValue);
            applyOptionalString(line, colRec, target::setParameterRecommendedValue);
            applyOptionalString(line, colNe, target::setApplicableNe);
            applyOptionalString(line, colFeature, target::setFeature);
            applyOptionalString(line, colBiz, target::setBusinessClassification);
            applyOptionalString(line, colImmediate, target::setTakeEffectImmediately);
            applyOptionalString(line, colEmCn, target::setEffectiveModeCn);
            applyOptionalString(line, colEmEn, target::setEffectiveModeEn);
            applyOptionalString(line, colTeam, target::setProjectTeam);
            applyOptionalString(line, colModule, target::setBelongingModule);
            applyOptionalString(line, colPatch, target::setPatchVersion);
            applyOptionalString(line, colIntroVer, target::setIntroducedVersion);
            applyOptionalString(line, colDescCn, target::setParameterDescriptionCn);
            applyOptionalString(line, colDescEn, target::setParameterDescriptionEn);
            applyOptionalString(line, colImpactCn, target::setImpactDescriptionCn);
            applyOptionalString(line, colImpactEn, target::setImpactDescriptionEn);
            applyOptionalString(line, colExCn, target::setConfigurationExampleCn);
            applyOptionalString(line, colExEn, target::setConfigurationExampleEn);
            applyOptionalString(line, colRelCn, target::setRelatedParameterDescriptionCn);
            applyOptionalString(line, colRelEn, target::setRelatedParameterDescriptionEn);
            applyOptionalString(line, colRemark, target::setRemark);
            applyOptionalString(line, colEnumCn, target::setEnumerationValuesCn);
            applyOptionalString(line, colEnumEn, target::setEnumerationValuesEn);
            applyOptionalString(line, colUnitCn, target::setParameterUnitCn);
            applyOptionalString(line, colUnitEn, target::setParameterUnitEn);
            applyOptionalString(line, colPr, target::setParameterRange);
        }
    }

    /**
     * 若列存在且非空则写入字符串字段。
     *
     * @param line   行数据
     * @param col    列索引
     * @param setter 字段 setter
     */
    private static void applyOptionalString(List<String> line, int col, Consumer<String> setter) {
        if (col < 0) {
            return;
        }
        String v = cell(line, col);
        if (StringUtils.isBlank(v)) {
            return;
        }
        setter.accept(v);
    }

    /**
     * 在同行参数中按编码（及 bit_usage）匹配导入目标行。
     *
     * @param peers    命令下既有参数
     * @param incoming 导入解析出的参数
     * @return 匹配到的参数，无匹配时 null
     */
    private static SystemParameterPo findImportMatch(List<SystemParameterPo> peers, SystemParameterPo incoming) {
        String code = StringUtils.defaultString(incoming.getParameterCode()).trim();
        if (code.isEmpty()) {
            return null;
        }
        List<SystemParameterPo> sameCode = new ArrayList<>();
        for (SystemParameterPo p : peers) {
            if (code.equals(StringUtils.defaultString(p.getParameterCode()).trim())) {
                sameCode.add(p);
            }
        }
        if (sameCode.isEmpty()) {
            return null;
        }
        if (sameCode.size() == 1) {
            return sameCode.get(0);
        }
        String wantBits = StringUtils.defaultString(incoming.getBitUsage());
        for (SystemParameterPo p : sameCode) {
            if (StringUtils.equals(StringUtils.defaultString(p.getBitUsage()), wantBits)) {
                return p;
            }
        }
        return sameCode.get(0);
    }

    /**
     * 在表头索引中按候选列名查找列号。
     *
     * @param hi    表头索引
     * @param names 候选列名（按优先级）
     * @return 列索引，未找到为 -1
     */
    private static int findColumn(Map<String, Integer> hi, String... names) {
        for (String n : names) {
            Integer ix = hi.get(n);
            if (ix != null) {
                return ix;
            }
        }
        return -1;
    }

    /**
     * 读取指定列单元格原文（不 trim）。
     *
     * @param line 行数据
     * @param col  列索引
     * @return 单元格内容，越界或负索引时为空串
     */
    private static String cell(List<String> line, int col) {
        if (col < 0 || col >= line.size()) {
            return "";
        }
        return line.get(col);
    }

    /**
     * null 安全转空串。
     *
     * @param s 原字符串
     * @return 非 null 字符串
     */
    private static String nz(String s) {
        return s == null ? "" : s;
    }

    /**
     * 校验变更说明类型与必填中英字段。
     *
     * @param isCreate      是否为新建参数
     * @param descriptions  变更说明列表
     * @throws DomainRuleException 校验不通过时
     */
    private void validateChangeDescriptions(boolean isCreate, List<ConfigChangeDescriptionPo> descriptions) {
        List<ConfigChangeDescriptionPo> rows = descriptions == null ? List.of() : descriptions;
        List<String> typeNames =
                rows.stream()
                        .map(ConfigChangeDescriptionPo::getChangeType)
                        .map(s -> s == null ? "" : s.trim())
                        .toList();
        configChangeTypeAppService.validateChangeTypesForParameterSave(isCreate, typeNames);
        for (ConfigChangeDescriptionPo d : rows) {
            if ("否".equals(d.getExportDelta()) && StringUtils.isBlank(d.getNoExportReason())) {
                throw new DomainRuleException("export_delta 为「否」时 no_export_reason 必填");
            }
            if (StringUtils.isBlank(d.getChangeReasonCn())
                    || StringUtils.isBlank(d.getChangeImpactCn())
                    || StringUtils.isBlank(d.getChangeReasonEn())
                    || StringUtils.isBlank(d.getChangeImpactEn())) {
                throw new DomainRuleException("变更说明中英四格均需填写");
            }
        }
    }

    /**
     * 校验变更来源黑名单；通过时将空值规范为空串。
     *
     * @param productId 产品 ID
     * @param main        参数主表
     * @throws BlacklistViolationException 命中黑名单时
     */
    private void validateAndApplyBlacklist(String productId, SystemParameterPo main) {
        String cs = main.getChangeSource();
        if (cs == null || cs.isBlank()) {
            main.setChangeSource("");
            return;
        }
        List<String> regexes = changeSourceKeywordRepository.listEnabledRegexesByProduct(productId);
        Optional<String> hit = ChangeSourceBlacklistPolicy.findFirstViolation(cs, regexes);
        if (hit.isPresent()) {
            String r = hit.get();
            throw new BlacklistViolationException("PARAM_CHANGE_SOURCE_FORBIDDEN: 命中黑名单 " + r, r);
        }
    }

    /**
     * 加载指定产品、版本、命令下的全部参数。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @param commandId 命令 ID
     * @return 参数列表
     * @throws DomainRuleException commandId 为空时
     */
    private List<SystemParameterPo> loadParametersForCommand(
            String productId, String versionId, String commandId) {
        if (StringUtils.isBlank(commandId)) {
            throw new DomainRuleException("owned_command_id 不能为空");
        }
        return systemParameterMapper.selectList(
                new LambdaQueryWrapper<SystemParameterPo>()
                        .eq(SystemParameterPo::getOwnedProductId, productId)
                        .eq(SystemParameterPo::getOwnedVersionId, versionId)
                        .eq(SystemParameterPo::getOwnedCommandId, commandId));
    }

    /**
     * 将参数主表行转为 BIT 不相交校验行。
     *
     * @param list 参数列表
     * @return BIT 校验行列表
     */
    private List<ParameterSaveInvariant.ParameterRowForBitCheck> toBitRows(List<SystemParameterPo> list) {
        List<ParameterSaveInvariant.ParameterRowForBitCheck> rows = new ArrayList<>();
        for (SystemParameterPo p : list) {
            rows.add(
                    new ParameterSaveInvariant.ParameterRowForBitCheck(
                            p.getParameterId(), p.getParameterCode(), p.getBitUsage()));
        }
        return rows;
    }

    /**
     * 批量插入变更说明子表行。
     *
     * @param parameterId   参数 ID
     * @param descriptions  变更说明列表（可为 null）
     * @param now           写入时间戳
     */
    private void insertChangeDescriptions(
            Integer parameterId, List<ConfigChangeDescriptionPo> descriptions, LocalDateTime now) {
        if (descriptions == null) {
            return;
        }
        for (ConfigChangeDescriptionPo d : descriptions) {
            if (StringUtils.isBlank(d.getChangeDescriptionId())) {
                d.setChangeDescriptionId(IdGenerator.changeDescriptionId());
            }
            d.setParameterId(parameterId);
            d.setUpdateTimestamp(now);
            configChangeDescriptionMapper.insert(d);
        }
    }

    /**
     * 按参数 ID 删除全部变更说明。
     *
     * @param parameterId 参数 ID
     */
    private void deleteDescriptionsByParameter(Integer parameterId) {
        configChangeDescriptionMapper.delete(
                new LambdaQueryWrapper<ConfigChangeDescriptionPo>()
                        .eq(ConfigChangeDescriptionPo::getParameterId, parameterId));
    }

    /**
     * 加载并校验参数属于指定产品版本。
     *
     * @param productId   产品 ID
     * @param versionId   版本 ID
     * @param parameterId 参数 ID
     * @return 参数主表行
     * @throws DomainRuleException 不存在或不在当前产品版本下时
     */
    private SystemParameterPo requireParameter(String productId, String versionId, Integer parameterId) {
        SystemParameterPo po = systemParameterMapper.selectById(parameterId);
        if (po == null
                || !productId.equals(po.getOwnedProductId())
                || !versionId.equals(po.getOwnedVersionId())) {
            throw new DomainRuleException("参数不存在或不在当前产品版本下");
        }
        return po;
    }

    /**
     * 解析类型在指定版本下的可用序号区间；无区段定义时返回默认 1～32。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @param commandId 命令 ID
     * @param typeKey   类型键
     * @return 长度为 2 的数组：{@code [min, max]}
     * @throws DomainRuleException 版本不存在时
     */
    private int[] resolveSequenceRange(
            String productId, String versionId, String commandId, String typeKey) {
        EntityVersionInfoPo ver = entityVersionInfoMapper.selectById(versionId);
        if (ver == null || !productId.equals(ver.getOwnedProductId())) {
            throw new DomainRuleException("版本不存在");
        }
        if (StringUtils.isBlank(commandId)) {
            return new int[] {FALLBACK_RANGE_MIN, FALLBACK_RANGE_MAX};
        }
        CommandTypeDefinitionPo def = findTypeDefinitionForAllocation(productId, commandId, typeKey);
        if (def == null) {
            return new int[] {FALLBACK_RANGE_MIN, FALLBACK_RANGE_MAX};
        }
        List<CommandTypeVersionRangePo> ranges =
                commandTypeVersionRangeMapper.selectList(
                        new LambdaQueryWrapper<CommandTypeVersionRangePo>()
                                .eq(CommandTypeVersionRangePo::getOwnedProductId, productId)
                                .eq(CommandTypeVersionRangePo::getOwnedCommandId, commandId)
                                .eq(CommandTypeVersionRangePo::getOwnedTypeId, def.getCommandTypeId())
                                .eq(CommandTypeVersionRangePo::getOwnedVersionOrBusinessId, versionId)
                                .eq(CommandTypeVersionRangePo::getRangeStatus, 1));
        if (ranges.isEmpty()) {
            return new int[] {FALLBACK_RANGE_MIN, FALLBACK_RANGE_MAX};
        }
        CommandTypeVersionRangePo r = ranges.get(0);
        int min = r.getStartIndex() != null ? r.getStartIndex() : FALLBACK_RANGE_MIN;
        int max = r.getEndIndex() != null ? r.getEndIndex() : FALLBACK_RANGE_MAX;
        if (min > max) {
            return new int[] {FALLBACK_RANGE_MIN, FALLBACK_RANGE_MAX};
        }
        return new int[] {min, max};
    }

    /**
     * 按类型键查找类型定义；typeKey 可为 command_type_id 或 command_type 枚举。
     *
     * @param productId 产品 ID
     * @param commandId 命令 ID
     * @param typeKey   类型键
     * @return 类型定义 PO，任一主键为空或未找到时 null
     */
    private CommandTypeDefinitionPo findTypeDefinitionForAllocation(
            String productId, String commandId, String typeKey) {
        if (StringUtils.isAnyBlank(productId, commandId, typeKey)) {
            return null;
        }
        return commandTypeDefinitionMapper.selectOne(
                new LambdaQueryWrapper<CommandTypeDefinitionPo>()
                        .eq(CommandTypeDefinitionPo::getOwnedProductId, productId)
                        .eq(CommandTypeDefinitionPo::getOwnedCommandId, commandId)
                        .and(
                                w ->
                                        w.eq(CommandTypeDefinitionPo::getCommandTypeId, typeKey.trim())
                                                .or()
                                                .eq(CommandTypeDefinitionPo::getCommandType, typeKey.trim())));
    }

    /**
     * 将 command_type_id 或枚举解析为 {@code command_type}，供位宽等规则使用。
     *
     * @param productId 产品 ID
     * @param commandId 命令 ID
     * @param typeKey   类型键
     * @return 类型枚举字符串
     */
    private String resolveTypeEnumForAllocation(String productId, String commandId, String typeKey) {
        CommandTypeDefinitionPo def = findTypeDefinitionForAllocation(productId, commandId, typeKey);
        if (def != null && StringUtils.isNotBlank(def.getCommandType())) {
            return def.getCommandType();
        }
        return typeKey;
    }
}
