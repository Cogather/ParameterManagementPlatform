package com.coretool.param.domain.config.ne;

import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

/** 适用网元字典（entity_applicable_ne_dict） */
public class ApplicableNe {

    public record Registration(
            String ownedProductId,
            String neTypeId,
            String neTypeNameCn,
            String neTypeDescription,
            Integer neTypeStatus,
            String productForm,
            String creatorId,
            String updaterId,
            LocalDateTime now) {}

    public record Snapshot(
            String ownedProductId,
            String neTypeId,
            String neTypeNameCn,
            String neTypeDescription,
            Integer neTypeStatus,
            String productForm,
            String creatorId,
            LocalDateTime creationTimestamp,
            String updaterId,
            LocalDateTime updateTimestamp) {}

    public record Patch(
            String neTypeNameCn,
            String neTypeDescription,
            Integer neTypeStatus,
            String productForm,
            String updaterId,
            LocalDateTime now) {}

    private String ownedProductId;
    private String neTypeId;
    private String neTypeNameCn;
    private String neTypeDescription;
    private int neTypeStatus;
    private String productForm;
    private String creatorId;
    private LocalDateTime creationTimestamp;
    private String updaterId;
    private LocalDateTime updateTimestamp;

    /**
     * 注册新的适用网元字典条目（创建聚合/实体）。
     *
     * @param input 注册入参，各字段与 {@link Registration} 记录组件一一对应
     * @return 新适用网元
     */
    public static ApplicableNe registerNew(Registration input) {
        if (input == null) {
            throw new DomainRuleException("注册参数不能为空");
        }
        if (StringUtils.isAnyBlank(input.neTypeId(), input.neTypeNameCn())) {
            throw new DomainRuleException("网元ID与名称不能为空");
        }
        ApplicableNe n = new ApplicableNe();
        n.ownedProductId = Objects.requireNonNull(input.ownedProductId());
        n.neTypeId = input.neTypeId().trim();
        n.neTypeNameCn = input.neTypeNameCn().trim();
        n.neTypeDescription = input.neTypeDescription();
        n.neTypeStatus = input.neTypeStatus() == null ? 1 : input.neTypeStatus();
        n.productForm = input.productForm();
        String c = StringUtils.defaultIfBlank(input.creatorId(), "system");
        n.creatorId = c;
        n.creationTimestamp = input.now();
        n.updaterId = StringUtils.defaultIfBlank(input.updaterId(), c);
        n.updateTimestamp = input.now();
        return n;
    }

    /**
     * 从持久化数据重建适用网元条目（rehydrate）。
     *
     * @param input 持久化快照，各字段与 {@link Snapshot} 记录组件一一对应
     * @return 重建后的适用网元
     */
    public static ApplicableNe rehydrate(Snapshot input) {
        if (input == null) {
            return null;
        }
        ApplicableNe n = new ApplicableNe();
        n.ownedProductId = input.ownedProductId();
        n.neTypeId = input.neTypeId();
        n.neTypeNameCn = input.neTypeNameCn();
        n.neTypeDescription = input.neTypeDescription();
        n.neTypeStatus = input.neTypeStatus() == null ? 1 : input.neTypeStatus();
        n.productForm = input.productForm();
        n.creatorId = input.creatorId();
        n.creationTimestamp = input.creationTimestamp();
        n.updaterId = input.updaterId();
        n.updateTimestamp = input.updateTimestamp();
        return n;
    }

    /**
     * 重命名网元中文名。
     *
     * @param newName 新名称
     */
    public void renameTo(String newName) {
        if (StringUtils.isBlank(newName)) {
            throw new DomainRuleException("网元名称不能为空");
        }
        this.neTypeNameCn = newName.trim();
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
        if (patch.neTypeNameCn() != null && StringUtils.isNotBlank(patch.neTypeNameCn())) {
            renameTo(patch.neTypeNameCn());
        }
        if (patch.neTypeDescription() != null) {
            this.neTypeDescription = patch.neTypeDescription();
        }
        if (patch.neTypeStatus() != null) {
            this.neTypeStatus = patch.neTypeStatus();
        }
        if (patch.productForm() != null) {
            this.productForm = patch.productForm();
        }
        if (StringUtils.isNotBlank(patch.updaterId())) {
            this.updaterId = patch.updaterId();
        }
        this.updateTimestamp = patch.now();
    }

    /**
     * 将网元条目置为禁用状态。
     *
     * @param now 当前时间
     */
    public void disable(LocalDateTime now) {
        this.neTypeStatus = 0;
        this.updateTimestamp = now;
    }

    /**
     * 仅刷新更新时间（用于无业务字段变更的更新场景）。
     *
     * @param now 当前时间
     */
    public void touch(LocalDateTime now) {
        this.updateTimestamp = now;
    }

    /**
     * 判断网元条目是否归属指定产品。
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
     * 获取网元类型 ID。
     *
     * @return 网元类型 ID
     */
    public String getNeTypeId() {
        return neTypeId;
    }

    /**
     * 获取网元中文名。
     *
     * @return 网元中文名
     */
    public String getNeTypeNameCn() {
        return neTypeNameCn;
    }

    /**
     * 获取网元描述。
     *
     * @return 网元描述
     */
    public String getNeTypeDescription() {
        return neTypeDescription;
    }

    /**
     * 获取网元状态。
     *
     * @return 网元状态
     */
    public int getNeTypeStatus() {
        return neTypeStatus;
    }

    /**
     * 获取产品形态。
     *
     * @return 产品形态
     */
    public String getProductForm() {
        return productForm;
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
