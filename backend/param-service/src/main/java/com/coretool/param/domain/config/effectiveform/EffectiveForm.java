package com.coretool.param.domain.config.effectiveform;

import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

/** 生效形态（entity_effective_form_dict） */
public class EffectiveForm {

    public record Registration(
            String ownedProductId,
            String effectiveFormId,
            String effectiveFormNameCn,
            String effectiveFormNameEn,
            String effectiveFormDescription,
            Integer effectiveFormStatus,
            String creatorId,
            String updaterId,
            LocalDateTime now) {}

    public record Snapshot(
            String ownedProductId,
            String effectiveFormId,
            String effectiveFormNameCn,
            String effectiveFormNameEn,
            String effectiveFormDescription,
            Integer effectiveFormStatus,
            String creatorId,
            LocalDateTime creationTimestamp,
            String updaterId,
            LocalDateTime updateTimestamp) {}

    public record Patch(
            String nameCn,
            String nameEn,
            String description,
            Integer status,
            String updaterId,
            LocalDateTime now) {}

    private String ownedProductId;
    private String effectiveFormId;
    private String effectiveFormNameCn;
    private String effectiveFormNameEn;
    private String effectiveFormDescription;
    private int effectiveFormStatus;
    private String creatorId;
    private LocalDateTime creationTimestamp;
    private String updaterId;
    private LocalDateTime updateTimestamp;

    /**
     * 注册新生效形态（创建聚合/实体）。
     *
     * @param input 注册入参，各字段与 {@link Registration} 记录组件一一对应
     * @return 新生效形态
     */
    public static EffectiveForm registerNew(Registration input) {
        if (input == null) {
            throw new DomainRuleException("注册参数不能为空");
        }
        if (StringUtils.isBlank(input.effectiveFormId())) {
            throw new DomainRuleException("生效形态ID不能为空");
        }
        EffectiveForm f = new EffectiveForm();
        f.ownedProductId = Objects.requireNonNull(input.ownedProductId());
        f.effectiveFormId = input.effectiveFormId().trim();
        f.effectiveFormNameCn = StringUtils.trimToNull(input.effectiveFormNameCn());
        f.effectiveFormNameEn = StringUtils.trimToNull(input.effectiveFormNameEn());
        f.assertBilingualFilled();
        f.effectiveFormDescription = input.effectiveFormDescription();
        f.effectiveFormStatus = input.effectiveFormStatus() == null ? 1 : input.effectiveFormStatus();
        String c = StringUtils.defaultIfBlank(input.creatorId(), "system");
        f.creatorId = c;
        f.creationTimestamp = input.now();
        f.updaterId = StringUtils.defaultIfBlank(input.updaterId(), c);
        f.updateTimestamp = input.now();
        return f;
    }

    /**
     * 从持久化数据重建生效形态（rehydrate）。
     *
     * @param input 持久化快照，各字段与 {@link Snapshot} 记录组件一一对应
     * @return 重建后的生效形态
     */
    public static EffectiveForm rehydrate(Snapshot input) {
        if (input == null) {
            return null;
        }
        EffectiveForm f = new EffectiveForm();
        f.ownedProductId = input.ownedProductId();
        f.effectiveFormId = input.effectiveFormId();
        f.effectiveFormNameCn = input.effectiveFormNameCn();
        f.effectiveFormNameEn = input.effectiveFormNameEn();
        f.effectiveFormDescription = input.effectiveFormDescription();
        f.effectiveFormStatus = input.effectiveFormStatus() == null ? 1 : input.effectiveFormStatus();
        f.creatorId = input.creatorId();
        f.creationTimestamp = input.creationTimestamp();
        f.updaterId = input.updaterId();
        f.updateTimestamp = input.updateTimestamp();
        return f;
    }

    /**
     * 应用可编辑字段的局部更新（更新后校验中英文名称必填）。
     *
     * @param patch 局部变更，各字段与 {@link Patch} 记录组件一一对应
     */
    public void applyPatch(Patch patch) {
        if (patch == null) {
            return;
        }
        if (StringUtils.isNotBlank(patch.nameCn())) {
            this.effectiveFormNameCn = patch.nameCn().trim();
        }
        if (StringUtils.isNotBlank(patch.nameEn())) {
            this.effectiveFormNameEn = patch.nameEn().trim();
        }
        if (patch.description() != null) {
            this.effectiveFormDescription = patch.description();
        }
        if (patch.status() != null) {
            this.effectiveFormStatus = patch.status();
        }
        if (StringUtils.isNotBlank(patch.updaterId())) {
            this.updaterId = patch.updaterId();
        }
        this.updateTimestamp = patch.now();
        assertBilingualFilled();
    }

    private void assertBilingualFilled() {
        if (StringUtils.isBlank(effectiveFormNameCn) || StringUtils.isBlank(effectiveFormNameEn)) {
            throw new DomainRuleException("生效形态中英文名称必填");
        }
    }

    /**
     * 将生效形态置为禁用状态。
     *
     * @param now 当前时间
     */
    public void disable(LocalDateTime now) {
        this.effectiveFormStatus = 0;
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
     * 判断生效形态是否归属指定产品。
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
     * 获取生效形态 ID。
     *
     * @return 生效形态 ID
     */
    public String getEffectiveFormId() {
        return effectiveFormId;
    }

    /**
     * 获取生效形态中文名。
     *
     * @return 生效形态中文名
     */
    public String getEffectiveFormNameCn() {
        return effectiveFormNameCn;
    }

    /**
     * 获取生效形态英文名。
     *
     * @return 生效形态英文名
     */
    public String getEffectiveFormNameEn() {
        return effectiveFormNameEn;
    }

    /**
     * 获取生效形态描述。
     *
     * @return 描述
     */
    public String getEffectiveFormDescription() {
        return effectiveFormDescription;
    }

    /**
     * 获取生效形态状态。
     *
     * @return 状态
     */
    public int getEffectiveFormStatus() {
        return effectiveFormStatus;
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
