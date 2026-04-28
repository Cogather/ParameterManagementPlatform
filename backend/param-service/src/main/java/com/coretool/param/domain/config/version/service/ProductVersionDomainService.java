package com.coretool.param.domain.config.version.service;

import com.coretool.param.domain.config.version.ProductVersion;
import com.coretool.param.domain.config.version.repository.ProductVersionRepository;
import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

/** 产品版本领域服务：封装唯一性、归属校验、状态变更等规则。 */
public class ProductVersionDomainService {

    public record CreateCommand(
            String productId,
            String versionId,
            String versionName,
            String versionType,
            String versionDescription,
            String baselineVersionId,
            String baselineVersionName,
            String versionDesc,
            String approver,
            String isHidden,
            String supportedVersion,
            String introducedProductId,
            String ownerList,
            Integer versionStatus,
            String creatorId,
            String updaterId,
            LocalDateTime now) {}

    public record BranchFromBaselineCommand(
            String productId,
            String baseVersionId,
            String newVersionId,
            String newVersionName,
            String supportedVersionOverride,
            String descriptionOverride,
            LocalDateTime now) {}

    public record UpdateCommand(
            String productId,
            String versionId,
            String versionName,
            String supportedVersion,
            String versionDescription,
            String versionDesc,
            Integer versionStatus,
            String updaterId,
            LocalDateTime now) {}

    private final ProductVersionRepository repository;

    /**
     * 创建产品版本领域服务。
     *
     * @param repository 产品版本仓储
     */
    public ProductVersionDomainService(ProductVersionRepository repository) {
        this.repository = repository;
    }

    /**
     * 创建新产品版本（含同产品下版本名称唯一性校验）。
     *
     * @param input 创建入参，各字段与 {@link CreateCommand} 记录组件一一对应
     * @return 新产品版本
     */
    public ProductVersion createNew(CreateCommand input) {
        if (input == null) {
            throw new DomainRuleException("创建参数不能为空");
        }
        if (repository.existsSameNameInProduct(input.productId(), input.versionName(), null)) {
            throw new DomainRuleException("VERSION_NAME_DUPLICATE: 同一产品下版本名称已存在");
        }
        return ProductVersion.registerNew(
                new ProductVersion.Registration(
                        input.productId(),
                        input.versionId(),
                        input.versionName(),
                        input.versionType(),
                        input.versionDescription(),
                        input.baselineVersionId(),
                        input.baselineVersionName(),
                        input.versionDesc(),
                        input.approver(),
                        input.isHidden(),
                        input.supportedVersion(),
                        input.introducedProductId(),
                        input.ownerList(),
                        input.versionStatus(),
                        input.creatorId(),
                        input.updaterId(),
                        input.now()));
    }

    /**
     * 基于基线版本拉分支创建新版本（含同产品下版本名称唯一性校验）。
     *
     * @param input 拉分支入参，各字段与 {@link BranchFromBaselineCommand} 记录组件一一对应
     * @return 新版本
     */
    public ProductVersion branchFromBaseline(BranchFromBaselineCommand input) {
        if (input == null) {
            throw new DomainRuleException("拉分支参数不能为空");
        }
        ProductVersion base =
                repository.findById(input.baseVersionId()).orElseThrow(() -> new DomainRuleException("基线版本不存在"));
        ProductVersion neo =
                ProductVersion.branchFromBaseline(
                        new ProductVersion.BranchFromBaselineCommand(
                                base,
                                input.productId(),
                                input.newVersionId(),
                                input.newVersionName(),
                                input.supportedVersionOverride(),
                                input.descriptionOverride(),
                                input.now()));
        if (repository.existsSameNameInProduct(input.productId(), neo.getVersionName(), null)) {
            throw new DomainRuleException("VERSION_NAME_DUPLICATE: 同一产品下版本名称已存在");
        }
        return neo;
    }

    /**
     * 更新既有产品版本（含归属校验与同产品下名称唯一性校验）。
     *
     * @param input 更新入参，各字段与 {@link UpdateCommand} 记录组件一一对应
     * @return 更新后的产品版本
     */
    public ProductVersion updateExisting(UpdateCommand input) {
        if (input == null) {
            throw new DomainRuleException("更新参数不能为空");
        }
        ProductVersion existing = requireOwned(input.productId(), input.versionId());
        if (StringUtils.isNotBlank(input.versionName())
                && !StringUtils.equals(input.versionName(), existing.getVersionName())) {
            if (repository.existsSameNameInProduct(input.productId(), input.versionName(), input.versionId())) {
                throw new DomainRuleException("VERSION_NAME_DUPLICATE: 同一产品下版本名称已存在");
            }
            existing.rename(input.versionName());
        }
        existing.applyAttributePatch(
                new ProductVersion.AttributePatch(
                        input.supportedVersion(),
                        input.versionDescription(),
                        input.versionDesc(),
                        input.versionStatus(),
                        input.updaterId(),
                        input.now()));
        return existing;
    }

    /**
     * 禁用产品版本（含归属校验）。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @param now       当前时间
     * @return 禁用后的产品版本
     */
    public ProductVersion disable(String productId, String versionId, LocalDateTime now) {
        ProductVersion existing = requireOwned(productId, versionId);
        existing.disable(now);
        return existing;
    }

    /**
     * 获取并校验产品版本归属产品（不存在或不归属则抛出领域异常）。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @return 产品版本
     */
    public ProductVersion requireOwned(String productId, String versionId) {
        ProductVersion existing = repository.findById(versionId).orElseThrow(() -> new DomainRuleException("版本不存在"));
        if (!existing.belongsToProduct(productId)) {
            throw new DomainRuleException("版本不属于该产品");
        }
        return existing;
    }
}

