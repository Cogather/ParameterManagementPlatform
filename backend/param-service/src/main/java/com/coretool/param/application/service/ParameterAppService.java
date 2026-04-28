package com.coretool.param.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coretool.param.application.support.ImportResultCollector;
import com.coretool.param.application.support.ParameterDefaults;
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

    /** 全产品维度：不区分版本（版本下拉 ALL 视图） */
    /**
     * 统计产品维度已基线参数数量（不区分版本）。
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

    /** 与 spec-03 一致：优先 commandTypeId，否则 commandTypeCode；可全空。 */
    private static String optionalCommandTypeKey(String commandTypeId, String commandTypeCode) {
        if (StringUtils.isNotBlank(commandTypeId)) {
            return commandTypeId.trim();
        }
        return commandTypeCode == null ? null : commandTypeCode.trim();
    }

    /** 全产品分页：同一产品下全部版本的参数 */
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
     */
    @Transactional
    public void delete(String productId, String versionId, Integer parameterId) {
        SystemParameterPo existing = requireParameter(productId, versionId, parameterId);
        ParameterAssembler.toDomain(existing).assertWritable();
        String opD = StringUtils.defaultIfBlank(existing.getUpdaterId(), "system");
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
        return ExcelHelper.buildWorkbook("parameters", ExcelInstructions.ID_CREATE_UPDATE_HINT, headers, rows);
    }

    /**
     * 获取参数导入模板（XLSX）。
     *
     * @return 模板 XLSX 字节
     */
    public byte[] importTemplate() {
        List<String> headers = parameterExportHeadersZh();
        return ExcelHelper.buildTemplate("parameters", ExcelInstructions.ID_CREATE_UPDATE_HINT, headers);
    }

    private static List<String> parameterExportHeadersZh() {
        return List.of(
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
                "parameter_range",
                "数据状态",
                "变更类型",
                "变更原因（中）",
                "变更影响（中）",
                "变更原因（英）",
                "变更影响（英）",
                "导出 delta",
                "不导出原因");
    }

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

    private List<String> buildParameterExportRow(
            SystemParameterPo po, Map<String, String> commandNameById, ConfigChangeDescriptionPo ch) {
        String cmdId = StringUtils.defaultString(po.getOwnedCommandId()).trim();
        String cmdName = cmdId.isEmpty() ? "" : StringUtils.defaultIfBlank(commandNameById.get(cmdId), cmdId);
        return List.of(
                nz(po.getParameterNameCn()),
                nz(po.getParameterNameEn()),
                cmdName,
                nz(po.getParameterCode()),
                po.getParameterSequence() == null ? "" : String.valueOf(po.getParameterSequence()),
                nz(po.getValueRange()),
                nz(po.getBitUsage()),
                nz(po.getValueDescriptionCn()),
                nz(po.getValueDescriptionEn()),
                nz(po.getApplicationScenarioCn()),
                nz(po.getApplicationScenarioEn()),
                nz(po.getParameterDefaultValue()),
                nz(po.getParameterRecommendedValue()),
                nz(po.getApplicableNe()),
                nz(po.getFeature()),
                nz(po.getBusinessClassification()),
                nz(po.getTakeEffectImmediately()),
                nz(po.getEffectiveModeCn()),
                nz(po.getEffectiveModeEn()),
                nz(po.getProjectTeam()),
                nz(po.getBelongingModule()),
                nz(po.getChangeSource()),
                nz(po.getPatchVersion()),
                nz(po.getIntroducedVersion()),
                nz(po.getParameterDescriptionCn()),
                nz(po.getParameterDescriptionEn()),
                nz(po.getImpactDescriptionCn()),
                nz(po.getImpactDescriptionEn()),
                nz(po.getConfigurationExampleCn()),
                nz(po.getConfigurationExampleEn()),
                nz(po.getRelatedParameterDescriptionCn()),
                nz(po.getRelatedParameterDescriptionEn()),
                nz(po.getRemark()),
                nz(po.getEnumerationValuesCn()),
                nz(po.getEnumerationValuesEn()),
                nz(po.getParameterUnitCn()),
                nz(po.getParameterUnitEn()),
                nz(po.getParameterRange()),
                nz(po.getDataStatus()),
                ch == null ? "" : nz(ch.getChangeType()),
                ch == null ? "" : nz(ch.getChangeReasonCn()),
                ch == null ? "" : nz(ch.getChangeImpactCn()),
                ch == null ? "" : nz(ch.getChangeReasonEn()),
                ch == null ? "" : nz(ch.getChangeImpactEn()),
                ch == null ? "" : nz(ch.getExportDelta()),
                ch == null ? "" : nz(ch.getNoExportReason()));
    }

    /**
     * 为导出填充「首条」变更说明：按更新时间倒序取一条。
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
     */
    @Transactional
    public BatchImportResult importParameters(
            String productId,
            String versionId,
            String mode,
            String commandId,
            String commandTypeCode,
            byte[] fileBytes) {
        ExcelHelper.ParsedSheet sheet = ExcelHelper.parseFirstSheet(fileBytes);
        List<List<String>> rows = sheet.rows();
        if (rows.isEmpty()) {
            throw new DomainRuleException("文件无内容");
        }
        int headerIdx = ExcelHelper.detectHeaderRowIndex(rows);
        if (rows.size() <= headerIdx) {
            throw new DomainRuleException("表头缺失");
        }
        List<String> header = rows.get(headerIdx);
        Map<String, Integer> hi = ExcelHelper.headerIndex(header);
        ImportSheetColumns cols = ImportSheetColumns.fromHeader(hi);
        int colCode = cols.colCode;
        ImportResultCollector c = new ImportResultCollector();
        int dataRows = rows.size() - headerIdx - 1;

        String importMode = mode == null ? "" : mode.trim().toUpperCase();
        if (!"FULL".equals(importMode) && !"INCREMENTAL".equals(importMode)) {
            throw new DomainRuleException("mode 仅支持 FULL / INCREMENTAL");
        }
        if (StringUtils.isBlank(commandId)) {
            throw new DomainRuleException("commandId 必填");
        }

        // 作用域：当前版本 + 命令 + 可选类型前缀（参数编码前缀）
        List<SystemParameterPo> scopeExisting = loadParametersForCommand(productId, versionId, commandId);
        if (StringUtils.isNotBlank(commandTypeCode)) {
            String prefix = commandTypeCode.trim() + "_";
            scopeExisting =
                    scopeExisting.stream()
                            .filter(p -> StringUtils.defaultString(p.getParameterCode()).startsWith(prefix))
                            .toList();
        }

        // 全量导入：先删后导（按 scope 删除）
        if ("FULL".equals(importMode)) {
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
            scopeExisting = List.of();
        }

        // 增量导入：已基线参数不做更改，但仍参与 BIT 冲突校验（占用）
        List<SystemParameterPo> peersForBitCheck = loadParametersForCommand(productId, versionId, commandId);

        for (int i = headerIdx + 1; i < rows.size(); i++) {
            List<String> line = rows.get(i);
            int dataRowNumber = i + 1;
            try {
                String code = cell(line, colCode);
                if (StringUtils.isBlank(code)) {
                    c.failure(dataRowNumber, "parameter_code 为空");
                } else {
                    SystemParameterPo fromSheet = new SystemParameterPo();
                    cols.applyMainFromLine(productId, versionId, commandId, code, fromSheet, line);
                    // 与 parameter_code 对应的既有行（多行同码时优先按 bit_usage 匹配），此处不做创建默认值，避免误伤更新行
                    SystemParameterPo matched = findImportMatch(peersForBitCheck, fromSheet);
                    if (matched != null && ParameterBaselinePolicy.isBaselineLocked(matched.getDataStatus())) {
                        c.failure(dataRowNumber, "已基线参数不会做更改，已跳过");
                        continue;
                    }
                    if (matched == null) {
                        SystemParameterPo incoming = fromSheet;
                        ParameterDefaults.applyForCreate(incoming);
                        applyOptionalString(line, cols.colDataStatus, incoming::setDataStatus);
                        validateAndApplyBlacklist(productId, incoming);
                        ParameterSaveInvariant.assertSequenceMatchesCode(
                                incoming.getParameterCode(), incoming.getParameterSequence());
                        // 新增：BIT 冲突（在 peers 基础上加上新行）
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
                        importReplaceChangeDescriptionIfPresent(
                                true, incoming.getParameterId(), cols, line, now);
                        peersForBitCheck = loadParametersForCommand(productId, versionId, commandId);
                        c.success(dataRowNumber);
                    } else {
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
                        // 修改：BIT 校验，排除自己
                        List<SystemParameterPo> peers = loadParametersForCommand(productId, versionId, commandId);
                        List<ParameterSaveInvariant.ParameterRowForBitCheck> bitRows = new ArrayList<>();
                        for (SystemParameterPo p : peers) {
                            if (incoming.getParameterId().equals(p.getParameterId())) {
                                continue;
                            }
                            bitRows.add(
                                    new ParameterSaveInvariant.ParameterRowForBitCheck(
                                            p.getParameterId(), p.getParameterCode(), p.getBitUsage()));
                        }
                        bitRows.add(
                                new ParameterSaveInvariant.ParameterRowForBitCheck(
                                        incoming.getParameterId(), incoming.getParameterCode(), incoming.getBitUsage()));
                        ParameterSaveInvariant.assertBitDisjointAcrossVersionCommand(null, bitRows);
                        systemParameterMapper.updateById(incoming);
                        importReplaceChangeDescriptionIfPresent(
                                false, incoming.getParameterId(), cols, line, now);
                        String opU = StringUtils.defaultIfBlank(incoming.getUpdaterId(), "system");
                        operationLogAppService.logSystemParameterUpdate(before, incoming, opU);
                        peersForBitCheck = loadParametersForCommand(productId, versionId, commandId);
                        c.success(dataRowNumber);
                    }
                }
            } catch (BlacklistViolationException e) {
                c.failure(dataRowNumber, e.getMessage());
            } catch (DomainRuleException e) {
                c.failure(dataRowNumber, e.getMessage());
            }
        }
        return c.build(dataRows);
    }

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
                    findColumn(hi, "parameter_range", "parameter_range"),
                    findColumn(hi, "data_status", "数据状态"),
                    findColumn(hi, "变更类型"),
                    findColumn(hi, "变更原因（中）"),
                    findColumn(hi, "变更影响（中）"),
                    findColumn(hi, "变更原因（英）"),
                    findColumn(hi, "变更影响（英）"),
                    findColumn(hi, "export_delta", "导出 delta", "导出delta"),
                    findColumn(hi, "不导出原因", "no_export_reason"));
        }

        private boolean hasChangePayload(List<String> line) {
            return StringUtils.isNotBlank(trimCell(line, colChType))
                    || StringUtils.isNotBlank(trimCell(line, colChReasonCn))
                    || StringUtils.isNotBlank(trimCell(line, colChImpactCn))
                    || StringUtils.isNotBlank(trimCell(line, colChReasonEn))
                    || StringUtils.isNotBlank(trimCell(line, colChImpactEn))
                    || StringUtils.isNotBlank(trimCell(line, colExportDelta))
                    || StringUtils.isNotBlank(trimCell(line, colNoExportReason));
        }

        private void applyMainFromLine(
                String productId,
                String versionId,
                String commandId,
                String parameterCode,
                SystemParameterPo target,
                List<String> line) {
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

    private static int findColumn(Map<String, Integer> hi, String... names) {
        for (String n : names) {
            Integer ix = hi.get(n);
            if (ix != null) {
                return ix;
            }
        }
        return -1;
    }

    private static String cell(List<String> line, int col) {
        if (col < 0 || col >= line.size()) {
            return "";
        }
        return line.get(col);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

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

    private List<ParameterSaveInvariant.ParameterRowForBitCheck> toBitRows(List<SystemParameterPo> list) {
        List<ParameterSaveInvariant.ParameterRowForBitCheck> rows = new ArrayList<>();
        for (SystemParameterPo p : list) {
            rows.add(
                    new ParameterSaveInvariant.ParameterRowForBitCheck(
                            p.getParameterId(), p.getParameterCode(), p.getBitUsage()));
        }
        return rows;
    }

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

    private void deleteDescriptionsByParameter(Integer parameterId) {
        configChangeDescriptionMapper.delete(
                new LambdaQueryWrapper<ConfigChangeDescriptionPo>()
                        .eq(ConfigChangeDescriptionPo::getParameterId, parameterId));
    }

    private SystemParameterPo requireParameter(String productId, String versionId, Integer parameterId) {
        SystemParameterPo po = systemParameterMapper.selectById(parameterId);
        if (po == null
                || !productId.equals(po.getOwnedProductId())
                || !versionId.equals(po.getOwnedVersionId())) {
            throw new DomainRuleException("参数不存在或不在当前产品版本下");
        }
        return po;
    }

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
     * typeKey 可为 {@code command_type_id}，或与 {@code command_type} 枚举（如 BIT）一致。
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

    /** 将 command_type_id 或枚举解析为 {@code command_type}，供位宽等规则使用。 */
    private String resolveTypeEnumForAllocation(String productId, String commandId, String typeKey) {
        CommandTypeDefinitionPo def = findTypeDefinitionForAllocation(productId, commandId, typeKey);
        if (def != null && StringUtils.isNotBlank(def.getCommandType())) {
            return def.getCommandType();
        }
        return typeKey;
    }
}
