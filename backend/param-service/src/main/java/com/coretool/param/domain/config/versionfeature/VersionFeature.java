package com.coretool.param.domain.config.versionfeature;

import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

/** 版本特性（version_feature_dict） */
public class VersionFeature {

    public record Registration(
            String ownedProductPbiId,
            String ownedVersionId,
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

    public record Snapshot(
            String ownedProductPbiId,
            String ownedVersionId,
            String featureId,
            String featureCode,
            String featureNameCn,
            String featureNameEn,
            String introduceType,
            String inheritReferenceVersionId,
            Integer featureStatus,
            String creatorId,
            LocalDateTime creationTimestamp,
            String updaterId,
            LocalDateTime updateTimestamp) {}

    public record Patch(
            String featureCode,
            String featureNameCn,
            String featureNameEn,
            String introduceType,
            String inheritReferenceVersionId,
            Integer featureStatus,
            String updaterId,
            LocalDateTime now) {}

    private String ownedProductPbiId;
    private String ownedVersionId;
    private String featureId;
    private String featureCode;
    private String featureNameCn;
    private String featureNameEn;
    private String introduceType;
    private String inheritReferenceVersionId;
    private int featureStatus;
    private String creatorId;
    private LocalDateTime creationTimestamp;
    private String updaterId;
    private LocalDateTime updateTimestamp;

    /**
     * 注册新版本特性（创建聚合/实体）。
     *
     * @param input 注册入参，各字段与 {@link Registration} 记录组件一一对应
     * @return 新版本特性
     */
    public static VersionFeature registerNew(Registration input) {
        if (input == null) {
            throw new DomainRuleException("注册参数不能为空");
        }
        if (StringUtils.isAnyBlank(
                input.featureId(),
                input.featureCode(),
                input.featureNameCn(),
                input.featureNameEn(),
                input.introduceType())) {
            throw new DomainRuleException("特性ID、特性编码、中英文名称、引入类型不能为空");
        }
        VersionFeature f = new VersionFeature();
        f.ownedProductPbiId = Objects.requireNonNull(input.ownedProductPbiId());
        f.ownedVersionId = Objects.requireNonNull(input.ownedVersionId());
        f.featureId = input.featureId().trim();
        f.featureCode = input.featureCode().trim();
        f.featureNameCn = input.featureNameCn().trim();
        f.featureNameEn = input.featureNameEn().trim();
        f.introduceType = input.introduceType().trim();
        f.inheritReferenceVersionId = input.inheritReferenceVersionId();
        f.featureStatus = input.featureStatus() == null ? 1 : input.featureStatus();
        String c = StringUtils.defaultIfBlank(input.creatorId(), "system");
        f.creatorId = c;
        f.creationTimestamp = input.now();
        f.updaterId = StringUtils.defaultIfBlank(input.updaterId(), c);
        f.updateTimestamp = input.now();
        return f;
    }

    /**
     * 从持久化数据重建版本特性（rehydrate）。
     *
     * @param input 持久化快照，各字段与 {@link Snapshot} 记录组件一一对应
     * @return 重建后的版本特性
     */
    public static VersionFeature rehydrate(Snapshot input) {
        if (input == null) {
            return null;
        }
        VersionFeature f = new VersionFeature();
        f.ownedProductPbiId = input.ownedProductPbiId();
        f.ownedVersionId = input.ownedVersionId();
        f.featureId = input.featureId();
        f.featureCode = input.featureCode();
        f.featureNameCn = input.featureNameCn();
        f.featureNameEn = input.featureNameEn();
        f.introduceType = input.introduceType();
        f.inheritReferenceVersionId = input.inheritReferenceVersionId();
        f.featureStatus = input.featureStatus() == null ? 1 : input.featureStatus();
        f.creatorId = input.creatorId();
        f.creationTimestamp = input.creationTimestamp();
        f.updaterId = input.updaterId();
        f.updateTimestamp = input.updateTimestamp();
        return f;
    }

    /**
     * 重命名特性中文名。
     *
     * @param newNameCn 新中文名
     */
    public void renameFeatureNameCn(String newNameCn) {
        if (StringUtils.isBlank(newNameCn)) {
            throw new DomainRuleException("特性中文名不能为空");
        }
        this.featureNameCn = newNameCn.trim();
    }

    /**
     * 应用可编辑字段的局部更新。
     *
     * @param patch 局部变更，各字段与 {@link Patch} 记录组件一一对应
     */
    public void applyPatch(Patch patch) {
        if (patch == null) {
            return;
        }
        if (StringUtils.isNotBlank(patch.featureCode())) {
            this.featureCode = patch.featureCode().trim();
        }
        if (patch.featureNameCn() != null && StringUtils.isNotBlank(patch.featureNameCn())) {
            renameFeatureNameCn(patch.featureNameCn());
        }
        if (StringUtils.isNotBlank(patch.featureNameEn())) {
            this.featureNameEn = patch.featureNameEn().trim();
        }
        if (StringUtils.isNotBlank(patch.introduceType())) {
            this.introduceType = patch.introduceType().trim();
        }
        if (patch.inheritReferenceVersionId() != null) {
            this.inheritReferenceVersionId = patch.inheritReferenceVersionId();
        }
        if (patch.featureStatus() != null) {
            this.featureStatus = patch.featureStatus();
        }
        if (StringUtils.isNotBlank(patch.updaterId())) {
            this.updaterId = patch.updaterId();
        }
        this.updateTimestamp = patch.now();
    }

    /**
     * 将版本特性置为禁用状态。
     *
     * @param now 当前时间
     */
    public void disable(LocalDateTime now) {
        this.featureStatus = 0;
        this.updateTimestamp = now;
    }

    /**
     * 仅刷新更新时间（例如无请求体时的幂等更新）。
     *
     * @param now 当前时间
     */
    public void touch(LocalDateTime now) {
        this.updateTimestamp = now;
    }

    /**
     * 判断特性是否归属指定产品与版本。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @return 是否归属该产品与版本
     */
    public boolean belongsToProductAndVersion(String productId, String versionId) {
        return Objects.equals(this.ownedProductPbiId, productId)
                && Objects.equals(this.ownedVersionId, versionId);
    }

    /**
     * 获取归属产品 ID。
     *
     * @return 归属产品 ID
     */
    public String getOwnedProductPbiId() {
        return ownedProductPbiId;
    }

    /**
     * 获取归属版本 ID。
     *
     * @return 归属版本 ID
     */
    public String getOwnedVersionId() {
        return ownedVersionId;
    }

    /**
     * 获取特性 ID。
     *
     * @return 特性 ID
     */
    public String getFeatureId() {
        return featureId;
    }

    /**
     * 获取特性编码。
     *
     * @return 特性编码
     */
    public String getFeatureCode() {
        return featureCode;
    }

    /**
     * 获取特性中文名。
     *
     * @return 特性中文名
     */
    public String getFeatureNameCn() {
        return featureNameCn;
    }

    /**
     * 获取特性英文名。
     *
     * @return 特性英文名
     */
    public String getFeatureNameEn() {
        return featureNameEn;
    }

    /**
     * 获取引入类型。
     *
     * @return 引入类型
     */
    public String getIntroduceType() {
        return introduceType;
    }

    /**
     * 获取继承参考版本 ID。
     *
     * @return 继承参考版本 ID
     */
    public String getInheritReferenceVersionId() {
        return inheritReferenceVersionId;
    }

    /**
     * 获取特性状态。
     *
     * @return 特性状态
     */
    public int getFeatureStatus() {
        return featureStatus;
    }

    /**
     * 获取创建人 ID。
     *
     * @return 创建人 ID
     */
    public String getCreatorId() {
        return creatorId;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreationTimestamp() {
        return creationTimestamp;
    }

    /**
     * 获取更新人 ID。
     *
     * @return 更新人 ID
     */
    public String getUpdaterId() {
        return updaterId;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdateTimestamp() {
        return updateTimestamp;
    }
}
