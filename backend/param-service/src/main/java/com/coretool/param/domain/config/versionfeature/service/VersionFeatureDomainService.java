package com.coretool.param.domain.config.versionfeature.service;

import com.coretool.param.domain.config.versionfeature.VersionFeature;
import com.coretool.param.domain.config.versionfeature.repository.VersionFeatureRepository;
import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

/** 版本特性领域服务：封装唯一性、归属校验、状态变更等规则。 */
public class VersionFeatureDomainService {

    public record CreateCommand(
            String productId,
            String versionId,
            String featureId,
            String featureCode,
            String featureNameCn,
            String featureNameEn,
            String introduceType,
            String inheritReferenceVersionId,
            Integer featureStatus,
            String creatorId,
            String updaterId,
            LocalDateTime now) {}

    public record UpdateCommand(
            String productId,
            String versionId,
            String featureId,
            String featureNameCn,
            String featureNameEn,
            String introduceType,
            String inheritReferenceVersionId,
            Integer featureStatus,
            String updaterId,
            LocalDateTime now) {}

    private final VersionFeatureRepository repository;

    /**
     * 创建版本特性领域服务。
     *
     * @param repository 版本特性仓储
     */
    public VersionFeatureDomainService(VersionFeatureRepository repository) {
        this.repository = repository;
    }

    /**
     * 创建新版本特性（含同版本下中文名唯一性校验）。
     *
     * @param input 创建入参，各字段与 {@link CreateCommand} 记录组件一一对应
     * @return 新版本特性
     */
    public VersionFeature createNew(CreateCommand input) {
        if (input == null) {
            throw new DomainRuleException("创建参数不能为空");
        }
        VersionFeature f =
                VersionFeature.registerNew(
                        new VersionFeature.Registration(
                                input.productId(),
                                input.versionId(),
                                input.featureId(),
                                input.featureCode(),
                                input.featureNameCn(),
                                input.featureNameEn(),
                                input.introduceType(),
                                input.inheritReferenceVersionId(),
                                input.featureStatus(),
                                input.creatorId(),
                                input.updaterId(),
                                input.now()));
        if (repository.existsSameFeatureNameCnInScope(input.productId(), input.versionId(), f.getFeatureNameCn(), null)) {
            throw new DomainRuleException("FEATURE_NAME_DUPLICATE: 同一版本下特性中文名已存在");
        }
        return f;
    }

    /**
     * 更新既有版本特性（含归属校验与同版本下中文名唯一性校验；无字段变更则仅刷新更新时间）。
     *
     * @param input 更新入参，各字段与 {@link UpdateCommand} 记录组件一一对应
     * @return 更新后的版本特性
     */
    public VersionFeature updateExisting(UpdateCommand input) {
        if (input == null) {
            throw new DomainRuleException("更新参数不能为空");
        }
        VersionFeature existing = requireOwned(input.productId(), input.versionId(), input.featureId());
        if (StringUtils.isNotBlank(input.featureNameCn())
                && !StringUtils.equals(input.featureNameCn(), existing.getFeatureNameCn())) {
            if (repository.existsSameFeatureNameCnInScope(
                    input.productId(), input.versionId(), input.featureNameCn(), input.featureId())) {
                throw new DomainRuleException("FEATURE_NAME_DUPLICATE: 同一版本下特性中文名已存在");
            }
        }
        if (StringUtils.isNotBlank(input.featureNameCn())
                || StringUtils.isNotBlank(input.featureNameEn())
                || StringUtils.isNotBlank(input.introduceType())
                || input.inheritReferenceVersionId() != null
                || input.featureStatus() != null
                || StringUtils.isNotBlank(input.updaterId())) {
            // featureCode 由后端生成，不作为可编辑字段
            existing.applyPatch(
                    new VersionFeature.Patch(
                            null,
                            input.featureNameCn(),
                            input.featureNameEn(),
                            input.introduceType(),
                            input.inheritReferenceVersionId(),
                            input.featureStatus(),
                            input.updaterId(),
                            input.now()));
        } else {
            existing.touch(input.now());
        }
        return existing;
    }

    /**
     * 禁用版本特性（含归属校验）。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @param featureId 特性 ID
     * @param now       当前时间
     * @return 禁用后的版本特性
     */
    public VersionFeature disable(String productId, String versionId, String featureId, LocalDateTime now) {
        VersionFeature existing = requireOwned(productId, versionId, featureId);
        existing.disable(now);
        return existing;
    }

    /**
     * 获取并校验版本特性归属产品与版本（不存在或不归属则抛出领域异常）。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @param featureId 特性 ID
     * @return 版本特性
     */
    public VersionFeature requireOwned(String productId, String versionId, String featureId) {
        return repository
                .findByFeatureId(featureId)
                .filter(f -> f.belongsToProductAndVersion(productId, versionId))
                .orElseThrow(() -> new DomainRuleException("特性不存在或不属于该产品/版本"));
    }
}

