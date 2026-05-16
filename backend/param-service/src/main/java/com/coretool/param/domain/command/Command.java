/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.domain.command;

import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 产品命令聚合（映射 entity_command_mapping），承载注册、还原与可编辑字段补丁等行为。
 *
 * @since 2026-04-28
 */
public class Command {

    /**
     * 新注册命令时的入参快照（record）。
     *
     * @since 2026-04-28
     */
    public record Registration(
            String ownedProductId,
            String commandId,
            String commandName,
            String creatorId,
            String updaterId,
            String ownerList,
            Integer commandStatus,
            LocalDateTime now) {}

    /**
     * 从持久化或仓储加载命令时的字段快照（record）。
     *
     * @since 2026-04-28
     */
    public record Snapshot(
            String ownedProductId,
            String commandId,
            String commandName,
            String creatorId,
            LocalDateTime creationTimestamp,
            String updaterId,
            LocalDateTime updateTimestamp,
            String ownerList,
            Integer commandStatus) {}

    /**
     * 可编辑字段的增量补丁（record）。
     *
     * @since 2026-04-28
     */
    public record EditablePatch(
            String commandName,
            String ownerList,
            Integer commandStatus,
            String updaterId,
            LocalDateTime now) {}

    private String ownedProductId;
    private String commandId;
    private String commandName;
    private String creatorId;
    private LocalDateTime creationTimestamp;
    private String updaterId;
    private LocalDateTime updateTimestamp;
    private String ownerList;
    private Integer commandStatus;

    /**
     * 由注册入参在内存中新建命令聚合。
     *
     * @param input 注册快照（非空，且命令 ID/名称/责任人非空）
     * @return 新建命令聚合
     * @throws DomainRuleException 入参为空或必填字段为空时
     */
    public static Command registerNew(Registration input) {
        if (input == null) {
            throw new DomainRuleException("注册参数不能为空");
        }
        if (StringUtils.isAnyBlank(input.commandId(), input.commandName(), input.ownerList())) {
            throw new DomainRuleException("命令ID/命令/责任人不能为空");
        }
        Command c = new Command();
        c.ownedProductId = Objects.requireNonNull(input.ownedProductId());
        c.commandId = input.commandId().trim();
        c.commandName = input.commandName().trim();
        String who = StringUtils.defaultIfBlank(input.creatorId(), "system");
        c.creatorId = who;
        c.creationTimestamp = input.now();
        c.updaterId = StringUtils.defaultIfBlank(input.updaterId(), who);
        c.updateTimestamp = input.now();
        c.ownerList = input.ownerList().trim();
        c.commandStatus = input.commandStatus() == null ? 1 : input.commandStatus();
        return c;
    }

    /**
     * 由快照还原命令聚合；入参为 null 时返回 null。
     *
     * @param input 快照（可为 null）
     * @return 命令聚合，或 null
     */
    public static Command rehydrate(Snapshot input) {
        if (input == null) {
            return null;
        }
        Command c = new Command();
        c.ownedProductId = input.ownedProductId();
        c.commandId = input.commandId();
        c.commandName = input.commandName();
        c.creatorId = input.creatorId();
        c.creationTimestamp = input.creationTimestamp();
        c.updaterId = input.updaterId();
        c.updateTimestamp = input.updateTimestamp();
        c.ownerList = input.ownerList();
        c.commandStatus = input.commandStatus();
        return c;
    }

    /**
     * 按补丁更新可编辑字段与审计时间；补丁为 null 时不做任何修改。
     *
     * @param patch 可编辑字段补丁（可为 null）
     */
    public void applyEditablePatch(EditablePatch patch) {
        if (patch == null) {
            return;
        }
        if (patch.commandName() != null && StringUtils.isNotBlank(patch.commandName())) {
            this.commandName = patch.commandName().trim();
        }
        if (patch.ownerList() != null && StringUtils.isNotBlank(patch.ownerList())) {
            this.ownerList = patch.ownerList().trim();
        }
        if (patch.commandStatus() != null) {
            this.commandStatus = patch.commandStatus();
        }
        if (StringUtils.isNotBlank(patch.updaterId())) {
            this.updaterId = patch.updaterId();
        }
        this.updateTimestamp = patch.now();
    }

    /**
     * 将命令置为禁用状态。
     *
     * @param now 当前时间
     */
    public void disable(LocalDateTime now) {
        this.commandStatus = 0;
        this.updateTimestamp = now;
    }

    /**
     * 判断命令是否归属指定产品。
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
     * 获取命令 ID。
     *
     * @return 命令 ID
     */
    public String getCommandId() {
        return commandId;
    }

    /**
     * 获取命令名称。
     *
     * @return 命令名称
     */
    public String getCommandName() {
        return commandName;
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

    /**
     * 获取责任人列表（原始存储格式）。
     *
     * @return 责任人列表
     */
    public String getOwnerList() {
        return ownerList;
    }

    /**
     * 获取命令状态。
     *
     * @return 命令状态
     */
    public Integer getCommandStatus() {
        return commandStatus;
    }
}
