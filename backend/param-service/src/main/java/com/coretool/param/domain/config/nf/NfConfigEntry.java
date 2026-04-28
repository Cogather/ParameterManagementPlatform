package com.coretool.param.domain.config.nf;

import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

/** NF 配置字典（entity_nf_config_dict） */
public class NfConfigEntry {

    public record Registration(
            String ownedProductId,
            String nfConfigId,
            String nfConfigNameCn,
            String nfConfigDescription,
            Integer nfConfigStatus,
            String creatorId,
            String updaterId,
            LocalDateTime now) {}

    public record Snapshot(
            String ownedProductId,
            String nfConfigId,
            String nfConfigNameCn,
            String nfConfigDescription,
            Integer nfConfigStatus,
            String creatorId,
            LocalDateTime creationTimestamp,
            String updaterId,
            LocalDateTime updateTimestamp) {}

    public record Patch(
            String nfConfigNameCn,
            String nfConfigDescription,
            Integer nfConfigStatus,
            String updaterId,
            LocalDateTime now) {}

    private String ownedProductId;
    private String nfConfigId;
    private String nfConfigNameCn;
    private String nfConfigDescription;
    private int nfConfigStatus;
    private String creatorId;
    private LocalDateTime creationTimestamp;
    private String updaterId;
    private LocalDateTime updateTimestamp;

    /**
     * 注册新的 NF 配置条目（创建聚合/实体）。
     *
     * @param input 注册入参，各字段与 {@link Registration} 记录组件一一对应
     * @return 新 NF 配置条目
     */
    public static NfConfigEntry registerNew(Registration input) {
        if (input == null) {
            throw new DomainRuleException("注册参数不能为空");
        }
        if (StringUtils.isAnyBlank(input.nfConfigId(), input.nfConfigNameCn())) {
            throw new DomainRuleException("NF ID 与名称不能为空");
        }
        NfConfigEntry e = new NfConfigEntry();
        e.ownedProductId = Objects.requireNonNull(input.ownedProductId());
        e.nfConfigId = input.nfConfigId().trim();
        e.nfConfigNameCn = input.nfConfigNameCn().trim();
        e.nfConfigDescription = input.nfConfigDescription();
        e.nfConfigStatus = input.nfConfigStatus() == null ? 1 : input.nfConfigStatus();
        String c = StringUtils.defaultIfBlank(input.creatorId(), "system");
        e.creatorId = c;
        e.creationTimestamp = input.now();
        e.updaterId = StringUtils.defaultIfBlank(input.updaterId(), c);
        e.updateTimestamp = input.now();
        return e;
    }

    /**
     * 从持久化数据重建 NF 配置条目（rehydrate）。
     *
     * @param input 持久化快照，各字段与 {@link Snapshot} 记录组件一一对应
     * @return 重建后的 NF 配置条目
     */
    public static NfConfigEntry rehydrate(Snapshot input) {
        if (input == null) {
            return null;
        }
        NfConfigEntry e = new NfConfigEntry();
        e.ownedProductId = input.ownedProductId();
        e.nfConfigId = input.nfConfigId();
        e.nfConfigNameCn = input.nfConfigNameCn();
        e.nfConfigDescription = input.nfConfigDescription();
        e.nfConfigStatus = input.nfConfigStatus() == null ? 1 : input.nfConfigStatus();
        e.creatorId = input.creatorId();
        e.creationTimestamp = input.creationTimestamp();
        e.updaterId = input.updaterId();
        e.updateTimestamp = input.updateTimestamp();
        return e;
    }

    /**
     * 重命名 NF 配置中文名。
     *
     * @param newName 新名称
     */
    public void renameTo(String newName) {
        if (StringUtils.isBlank(newName)) {
            throw new DomainRuleException("NF 名称不能为空");
        }
        this.nfConfigNameCn = newName.trim();
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
        if (patch.nfConfigNameCn() != null && StringUtils.isNotBlank(patch.nfConfigNameCn())) {
            renameTo(patch.nfConfigNameCn());
        }
        if (patch.nfConfigDescription() != null) {
            this.nfConfigDescription = patch.nfConfigDescription();
        }
        if (patch.nfConfigStatus() != null) {
            this.nfConfigStatus = patch.nfConfigStatus();
        }
        if (StringUtils.isNotBlank(patch.updaterId())) {
            this.updaterId = patch.updaterId();
        }
        this.updateTimestamp = patch.now();
    }

    /**
     * 将 NF 配置条目置为禁用状态。
     *
     * @param now 当前时间
     */
    public void disable(LocalDateTime now) {
        this.nfConfigStatus = 0;
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
     * 判断 NF 配置条目是否归属指定产品。
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
     * 获取 NF 配置 ID。
     *
     * @return NF 配置 ID
     */
    public String getNfConfigId() {
        return nfConfigId;
    }

    /**
     * 获取 NF 配置中文名。
     *
     * @return NF 配置中文名
     */
    public String getNfConfigNameCn() {
        return nfConfigNameCn;
    }

    /**
     * 获取描述。
     *
     * @return 描述
     */
    public String getNfConfigDescription() {
        return nfConfigDescription;
    }

    /**
     * 获取 NF 配置状态。
     *
     * @return 状态
     */
    public int getNfConfigStatus() {
        return nfConfigStatus;
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
