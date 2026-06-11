/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coretool.param.domain.config.keyword.repository.ChangeSourceKeywordRepository;
import com.coretool.param.domain.exception.DomainRuleException;
import com.coretool.param.domain.parameter.ChangeSourceBlacklistPolicy;
import com.coretool.param.domain.parameter.ParameterBaselinePolicy;
import com.coretool.param.domain.parameter.ParameterSaveInvariant;
import com.coretool.param.domain.support.IdGenerator;
import com.coretool.param.infrastructure.persistence.entity.ConfigChangeDescriptionPo;
import com.coretool.param.infrastructure.persistence.entity.EntityCommandMappingPo;
import com.coretool.param.infrastructure.persistence.entity.SystemParameterPo;
import com.coretool.param.infrastructure.persistence.mapper.ConfigChangeDescriptionMapper;
import com.coretool.param.infrastructure.persistence.mapper.EntityCommandMappingMapper;
import com.coretool.param.infrastructure.persistence.mapper.SystemParameterMapper;
import com.coretool.param.domain.exception.BlacklistViolationException;
import com.coretool.param.ui.vo.ParameterSyncCommandRequest;
import com.coretool.param.ui.vo.ParameterSyncItemRequest;
import com.coretool.param.ui.vo.ParameterSyncParameterOption;
import com.coretool.param.ui.vo.ParameterSyncResultPayload;
import com.coretool.param.ui.vo.ParameterSyncTypeOption;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 跨版本参数复制（全量继承 / 按需同步 / 拉分支）。
 *
 * @since 2026-05-21
 */
@Service
public class ParameterVersionCopyAppService {

    private static final String INTRODUCE_TYPE_INHERIT = "继承Inherit";
    private static final String DATA_STATUS_OBSOLETE = "Obsolete";

    private final SystemParameterMapper systemParameterMapper;
    private final ConfigChangeDescriptionMapper configChangeDescriptionMapper;
    private final EntityCommandMappingMapper entityCommandMappingMapper;
    private final ChangeSourceKeywordRepository changeSourceKeywordRepository;
    private final OperationLogAppService operationLogAppService;

    /**
     * 构造复制服务。
     *
     * @param systemParameterMapper         参数 Mapper
     * @param configChangeDescriptionMapper 变更说明 Mapper
     * @param entityCommandMappingMapper    命令 Mapper
     * @param changeSourceKeywordRepository 变更来源关键字仓储
     * @param operationLogAppService        操作日志
     */
    public ParameterVersionCopyAppService(
            SystemParameterMapper systemParameterMapper,
            ConfigChangeDescriptionMapper configChangeDescriptionMapper,
            EntityCommandMappingMapper entityCommandMappingMapper,
            ChangeSourceKeywordRepository changeSourceKeywordRepository,
            OperationLogAppService operationLogAppService) {
        this.systemParameterMapper = systemParameterMapper;
        this.configChangeDescriptionMapper = configChangeDescriptionMapper;
        this.entityCommandMappingMapper = entityCommandMappingMapper;
        this.changeSourceKeywordRepository = changeSourceKeywordRepository;
        this.operationLogAppService = operationLogAppService;
    }

    /**
     * 全量复制源版本软参空间至目标版本（冲突则整单失败）。
     *
     * @param productId       产品 ID
     * @param sourceVersionId 源版本 ID
     * @param targetVersionId 目标版本 ID
     * @param operatorId      操作人
     * @return 成功复制条数
     */
    @Transactional
    public int copyAll(String productId, String sourceVersionId, String targetVersionId, String operatorId) {
        assertDistinctVersions(sourceVersionId, targetVersionId);
        List<SystemParameterPo> sources = listCopyableByVersion(productId, sourceVersionId);
        if (sources.isEmpty()) {
            return 0;
        }
        return insertClonesGroupedByCommand(productId, sourceVersionId, targetVersionId, sources, operatorId);
    }

