package com.coretool.param.domain.config.category.service;

import com.coretool.param.domain.config.category.BusinessCategory;
import com.coretool.param.domain.config.category.repository.BusinessCategoryRepository;
import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

/** 业务分类领域服务：封装唯一性/归属/状态变更等规则。 */
public class BusinessCategoryDomainService {

    public record CreateCommand(
            String productId,
            String categoryId,
            String nameCn,
            String nameEn,
            String featureRange,
            String categoryType,
            Integer status,
            String creatorId,
            String updaterId,
            LocalDateTime now) {}

    public record UpdateCommand(
            String productId,
            String categoryId,
            String nameCn,
            String nameEn,
            String featureRange,
            String categoryType,
            Integer status,
            String updaterId,
            LocalDateTime now) {}

    private final BusinessCategoryRepository repository;

    /**
     * 创建业务分类领域服务。
     *
     * @param repository 业务分类仓储
     */
    public BusinessCategoryDomainService(BusinessCategoryRepository repository) {
        this.repository = repository;
    }

    /**
     * 创建新业务分类（含同产品下中文名唯一性校验）。
     *
     * @param input 创建入参，各字段与 {@link CreateCommand} 记录组件一一对应
     * @return 新业务分类
     */
    public BusinessCategory createNew(CreateCommand input) {
        if (input == null) {
            throw new DomainRuleException("创建参数不能为空");
        }
        if (repository.existsSameChineseNameInProduct(input.productId(), input.nameCn(), null)) {
            throw new DomainRuleException("CATEGORY_NAME_DUPLICATE: 同一产品下分类中文名已存在");
        }
        return BusinessCategory.registerNew(
                new BusinessCategory.Registration(
                        input.productId(),
                        input.categoryId(),
                        input.nameCn(),
                        input.nameEn(),
                        input.featureRange(),
                        input.categoryType(),
                        input.status(),
                        input.creatorId(),
                        input.updaterId(),
                        input.now()));
    }

    /**
     * 更新既有业务分类（含归属校验与同产品下中文名唯一性校验）。
     *
     * @param input 更新入参，各字段与 {@link UpdateCommand} 记录组件一一对应
     * @return 更新后的业务分类
     */
    public BusinessCategory updateExisting(UpdateCommand input) {
        if (input == null) {
            throw new DomainRuleException("更新参数不能为空");
        }
        BusinessCategory existing =
                repository
                        .findByCategoryId(input.categoryId())
                        .orElseThrow(() -> new DomainRuleException("分类不存在或不属于该产品"));
        if (!existing.belongsToProduct(input.productId())) {
            throw new DomainRuleException("分类不存在或不属于该产品");
        }
        if (StringUtils.isNotBlank(input.nameCn())
                && !StringUtils.equals(input.nameCn(), existing.getCategoryNameCn())) {
            if (repository.existsSameChineseNameInProduct(input.productId(), input.nameCn(), input.categoryId())) {
                throw new DomainRuleException("CATEGORY_NAME_DUPLICATE: 同一产品下分类中文名已存在");
            }
        }
        existing.applyEditablePatch(
                new BusinessCategory.EditablePatch(
                        input.nameCn(),
                        input.nameEn(),
                        input.featureRange(),
                        input.categoryType(),
                        input.status(),
                        input.updaterId(),
                        input.now()));
        return existing;
    }

    /**
     * 禁用业务分类（含归属校验）。
     *
     * @param productId  产品 ID
     * @param categoryId 分类 ID
     * @param now        当前时间
     * @return 禁用后的业务分类
     */
    public BusinessCategory disable(String productId, String categoryId, LocalDateTime now) {
        BusinessCategory existing =
                repository.findByCategoryId(categoryId).orElseThrow(() -> new DomainRuleException("分类不存在或不属于该产品"));
        if (!existing.belongsToProduct(productId)) {
            throw new DomainRuleException("分类不存在或不属于该产品");
        }
        existing.disable(now);
        return existing;
    }

    /**
     * 获取并校验业务分类归属产品（不存在或不归属则抛出领域异常）。
     *
     * @param productId  产品 ID
     * @param categoryId 分类 ID
     * @return 业务分类
     */
    public BusinessCategory requireOwned(String productId, String categoryId) {
        return repository
                .findByCategoryId(categoryId)
                .filter(c -> c.belongsToProduct(productId))
                .orElseThrow(() -> new DomainRuleException("分类不存在或不属于该产品"));
    }
}

