package com.coretool.param.domain.command;

import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

/** 产品命令（entity_command_mapping）。 */
public class Command {

    public record Registration(
            String ownedProductId,
            String commandId,
            String commandName,
            String creatorId,
            String updaterId,
            String ownerList,
            Integer commandStatus,
            LocalDateTime now) {}

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
