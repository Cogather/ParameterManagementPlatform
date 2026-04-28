package com.coretool.param.domain.config.effectivemode;

import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

/** 生效方式（entity_effective_mode_dict） */
public class EffectiveMode {

    public record Registration(
            String ownedProductId,
            String effectiveModeId,
            String effectiveModeNameCn,
            String effectiveModeNameEn,
            String effectiveModeDescription,
            Integer effectiveModeStatus,
            String creatorId,
            String updaterId,
            LocalDateTime now) {}

    public record Snapshot(
            String ownedProductId,
            String effectiveModeId,
            String effectiveModeNameCn,
            String effectiveModeNameEn,
            String effectiveModeDescription,
            Integer effectiveModeStatus,
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
    private String effectiveModeId;
    private String effectiveModeNameCn;
    private String effectiveModeNameEn;
    private String effectiveModeDescription;
    private int effectiveModeStatus;
    private String creatorId;
    private LocalDateTime creationTimestamp;
    private String updaterId;
    private LocalDateTime updateTimestamp;

    /**
     * 注册新生效方式（创建聚合/实体）。
     *
     * @param input 注册入参，各字段与 {@link Registration} 记录组件一一对应
     * @return 新生效方式
     */
    public static EffectiveMode registerNew(Registration input) {
        if (input == null) {
            throw new DomainRuleException("注册参数不能为空");
        }
        if (StringUtils.isBlank(input.effectiveModeId())) {
            throw new DomainRuleException("生效方式ID不能为空");
        }
        EffectiveMode m = new EffectiveMode();
        m.ownedProductId = Objects.requireNonNull(input.ownedProductId());
        m.effectiveModeId = input.effectiveModeId().trim();
        m.effectiveModeNameCn = StringUtils.trimToNull(input.effectiveModeNameCn());
        m.effectiveModeNameEn = StringUtils.trimToNull(input.effectiveModeNameEn());
        m.assertBilingualFilled();
        m.effectiveModeDescription = input.effectiveModeDescription();
        m.effectiveModeStatus = input.effectiveModeStatus() == null ? 1 : input.effectiveModeStatus();
        String c = StringUtils.defaultIfBlank(input.creatorId(), "system");
        m.creatorId = c;
        m.creationTimestamp = input.now();
        m.updaterId = StringUtils.defaultIfBlank(input.updaterId(), c);
        m.updateTimestamp = input.now();
        return m;
    }

    /**
     * 从持久化数据重建生效方式（rehydrate）。
     *
     * @param input 持久化快照，各字段与 {@link Snapshot} 记录组件一一对应
     * @return 重建后的生效方式
     */
    public static EffectiveMode rehydrate(Snapshot input) {
        if (input == null) {
            return null;
        }
        EffectiveMode m = new EffectiveMode();
        m.ownedProductId = input.ownedProductId();
        m.effectiveModeId = input.effectiveModeId();
        m.effectiveModeNameCn = input.effectiveModeNameCn();
        m.effectiveModeNameEn = input.effectiveModeNameEn();
        m.effectiveModeDescription = input.effectiveModeDescription();
        m.effectiveModeStatus = input.effectiveModeStatus() == null ? 1 : input.effectiveModeStatus();
        m.creatorId = input.creatorId();
        m.creationTimestamp = input.creationTimestamp();
        m.updaterId = input.updaterId();
        m.updateTimestamp = input.updateTimestamp();
        return m;
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
            this.effectiveModeNameCn = patch.nameCn().trim();
        }
        if (StringUtils.isNotBlank(patch.nameEn())) {
            this.effectiveModeNameEn = patch.nameEn().trim();
        }
        if (patch.description() != null) {
            this.effectiveModeDescription = patch.description();
        }
        if (patch.status() != null) {
            this.effectiveModeStatus = patch.status();
        }
        if (StringUtils.isNotBlank(patch.updaterId())) {
            this.updaterId = patch.updaterId();
        }
        this.updateTimestamp = patch.now();
        assertBilingualFilled();
    }

    private void assertBilingualFilled() {
        if (StringUtils.isBlank(effectiveModeNameCn) || StringUtils.isBlank(effectiveModeNameEn)) {
            throw new DomainRuleException("EFFECTIVE_MODE_NAME_REQUIRED: 中英文名称必填");
        }
    }

    /**
     * 将生效方式置为禁用状态。
     *
     * @param now 当前时间
     */
    public void disable(LocalDateTime now) {
        this.effectiveModeStatus = 0;
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
     * 判断生效方式是否归属指定产品。
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
     * 获取生效方式 ID。
     *
     * @return 生效方式 ID
     */
    public String getEffectiveModeId() {
        return effectiveModeId;
    }

    /**
     * 获取生效方式中文名。
     *
     * @return 生效方式中文名
     */
    public String getEffectiveModeNameCn() {
        return effectiveModeNameCn;
    }

    /**
     * 获取生效方式英文名。
     *
     * @return 生效方式英文名
     */
    public String getEffectiveModeNameEn() {
        return effectiveModeNameEn;
    }

    /**
     * 获取生效方式描述。
     *
     * @return 描述
     */
    public String getEffectiveModeDescription() {
        return effectiveModeDescription;
    }

    /**
     * 获取生效方式状态。
     *
     * @return 状态
     */
    public int getEffectiveModeStatus() {
        return effectiveModeStatus;
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
