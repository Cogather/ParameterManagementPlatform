package com.coretool.param.domain.command.service;

import com.coretool.param.domain.command.CommandTypeDefinition;
import com.coretool.param.domain.command.CommandTypeVersionRange;
import com.coretool.param.domain.command.repository.CommandTypeVersionRangeRepository;
import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 命令类型版本区段领域服务：封装跨聚合校验（归属一致、区段不重叠、区段边界等）。
 */
public class CommandTypeVersionRangeDomainService {

    public record CreateCommand(
            String productId,
            String ownedCommandId,
            String ownedTypeId,
            String versionRangeId,
            Integer startIndex,
            Integer endIndex,
            String rangeDescription,
            String rangeType,
            String ownedVersionOrBusinessId,
            Integer rangeStatus,
            String creatorId,
            String updaterId,
            LocalDateTime now) {}

    public record UpdateCommand(
            String productId,
            String rangeId,
            Integer startIndex,
            Integer endIndex,
            String rangeDescription,
            String rangeType,
            String ownedVersionOrBusinessId,
            Integer rangeStatus,
            String updaterId,
            LocalDateTime now) {}

    private final CommandTypeVersionRangeRepository rangeRepository;
    private final CommandTypeDefinitionDomainService typeDomainService;

    /**
     * 创建命令类型版本区段领域服务。
     *
     * @param rangeRepository  区段仓储
     * @param typeDomainService 类型定义领域服务
     */
    public CommandTypeVersionRangeDomainService(
            CommandTypeVersionRangeRepository rangeRepository,
            CommandTypeDefinitionDomainService typeDomainService) {
        this.rangeRepository = rangeRepository;
        this.typeDomainService = typeDomainService;
    }

    /**
     * 创建新区段（含归属一致、边界校验、区段不重叠校验）。
     *
     * @param input 创建入参，各字段与 {@link CreateCommand} 记录组件一一对应
     * @return 新区段
     */
    public CommandTypeVersionRange createNew(CreateCommand input) {
        if (input == null) {
            throw new DomainRuleException("创建参数不能为空");
        }
        if (StringUtils.isBlank(input.ownedCommandId())) {
            throw new DomainRuleException("归属命令ID不能为空");
        }
        if (StringUtils.isBlank(input.ownedVersionOrBusinessId())) {
            throw new DomainRuleException("归属版本ID不能为空");
        }
        CommandTypeDefinition type = typeDomainService.requireOwned(input.productId(), input.ownedTypeId());
        if (!StringUtils.equals(type.getOwnedCommandId(), input.ownedCommandId())) {
            throw new DomainRuleException("类型定义与归属命令不一致");
        }
        CommandTypeVersionRange r =
                CommandTypeVersionRange.registerNew(
                        new CommandTypeVersionRange.Registration(
                                input.productId(),
                                input.ownedCommandId(),
                                input.ownedTypeId(),
                                input.versionRangeId(),
                                input.startIndex(),
                                input.endIndex(),
                                input.rangeDescription(),
                                input.rangeType(),
                                input.ownedVersionOrBusinessId(),
                                input.rangeStatus(),
                                input.creatorId(),
                                input.updaterId(),
                                input.now()));
        r.assertWithinTypeBounds(type.getMinValue(), type.getMaxValue());
        assertNoOverlap(r, null);
        return r;
    }

    /**
     * 更新既有区段（含归属校验、边界校验、区段不重叠校验）。
     *
     * @param input 更新入参，各字段与 {@link UpdateCommand} 记录组件一一对应
     * @return 更新后的区段
     */
    public CommandTypeVersionRange updateExisting(UpdateCommand input) {
        if (input == null) {
            throw new DomainRuleException("更新参数不能为空");
        }
        CommandTypeVersionRange existing =
                rangeRepository.findById(input.rangeId()).orElseThrow(() -> new DomainRuleException("区段不存在"));
        if (!existing.belongsToProduct(input.productId())) {
            throw new DomainRuleException("区段不属于该产品");
        }
        CommandTypeDefinition type = typeDomainService.requireOwned(input.productId(), existing.getOwnedTypeId());
        existing.applyEditablePatch(
                new CommandTypeVersionRange.EditablePatch(
                        input.startIndex(),
                        input.endIndex(),
                        input.rangeDescription(),
                        input.rangeType(),
                        input.ownedVersionOrBusinessId(),
                        input.rangeStatus(),
                        input.updaterId(),
                        input.now()));
        existing.assertWithinTypeBounds(type.getMinValue(), type.getMaxValue());
        assertNoOverlap(existing, input.rangeId());
        return existing;
    }

    /**
     * 禁用区段（含归属校验）。
     *
     * @param productId 产品 ID
     * @param rangeId   区段 ID
     * @param now       当前时间
     * @return 禁用后的区段
     */
    public CommandTypeVersionRange disable(String productId, String rangeId, LocalDateTime now) {
        CommandTypeVersionRange existing =
                rangeRepository.findById(rangeId).orElseThrow(() -> new DomainRuleException("区段不存在"));
        if (!existing.belongsToProduct(productId)) {
            throw new DomainRuleException("区段不属于该产品");
        }
        existing.disable(now);
        return existing;
    }

    private void assertNoOverlap(CommandTypeVersionRange candidate, String excludeRangeId) {
        List<CommandTypeVersionRange> enabled =
                rangeRepository.listEnabledInScope(
                        candidate.getOwnedProductId(),
                        candidate.getOwnedCommandId(),
                        candidate.getOwnedTypeId(),
                        candidate.getOwnedVersionOrBusinessId());
        for (CommandTypeVersionRange r : enabled) {
            if (excludeRangeId != null && excludeRangeId.equals(r.getVersionRangeId())) {
                continue;
            }
            if (candidate.overlapsIndexRange(r)) {
                throw new DomainRuleException("RANGE_OVERLAP: 同一版本下序号区段不能重叠");
            }
        }
    }
}

