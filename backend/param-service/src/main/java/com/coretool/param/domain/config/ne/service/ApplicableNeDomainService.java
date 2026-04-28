package com.coretool.param.domain.config.ne.service;

import com.coretool.param.domain.config.ne.ApplicableNe;
import com.coretool.param.domain.config.ne.repository.ApplicableNeRepository;
import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

/** 适用网元领域服务：封装唯一性/归属/状态变更等规则。 */
public class ApplicableNeDomainService {

    public record CreateCommand(
            String productId,
            String neTypeId,
            String nameCn,
            String description,
            Integer status,
            String productForm,
            String creatorId,
            String updaterId,
            LocalDateTime now) {}

    public record UpdateCommand(
            String productId,
            String neTypeId,
            String nameCn,
            String description,
            Integer status,
            String productForm,
            String updaterId,
            LocalDateTime now) {}

    private final ApplicableNeRepository repository;

    /**
     * 创建适用网元领域服务。
     *
     * @param repository 适用网元仓储
     */
    public ApplicableNeDomainService(ApplicableNeRepository repository) {
        this.repository = repository;
    }

    /**
     * 创建新适用网元（含同产品下名称唯一性校验）。
     *
     * @param input 创建入参，各字段与 {@link CreateCommand} 记录组件一一对应
     * @return 新适用网元
     */
    public ApplicableNe createNew(CreateCommand input) {
        if (input == null) {
            throw new DomainRuleException("创建参数不能为空");
        }
        ApplicableNe ne =
                ApplicableNe.registerNew(
                        new ApplicableNe.Registration(
                                input.productId(),
                                input.neTypeId(),
                                input.nameCn(),
                                input.description(),
                                input.status(),
                                input.productForm(),
                                input.creatorId(),
                                input.updaterId(),
                                input.now()));
        if (repository.existsSameNameInProduct(input.productId(), ne.getNeTypeNameCn(), null)) {
            throw new DomainRuleException("NE_NAME_DUPLICATE: 同一产品下网元名称已存在");
        }
        return ne;
    }

    /**
     * 更新既有适用网元（含归属校验与同产品下名称唯一性校验；无字段变更则仅刷新更新时间）。
     *
     * @param input 更新入参，各字段与 {@link UpdateCommand} 记录组件一一对应
     * @return 更新后的适用网元
     */
    public ApplicableNe updateExisting(UpdateCommand input) {
        if (input == null) {
            throw new DomainRuleException("更新参数不能为空");
        }
        ApplicableNe existing = requireOwned(input.productId(), input.neTypeId());
        if (StringUtils.isNotBlank(input.nameCn())
                && !StringUtils.equals(input.nameCn(), existing.getNeTypeNameCn())) {
            if (repository.existsSameNameInProduct(input.productId(), input.nameCn(), input.neTypeId())) {
                throw new DomainRuleException("NE_NAME_DUPLICATE: 同一产品下网元名称已存在");
            }
        }
        if (StringUtils.isNotBlank(input.nameCn())
                || input.description() != null
                || input.status() != null
                || input.productForm() != null
                || StringUtils.isNotBlank(input.updaterId())) {
            existing.applyPatch(
                    new ApplicableNe.Patch(
                            input.nameCn(),
                            input.description(),
                            input.status(),
                            input.productForm(),
                            input.updaterId(),
                            input.now()));
        } else {
            existing.touch(input.now());
        }
        return existing;
    }

    /**
     * 禁用适用网元（含归属校验）。
     *
     * @param productId 产品 ID
     * @param neTypeId  网元类型 ID
     * @param now       当前时间
     * @return 禁用后的适用网元
     */
    public ApplicableNe disable(String productId, String neTypeId, LocalDateTime now) {
        ApplicableNe existing = requireOwned(productId, neTypeId);
        existing.disable(now);
        return existing;
    }

    /**
     * 获取并校验适用网元归属产品（不存在或不归属则抛出领域异常）。
     *
     * @param productId 产品 ID
     * @param neTypeId  网元类型 ID
     * @return 适用网元
     */
    public ApplicableNe requireOwned(String productId, String neTypeId) {
        return repository
                .findByNeTypeId(neTypeId)
                .filter(ne -> ne.belongsToProduct(productId))
                .orElseThrow(() -> new DomainRuleException("网元不存在或不属于该产品"));
    }
}

