package com.coretool.param.domain.config.effectivemode.service;

import com.coretool.param.domain.config.effectivemode.EffectiveMode;
import com.coretool.param.domain.config.effectivemode.repository.EffectiveModeRepository;
import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

/** 生效方式领域服务：封装归属校验与状态变更等规则。 */
public class EffectiveModeDomainService {

    public record CreateCommand(
            String productId,
            String effectiveModeId,
            String nameCn,
            String nameEn,
            String description,
            Integer status,
            String creatorId,
            String updaterId,
            LocalDateTime now) {}

    public record UpdateCommand(
            String productId,
            String effectiveModeId,
            String nameCn,
            String nameEn,
            String description,
            Integer status,
            String updaterId,
            LocalDateTime now) {}

    private final EffectiveModeRepository repository;

    /**
     * 创建生效方式领域服务。
     *
     * @param repository 生效方式仓储
     */
    public EffectiveModeDomainService(EffectiveModeRepository repository) {
        this.repository = repository;
    }

    /**
     * 创建新生效方式。
     *
     * @param input 创建入参，各字段与 {@link CreateCommand} 记录组件一一对应
     * @return 新生效方式
     */
    public EffectiveMode createNew(CreateCommand input) {
        if (input == null) {
            throw new DomainRuleException("创建参数不能为空");
        }
        return EffectiveMode.registerNew(
                new EffectiveMode.Registration(
                        input.productId(),
                        input.effectiveModeId(),
                        input.nameCn(),
                        input.nameEn(),
                        input.description(),
                        input.status(),
                        input.creatorId(),
                        input.updaterId(),
                        input.now()));
    }

    /**
     * 更新既有生效方式（无字段变更时仅刷新更新时间）。
     *
     * @param input 更新入参，各字段与 {@link UpdateCommand} 记录组件一一对应
     * @return 更新后的生效方式
     */
    public EffectiveMode updateExisting(UpdateCommand input) {
        if (input == null) {
            throw new DomainRuleException("更新参数不能为空");
        }
        EffectiveMode existing = requireOwned(input.productId(), input.effectiveModeId());
        if (StringUtils.isNotBlank(input.nameCn())
                || StringUtils.isNotBlank(input.nameEn())
                || input.description() != null
                || input.status() != null
                || StringUtils.isNotBlank(input.updaterId())) {
            existing.applyPatch(
                    new EffectiveMode.Patch(
                            input.nameCn(),
                            input.nameEn(),
                            input.description(),
                            input.status(),
                            input.updaterId(),
                            input.now()));
        } else {
            existing.touch(input.now());
        }
        return existing;
    }

    /**
     * 禁用生效方式（含归属校验）。
     *
     * @param productId       产品 ID
     * @param effectiveModeId 生效方式 ID
     * @param now             当前时间
     * @return 禁用后的生效方式
     */
    public EffectiveMode disable(String productId, String effectiveModeId, LocalDateTime now) {
        EffectiveMode existing = requireOwned(productId, effectiveModeId);
        existing.disable(now);
        return existing;
    }

    /**
     * 获取并校验生效方式归属产品（不存在或不归属则抛出领域异常）。
     *
     * @param productId 产品 ID
     * @param id        生效方式 ID
     * @return 生效方式
     */
    public EffectiveMode requireOwned(String productId, String id) {
        return repository
                .findById(id)
                .filter(e -> e.belongsToProduct(productId))
                .orElseThrow(() -> new DomainRuleException("生效方式不存在或不属于该产品"));
    }
}

