package com.coretool.param.domain.config.category;

import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

/** 业务分类（entity_business_category）；同一产品下分类中文名唯一。 */
public class BusinessCategory {

    public record Registration(
            String ownedProductId,
            String categoryId,
            String categoryNameCn,
            String categoryNameEn,
            String featureRange,
            String categoryType,
            Integer categoryStatus,
            String creatorId,
            String updaterId,
            LocalDateTime now) {}

    public record Snapshot(
            String ownedProductId,
            String categoryId,
            String categoryNameCn,
            String categoryNameEn,
            String featureRange,
            String categoryType,
            Integer categoryStatus,
            String creatorId,
            LocalDateTime creationTimestamp,
            String updaterId,
            LocalDateTime updateTimestamp) {}

    public record EditablePatch(
            String categoryNameCn,
            String categoryNameEn,
            String featureRange,
            String categoryType,
            Integer categoryStatus,
            String updaterId,
            LocalDateTime now) {}

    private String ownedProductId;
    private String categoryId;
    private String categoryNameCn;
    private String categoryNameEn;
    private String featureRange;
    private String categoryType;
    private int categoryStatus;
    private String creatorId;
    private LocalDateTime creationTimestamp;
    private String updaterId;
    private LocalDateTime updateTimestamp;

    /**
     * 注册新业务分类（创建聚合/实体）。
     *
     * @param input 注册入参，各字段与 {@link Registration} 记录组件一一对应
     * @return 新业务分类
     */
    public static BusinessCategory registerNew(Registration input) {
        if (input == null) {
            throw new DomainRuleException("注册参数不能为空");
        }
        if (StringUtils.isAnyBlank(input.categoryId(), input.categoryNameCn(), input.categoryNameEn())) {
            throw new DomainRuleException("分类ID与中英文名称不能为空");
        }
        BusinessCategory b = new BusinessCategory();
        b.ownedProductId = Objects.requireNonNull(input.ownedProductId());
        b.categoryId = input.categoryId().trim();
        b.categoryNameCn = input.categoryNameCn().trim();
        b.categoryNameEn = input.categoryNameEn().trim();
        b.featureRange = input.featureRange();
        b.categoryType = input.categoryType();
        b.categoryStatus = input.categoryStatus() == null ? 1 : input.categoryStatus();
        String c = StringUtils.defaultIfBlank(input.creatorId(), "system");
        b.creatorId = c;
        b.creationTimestamp = input.now();
        b.updaterId = StringUtils.defaultIfBlank(input.updaterId(), c);
        b.updateTimestamp = input.now();
        return b;
    }

    /**
     * 从持久化数据重建业务分类（rehydrate）。
     *
     * @param input 持久化快照，各字段与 {@link Snapshot} 记录组件一一对应
     * @return 重建后的业务分类
     */
    public static BusinessCategory rehydrate(Snapshot input) {
        if (input == null) {
            return null;
        }
        BusinessCategory b = new BusinessCategory();
        b.ownedProductId = input.ownedProductId();
        b.categoryId = input.categoryId();
        b.categoryNameCn = input.categoryNameCn();
        b.categoryNameEn = input.categoryNameEn();
        b.featureRange = input.featureRange();
        b.categoryType = input.categoryType();
        b.categoryStatus = input.categoryStatus() == null ? 1 : input.categoryStatus();
        b.creatorId = input.creatorId();
        b.creationTimestamp = input.creationTimestamp();
        b.updaterId = input.updaterId();
        b.updateTimestamp = input.updateTimestamp();
        return b;
    }

    /**
     * 应用可编辑字段的局部更新。
     *
     * @param patch 可编辑变更，各字段与 {@link EditablePatch} 记录组件一一对应
     */
    public void applyEditablePatch(EditablePatch patch) {
        if (patch == null) {
            return;
        }
        if (patch.categoryNameCn() != null && StringUtils.isNotBlank(patch.categoryNameCn())) {
            this.categoryNameCn = patch.categoryNameCn().trim();
        }
        if (patch.categoryNameEn() != null && StringUtils.isNotBlank(patch.categoryNameEn())) {
            this.categoryNameEn = patch.categoryNameEn().trim();
        }
        if (patch.featureRange() != null) {
            this.featureRange = patch.featureRange();
        }
        if (patch.categoryType() != null) {
            this.categoryType = patch.categoryType();
        }
        if (patch.categoryStatus() != null) {
            this.categoryStatus = patch.categoryStatus();
        }
        if (StringUtils.isNotBlank(patch.updaterId())) {
            this.updaterId = patch.updaterId();
        }
        this.updateTimestamp = patch.now();
    }

    /**
     * 将业务分类置为禁用状态。
     *
     * @param now 当前时间
     */
    public void disable(LocalDateTime now) {
        this.categoryStatus = 0;
        this.updateTimestamp = now;
    }

    /**
     * 判断分类是否归属指定产品。
     *
     * @param productId 产品 ID
     * @return 是否归属该产品
     */
    public boolean belongsToProduct(String productId) {
        return Objects.equals(this.ownedProductId, productId);
    }

    /**
     * 获取归属产品 ID。
     *
     * @return 归属产品 ID
     */
    public String getOwnedProductId() {
        return ownedProductId;
    }

    /**
     * 获取分类 ID。
     *
     * @return 分类 ID
     */
    public String getCategoryId() {
        return categoryId;
    }

    /**
     * 获取分类中文名。
     *
     * @return 分类中文名
     */
    public String getCategoryNameCn() {
        return categoryNameCn;
    }

    /**
     * 获取分类英文名。
     *
     * @return 分类英文名
     */
    public String getCategoryNameEn() {
        return categoryNameEn;
    }

    /**
     * 获取特性范围。
     *
     * @return 特性范围
     */
    public String getFeatureRange() {
        return featureRange;
    }

    /**
     * 获取分类类型。
     *
     * @return 分类类型
     */
    public String getCategoryType() {
        return categoryType;
    }

    /**
     * 获取分类状态。
     *
     * @return 分类状态
     */
    public int getCategoryStatus() {
        return categoryStatus;
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
