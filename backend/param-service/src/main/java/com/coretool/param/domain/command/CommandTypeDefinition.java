package com.coretool.param.domain.command;

import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

/** 命令类型定义（command_type_definition）。 */
public class CommandTypeDefinition {

    public record Registration(
            String ownedProductId,
            String ownedCommandId,
            String commandTypeId,
            String commandTypeName,
            String commandType,
            Integer minValue,
            Integer maxValue,
            String occupiedSerialNumber,
            Integer commandTypeStatus,
            String creatorId,
            String updaterId,
            LocalDateTime now) {}

    public record Snapshot(
            String ownedProductId,
            String ownedCommandId,
            String commandTypeId,
            String commandTypeName,
            String commandType,
            Integer minValue,
            Integer maxValue,
            String occupiedSerialNumber,
            Integer commandTypeStatus,
            String creatorId,
            LocalDateTime creationTimestamp,
            String updaterId,
            LocalDateTime updateTimestamp) {}

    public record EditablePatch(
            String commandTypeName,
            String commandType,
            Integer minValue,
            Integer maxValue,
            String occupiedSerialNumber,
            Integer commandTypeStatus,
            String updaterId,
            LocalDateTime now) {}

    private String ownedProductId;
    private String ownedCommandId;
    private String commandTypeId;
    private String commandTypeName;
    private String commandType;
    private Integer minValue;
    private Integer maxValue;
    private String occupiedSerialNumber;
    private Integer commandTypeStatus;
    private String creatorId;
    private LocalDateTime creationTimestamp;
    private String updaterId;
    private LocalDateTime updateTimestamp;

    /**
     * 注册新命令类型定义（创建聚合/实体）。
     *
     * @param input 注册入参，各字段与 {@link Registration} 记录组件一一对应
     * @return 新命令类型定义
     */
    public static CommandTypeDefinition registerNew(Registration input) {
        if (input == null) {
            throw new DomainRuleException("注册参数不能为空");
        }
        if (StringUtils.isAnyBlank(
                input.ownedCommandId(), input.commandTypeId(), input.commandTypeName(), input.commandType())) {
            throw new DomainRuleException("归属命令/类型ID/类型名称/类型枚举不能为空");
        }
        if (input.minValue() != null && input.maxValue() != null && input.minValue() > input.maxValue()) {
            throw new DomainRuleException("min_value 不能大于 max_value");
        }
        CommandTypeDefinition t = new CommandTypeDefinition();
        t.ownedProductId = Objects.requireNonNull(input.ownedProductId());
        t.ownedCommandId = input.ownedCommandId().trim();
        t.commandTypeId = input.commandTypeId().trim();
        t.commandTypeName = input.commandTypeName().trim();
        t.commandType = input.commandType().trim();
        t.minValue = input.minValue();
        t.maxValue = input.maxValue();
        t.occupiedSerialNumber = input.occupiedSerialNumber();
        t.commandTypeStatus = input.commandTypeStatus() == null ? 1 : input.commandTypeStatus();
        String who = StringUtils.defaultIfBlank(input.creatorId(), "system");
        t.creatorId = who;
        t.creationTimestamp = input.now();
        t.updaterId = StringUtils.defaultIfBlank(input.updaterId(), who);
        t.updateTimestamp = input.now();
        return t;
    }

    /**
     * 从持久化数据重建命令类型定义（rehydrate）。
     *
     * @param input 持久化快照，各字段与 {@link Snapshot} 记录组件一一对应
     * @return 重建后的命令类型定义
     */
    public static CommandTypeDefinition rehydrate(Snapshot input) {
        if (input == null) {
            return null;
        }
        CommandTypeDefinition t = new CommandTypeDefinition();
        t.ownedProductId = input.ownedProductId();
        t.ownedCommandId = input.ownedCommandId();
        t.commandTypeId = input.commandTypeId();
        t.commandTypeName = input.commandTypeName();
        t.commandType = input.commandType();
        t.minValue = input.minValue();
        t.maxValue = input.maxValue();
        t.occupiedSerialNumber = input.occupiedSerialNumber();
        t.commandTypeStatus = input.commandTypeStatus();
        t.creatorId = input.creatorId();
        t.creationTimestamp = input.creationTimestamp();
        t.updaterId = input.updaterId();
        t.updateTimestamp = input.updateTimestamp();
        return t;
    }

    /**
     * 应用可编辑字段的局部更新（带领域校验）。
     *
     * @param patch 可编辑变更，各字段与 {@link EditablePatch} 记录组件一一对应
     */
    public void applyEditablePatch(EditablePatch patch) {
        if (patch == null) {
            return;
        }
        if (patch.commandTypeName() != null && StringUtils.isNotBlank(patch.commandTypeName())) {
            this.commandTypeName = patch.commandTypeName().trim();
        }
        if (patch.commandType() != null && StringUtils.isNotBlank(patch.commandType())) {
            this.commandType = patch.commandType().trim();
        }
        if (patch.minValue() != null) {
            this.minValue = patch.minValue();
        }
        if (patch.maxValue() != null) {
            this.maxValue = patch.maxValue();
        }
        if (this.minValue != null && this.maxValue != null && this.minValue > this.maxValue) {
            throw new DomainRuleException("min_value 不能大于 max_value");
        }
        if (patch.occupiedSerialNumber() != null) {
            this.occupiedSerialNumber = patch.occupiedSerialNumber();
        }
        if (patch.commandTypeStatus() != null) {
            this.commandTypeStatus = patch.commandTypeStatus();
        }
        if (StringUtils.isNotBlank(patch.updaterId())) {
            this.updaterId = patch.updaterId();
        }
        this.updateTimestamp = patch.now();
    }

    /**
     * 判断类型定义是否归属指定产品。
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
     * 获取归属命令 ID。
     *
     * @return 归属命令 ID
     */
    public String getOwnedCommandId() {
        return ownedCommandId;
    }

    /**
     * 获取类型 ID。
     *
     * @return 类型 ID
     */
    public String getCommandTypeId() {
        return commandTypeId;
    }

    /**
     * 获取类型名称。
     *
     * @return 类型名称
     */
    public String getCommandTypeName() {
        return commandTypeName;
    }

    /**
     * 获取类型枚举/分类。
     *
     * @return 类型枚举/分类
     */
    public String getCommandType() {
        return commandType;
    }

    /**
     * 获取最小值。
     *
     * @return 最小值
     */
    public Integer getMinValue() {
        return minValue;
    }

    /**
     * 获取最大值。
     *
     * @return 最大值
     */
    public Integer getMaxValue() {
        return maxValue;
    }

    /**
     * 获取占用序号。
     *
     * @return 占用序号
     */
    public String getOccupiedSerialNumber() {
        return occupiedSerialNumber;
    }

    /**
     * 获取类型状态。
     *
     * @return 类型状态
     */
    public Integer getCommandTypeStatus() {
        return commandTypeStatus;
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