    /**
     * 按用户勾选同步参数（部分成功）。
     *
     * @param productId       产品 ID
     * @param targetVersionId 目标版本（当前版）
     * @param request         同步请求
     * @param operatorId      操作人
     * @return 汇总结果
     */
    @Transactional
    public ParameterSyncResultPayload syncMany(
            String productId, String targetVersionId, ParameterSyncCommandRequest request, String operatorId) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new DomainRuleException("同步项不能为空");
        }
        String sourceVersionId = StringUtils.trimToNull(request.getSourceVersionId());
        if (sourceVersionId == null) {
            throw new DomainRuleException("sourceVersionId 必填");
        }
        assertDistinctVersions(sourceVersionId, targetVersionId);
        ParameterSyncResultPayload out = new ParameterSyncResultPayload();
        out.setFailures(new ArrayList<>());
        out.setSuccesses(new ArrayList<>());
        for (ParameterSyncItemRequest item : request.getItems()) {
            processOneSyncItem(productId, sourceVersionId, targetVersionId, item, operatorId, out);
        }
        out.setSuccessCount(out.getSuccesses().size());
        out.setFailureCount(out.getFailures().size());
        return out;
    }

    /**
     * 源版本下存在参数的命令+类型选项。
     *
     * @param productId       产品 ID
     * @param sourceVersionId 源版本 ID
     * @return 选项列表
     */
    public List<ParameterSyncTypeOption> listTypeOptions(String productId, String sourceVersionId) {
        List<SystemParameterPo> rows = listCopyableByVersion(productId, sourceVersionId);
        Map<String, String> commandNames = commandNamesByProduct(productId);
        Map<String, ParameterSyncTypeOption> keyed = new LinkedHashMap<>();
        for (SystemParameterPo p : rows) {
            String cmd = p.getOwnedCommandId();
            String typeKey = resolveTypeKey(p);
            String key = cmd + "\0" + typeKey;
            keyed.computeIfAbsent(key, k -> buildTypeOption(cmd, typeKey, commandNames, p));
        }
        return keyed.values().stream()
                .sorted(Comparator.comparing(ParameterSyncTypeOption::getCommandName)
                        .thenComparing(ParameterSyncTypeOption::getCommandTypeName))
                .collect(Collectors.toList());
    }

    /**
     * 源版本+命令+类型下可同步参数列表。
     *
     * @param productId       产品 ID
     * @param sourceVersionId 源版本
     * @param commandId       命令 ID
     * @param commandTypeId   类型 ID 或类型枚举
     * @return 参数选项
     */
    public List<ParameterSyncParameterOption> listParameterOptions(
            String productId, String sourceVersionId, String commandId, String commandTypeId) {
        if (StringUtils.isAnyBlank(commandId, commandTypeId)) {
            throw new DomainRuleException("commandId 与 commandTypeId 必填");
        }
        String prefix = commandTypeId.trim() + "_";
        return listCopyableByVersion(productId, sourceVersionId).stream()
                .filter(p -> commandId.equals(p.getOwnedCommandId()))
                .filter(p -> p.getParameterCode() != null && p.getParameterCode().startsWith(prefix))
                .map(this::toParameterOption)
                .sorted(Comparator.comparing(ParameterSyncParameterOption::getParameterNameCn,
                        Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
    }

    private void processOneSyncItem(
            String productId,
            String sourceVersionId,
            String targetVersionId,
            ParameterSyncItemRequest item,
            String operatorId,
            ParameterSyncResultPayload out) {
        if (item == null || item.getSourceParameterId() == null) {
            addFailure(out, null, null, "PARAM_SYNC_SOURCE_INVALID: 缺少 sourceParameterId");
            return;
        }
        SystemParameterPo source = systemParameterMapper.selectById(item.getSourceParameterId());
        if (!isValidSource(source, productId, sourceVersionId)) {
            addFailure(out, item.getSourceParameterId(), null, "PARAM_SYNC_SOURCE_INVALID: 源参数不存在或不属于源版本");
            return;
        }
        try {
            int inserted =
                    insertClonesGroupedByCommand(
                            productId,
                            sourceVersionId,
                            targetVersionId,
                            List.of(source),
                            operatorId);
            if (inserted == 1) {
                SystemParameterPo row =
                        findNewestByCode(productId, targetVersionId, source.getParameterCode());
                ParameterSyncResultPayload.ParameterSyncSuccessItem ok =
                        new ParameterSyncResultPayload.ParameterSyncSuccessItem();
                ok.setSourceParameterId(item.getSourceParameterId());
                ok.setNewParameterId(row != null ? row.getParameterId() : null);
                out.getSuccesses().add(ok);
            } else {
                addFailure(out, item.getSourceParameterId(), source.getParameterNameCn(), "同步插入失败");
            }
        } catch (DomainRuleException | BlacklistViolationException ex) {
            addFailure(out, item.getSourceParameterId(), source.getParameterNameCn(), ex.getMessage());
        }
    }

    private int insertClonesGroupedByCommand(
            String productId,
            String sourceVersionId,
            String targetVersionId,
            List<SystemParameterPo> sources,
            String operatorId) {
        Map<String, List<SystemParameterPo>> byCmd =
                sources.stream()
                        .collect(Collectors.groupingBy(SystemParameterPo::getOwnedCommandId));
        int total = 0;
        for (Map.Entry<String, List<SystemParameterPo>> e : byCmd.entrySet()) {
            total += insertClonesForCommand(
                    productId, sourceVersionId, targetVersionId, e.getKey(), e.getValue(), operatorId);
        }
        return total;
    }

    private int insertClonesForCommand(
            String productId,
            String sourceVersionId,
            String targetVersionId,
            String commandId,
            List<SystemParameterPo> sources,
            String operatorId) {
        List<SystemParameterPo> targetExisting = loadByVersionAndCommand(productId, targetVersionId, commandId);
        List<ParameterSaveInvariant.ParameterRowForBitCheck> bitRows = toBitRows(targetExisting);
        LocalDateTime now = LocalDateTime.now();
        int n = 0;
        for (SystemParameterPo src : sources) {
            SystemParameterPo clone = cloneRow(src, productId, targetVersionId, sourceVersionId, operatorId, now);
            validateClone(productId, clone);
            bitRows.add(
                    new ParameterSaveInvariant.ParameterRowForBitCheck(
                            null, clone.getParameterCode(), clone.getBitUsage()));
            ParameterSaveInvariant.assertBitDisjointAcrossVersionCommand(null, bitRows);
            systemParameterMapper.insert(clone);
            copyDescriptions(src.getParameterId(), clone.getParameterId(), now);
            operationLogAppService.logSystemParameterCreate(clone, operatorId);
            n++;
        }
        return n;
    }

    private SystemParameterPo cloneRow(
            SystemParameterPo src,
            String productId,
            String targetVersionId,
            String sourceVersionId,
            String operatorId,
            LocalDateTime now) {
        SystemParameterPo clone = new SystemParameterPo();
        BeanUtils.copyProperties(src, clone);
        clone.setParameterId(null);
        clone.setOwnedProductId(productId);
        clone.setOwnedVersionId(targetVersionId);
        clone.setIntroduceType(INTRODUCE_TYPE_INHERIT);
        clone.setInheritReferenceVersionId(sourceVersionId);
        clone.setCreatorId(operatorId);
        clone.setUpdaterId(operatorId);
        clone.setCreationTimestamp(now);
        clone.setUpdateTimestamp(now);
        return clone;
    }

    private void validateClone(String productId, SystemParameterPo clone) {
        ParameterSaveInvariant.assertSequenceMatchesCode(
                clone.getParameterCode(), clone.getParameterSequence());
        applyBlacklist(productId, clone);
    }

    private void applyBlacklist(String productId, SystemParameterPo main) {
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

    private void copyDescriptions(Integer sourcePid, Integer targetPid, LocalDateTime now) {
        List<ConfigChangeDescriptionPo> rows =
                configChangeDescriptionMapper.selectList(
                        new LambdaQueryWrapper<ConfigChangeDescriptionPo>()
                                .eq(ConfigChangeDescriptionPo::getParameterId, sourcePid));
        for (ConfigChangeDescriptionPo d : rows) {
            ConfigChangeDescriptionPo copy = new ConfigChangeDescriptionPo();
            BeanUtils.copyProperties(d, copy);
            copy.setChangeDescriptionId(IdGenerator.changeDescriptionId());
            copy.setParameterId(targetPid);
            copy.setUpdateTimestamp(now);
            configChangeDescriptionMapper.insert(copy);
        }
    }

    private List<SystemParameterPo> listCopyableByVersion(String productId, String versionId) {
        return systemParameterMapper.selectList(
                        new LambdaQueryWrapper<SystemParameterPo>()
                                .eq(SystemParameterPo::getOwnedProductId, productId)
                                .eq(SystemParameterPo::getOwnedVersionId, versionId))
                .stream()
                .filter(this::isCopyable)
                .collect(Collectors.toList());
    }

    private boolean isCopyable(SystemParameterPo p) {
        if (p == null) {
            return false;
        }
        String st = p.getDataStatus();
        if (st == null) {
            return true;
        }
        return !DATA_STATUS_OBSOLETE.equalsIgnoreCase(st.trim());
    }

    private SystemParameterPo findNewestByCode(String productId, String versionId, String code) {
        List<SystemParameterPo> list =
                systemParameterMapper.selectList(
                        new LambdaQueryWrapper<SystemParameterPo>()
                                .eq(SystemParameterPo::getOwnedProductId, productId)
                                .eq(SystemParameterPo::getOwnedVersionId, versionId)
                                .eq(SystemParameterPo::getParameterCode, code)
                                .orderByDesc(SystemParameterPo::getParameterId)
                                .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    private List<SystemParameterPo> loadByVersionAndCommand(
            String productId, String versionId, String commandId) {
        return systemParameterMapper.selectList(
                new LambdaQueryWrapper<SystemParameterPo>()
                        .eq(SystemParameterPo::getOwnedProductId, productId)
                        .eq(SystemParameterPo::getOwnedVersionId, versionId)
                        .eq(SystemParameterPo::getOwnedCommandId, commandId));
    }

    private static List<ParameterSaveInvariant.ParameterRowForBitCheck> toBitRows(List<SystemParameterPo> list) {
        List<ParameterSaveInvariant.ParameterRowForBitCheck> rows = new ArrayList<>();
        for (SystemParameterPo p : list) {
            rows.add(
                    new ParameterSaveInvariant.ParameterRowForBitCheck(
                            p.getParameterId(), p.getParameterCode(), p.getBitUsage()));
        }
        return rows;
    }

    private static void assertDistinctVersions(String sourceVersionId, String targetVersionId) {
        if (StringUtils.equals(sourceVersionId, targetVersionId)) {
            throw new DomainRuleException("源版本与目标版本不能相同");
        }
    }

    private static boolean isValidSource(SystemParameterPo source, String productId, String sourceVersionId) {
        return source != null
                && productId.equals(source.getOwnedProductId())
                && sourceVersionId.equals(source.getOwnedVersionId())
                && !DATA_STATUS_OBSOLETE.equalsIgnoreCase(StringUtils.trimToEmpty(source.getDataStatus()));
    }

    private Map<String, String> commandNamesByProduct(String productId) {
        List<EntityCommandMappingPo> cmds =
                entityCommandMappingMapper.selectList(
                        new LambdaQueryWrapper<EntityCommandMappingPo>()
                                .eq(EntityCommandMappingPo::getOwnedProductId, productId)
                                .eq(EntityCommandMappingPo::getCommandStatus, 1));
        Map<String, String> map = new HashMap<>();
        for (EntityCommandMappingPo c : cmds) {
            map.put(c.getCommandId(), c.getCommandName());
        }
        return map;
    }

    private ParameterSyncTypeOption buildTypeOption(
            String commandId,
            String typeKey,
            Map<String, String> commandNames,
            SystemParameterPo sample) {
        ParameterSyncTypeOption o = new ParameterSyncTypeOption();
        o.setCommandId(commandId);
        o.setCommandName(commandNames.getOrDefault(commandId, commandId));
        o.setCommandTypeId(typeKey);
        o.setCommandTypeName(typeKeyFromCode(sample.getParameterCode()));
        return o;
    }

    private static String resolveTypeKey(SystemParameterPo p) {
        String code = p.getParameterCode();
        if (code == null || !code.contains("_")) {
            return "";
        }
        return code.substring(0, code.indexOf('_'));
    }

    private static String typeKeyFromCode(String code) {
        if (code == null || !code.contains("_")) {
            return "";
        }
        return code.substring(0, code.indexOf('_'));
    }

    private ParameterSyncParameterOption toParameterOption(SystemParameterPo p) {
        ParameterSyncParameterOption o = new ParameterSyncParameterOption();
        o.setSourceParameterId(p.getParameterId());
        o.setParameterNameCn(p.getParameterNameCn());
        o.setDataStatus(p.getDataStatus());
        return o;
    }

    private static void addFailure(
            ParameterSyncResultPayload out,
            Integer sourceParameterId,
            String nameCn,
            String reason) {
        ParameterSyncResultPayload.ParameterSyncFailureItem f =
                new ParameterSyncResultPayload.ParameterSyncFailureItem();
        f.setSourceParameterId(sourceParameterId);
        f.setParameterNameCn(nameCn);
        f.setReason(reason);
        out.getFailures().add(f);
    }
}
