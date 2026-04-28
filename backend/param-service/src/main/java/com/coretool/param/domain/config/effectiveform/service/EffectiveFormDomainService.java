package com.coretool.param.domain.config.effectiveform.service;

import com.coretool.param.domain.config.effectiveform.EffectiveForm;
import com.coretool.param.domain.config.effectiveform.repository.EffectiveFormRepository;
import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

/** 生效形态领域服务：封装归属校验与状态变更等规则。 */
public class EffectiveFormDomainService {

    public record CreateCommand(
            String productId,
            String effectiveFormId,
            String nameCn,
            String nameEn,
            String description,
            Integer status,
            String creatorId,
            String updaterId,
            LocalDateTime now) {}

    public record UpdateCommand(
            String productId,
            String effectiveFormId,
            String nameCn,
            String nameEn,
            String description,
            Integer status,
            String updaterId,
            LocalDateTime now) {}

    private final EffectiveFormRepository repository;

    /**
     * 创建生效形态领域服务。
     *
     * @param repository 生效形态仓储
     */
    public EffectiveFormDomainService(EffectiveFormRepository repository) {
        this.repository = repository;
    }

    /**
     * 创建新生效形态。
     *
     * @param input 创建入参，各字段与 {@link CreateCommand} 记录组件一一对应
     * @return 新生效形态
     */
    public EffectiveForm createNew(CreateCommand input) {
        if (input == null) {
            throw new DomainRuleException("创建参数不能为空");
        }
        return EffectiveForm.registerNew(
                new EffectiveForm.Registration(
                        input.productId(),
                        input.effectiveFormId(),
                        input.nameCn(),
                        input.nameEn(),
                        input.description(),
                        input.status(),
                        input.creatorId(),
                        input.updaterId(),
                        input.now()));
    }

    /**
     * 更新既有生效形态（无字段变更时仅刷新更新时间）。
     *
     * @param input 更新入参，各字段与 {@link UpdateCommand} 记录组件一一对应
     * @return 更新后的生效形态
     */
    public EffectiveForm updateExisting(UpdateCommand input) {
        if (input == null) {
            throw new DomainRuleException("更新参数不能为空");
        }
        EffectiveForm existing = requireOwned(input.productId(), input.effectiveFormId());
        if (StringUtils.isNotBlank(input.nameCn())
                || StringUtils.isNotBlank(input.nameEn())
                || input.description() != null
                || input.status() != null
                || StringUtils.isNotBlank(input.updaterId())) {
            existing.applyPatch(
                    new EffectiveForm.Patch(
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
     * 禁用生效形态（含归属校验）。
     *
     * @param productId       产品 ID
     * @param effectiveFormId 生效形态 ID
     * @param now             当前时间
     * @return 禁用后的生效形态
     */
    public EffectiveForm disable(String productId, String effectiveFormId, LocalDateTime now) {
        EffectiveForm existing = requireOwned(productId, effectiveFormId);
        existing.disable(now);
        return existing;
    }

    /**
     * 获取并校验生效形态归属产品（不存在或不归属则抛出领域异常）。
     *
     * @param productId 产品 ID
     * @param id        生效形态 ID
     * @return 生效形态
     */
    public EffectiveForm requireOwned(String productId, String id) {
        return repository
                .findById(id)
                .filter(e -> e.belongsToProduct(productId))
                .orElseThrow(() -> new DomainRuleException("生效形态不存在或不属于该产品"));
    }
}

