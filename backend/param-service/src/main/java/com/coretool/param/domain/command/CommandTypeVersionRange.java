package com.coretool.param.domain.command;

import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

/** 命令类型版本区段（command_type_version_range）。 */
public class CommandTypeVersionRange {

    public record Registration(
            String ownedProductId,
            String ownedCommandId,
            String ownedTypeId,
            String versionRangeId,
            Integer startIndex,
            Integer endIndex,
            String rangeDescription,
            String rangeType,
            String ownedVersionOrBusinessId,
            Integer rangeStatus,
            String creatorId,
            String updaterId,
            LocalDateTime now) {}

    public record Snapshot(
            String ownedProductId,
            String ownedCommandId,
            String ownedTypeId,
            String versionRangeId,
            Integer startIndex,
            Integer endIndex,
            String rangeDescription,
            String rangeType,
            String ownedVersionOrBusinessId,
            Integer rangeStatus,
            String creatorId,
            LocalDateTime creationTimestamp,
            String updaterId,
            LocalDateTime updateTimestamp) {}

    public record EditablePatch(
            Integer startIndex,
            Integer endIndex,
            String rangeDescription,
            String rangeType,
            String ownedVersionOrBusinessId,
            Integer rangeStatus,
            String updaterId,
            LocalDateTime now) {}

    private String versionRangeId;
    private String ownedProductId;
    private String ownedCommandId;
    private String ownedTypeId;
    private Integer startIndex;
    private Integer endIndex;
    private String rangeDescription;
    private String rangeType;
    private String ownedVersionOrBusinessId;
    private Integer rangeStatus;
    private String creatorId;
    private LocalDateTime creationTimestamp;
    private String updaterId;
    private LocalDateTime updateTimestamp;

    /**
     * 注册新的命令类型版本区段（创建聚合/实体）。
     *
     * @param input 注册入参，各字段与 {@link Registration} 记录组件一一对应
     * @return 新区段
     */
    public static CommandTypeVersionRange registerNew(Registration input) {
        if (input == null) {
            throw new DomainRuleException("注册参数不能为空");
        }
        if (StringUtils.isAnyBlank(
                input.ownedCommandId(), input.ownedTypeId(), input.versionRangeId(), input.ownedVersionOrBusinessId())) {
            throw new DomainRuleException("归属命令/类型ID/区段ID/归属版本不能为空");
        }
        if (input.startIndex() == null || input.endIndex() == null) {
            throw new DomainRuleException("起始/结束序号不能为空");
        }
        if (input.startIndex() > input.endIndex()) {
            throw new DomainRuleException("start_index 不能大于 end_index");
        }
        CommandTypeVersionRange r = new CommandTypeVersionRange();
        r.ownedProductId = Objects.requireNonNull(input.ownedProductId());
        r.ownedCommandId = input.ownedCommandId().trim();
        r.ownedTypeId = input.ownedTypeId().trim();
        r.versionRangeId = input.versionRangeId().trim();
        r.startIndex = input.startIndex();
        r.endIndex = input.endIndex();
        r.rangeDescription = input.rangeDescription();
        r.rangeType = input.rangeType();
        r.ownedVersionOrBusinessId = input.ownedVersionOrBusinessId().trim();
        r.rangeStatus = input.rangeStatus() == null ? 1 : input.rangeStatus();
        String who = StringUtils.defaultIfBlank(input.creatorId(), "system");
        r.creatorId = who;
        r.creationTimestamp = input.now();
        r.updaterId = StringUtils.defaultIfBlank(input.updaterId(), who);
        r.updateTimestamp = input.now();
        return r;
    }

    /**
     * 从持久化数据重建命令类型版本区段（rehydrate）。
     *
     * @param input 持久化快照，各字段与 {@link Snapshot} 记录组件一一对应
     * @return 重建后的区段
     */
    public static CommandTypeVersionRange rehydrate(Snapshot input) {
        if (input == null) {
            return null;
        }
        CommandTypeVersionRange r = new CommandTypeVersionRange();
        r.ownedProductId = input.ownedProductId();
        r.ownedCommandId = input.ownedCommandId();
        r.ownedTypeId = input.ownedTypeId();
        r.versionRangeId = input.versionRangeId();
        r.startIndex = input.startIndex();
        r.endIndex = input.endIndex();
        r.rangeDescription = input.rangeDescription();
        r.rangeType = input.rangeType();
        r.ownedVersionOrBusinessId = input.ownedVersionOrBusinessId();
        r.rangeStatus = input.rangeStatus();
        r.creatorId = input.creatorId();
        r.creationTimestamp = input.creationTimestamp();
        r.updaterId = input.updaterId();
        r.updateTimestamp = input.updateTimestamp();
        return r;
    }

    /**
     * 校验区段序号范围是否落在类型定义的范围内。
     *
     * @param typeMin 类型最小值（为空则不校验）
     * @param typeMax 类型最大值（为空则不校验）
     */
    public void assertWithinTypeBounds(Integer typeMin, Integer typeMax) {
        if (typeMin == null || typeMax == null) {
            return;
        }
        int min = startIndex == null ? typeMin : startIndex;
        int max = endIndex == null ? typeMax : endIndex;
        if (min < typeMin || max > typeMax) {
            throw new DomainRuleException("RANGE_OUT_OF_TYPE_BOUNDS: 区段超出类型定义范围");
        }
    }

    /**
     * 判断同一产品/命令/类型/版本下，序号区间是否重叠。
     *
     * @param other 另一条区段
     * @return 是否重叠
     */
    public boolean overlapsIndexRange(CommandTypeVersionRange other) {
        if (other == null) {
            return false;
        }
        if (!Objects.equals(this.ownedProductId, other.ownedProductId)
                || !Objects.equals(this.ownedCommandId, other.ownedCommandId)
                || !Objects.equals(this.ownedTypeId, other.ownedTypeId)
                || !Objects.equals(this.ownedVersionOrBusinessId, other.ownedVersionOrBusinessId)) {
            return false;
        }
        return this.startIndex <= other.endIndex && other.startIndex <= this.endIndex;
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
        if (patch.startIndex() != null) {
            this.startIndex = patch.startIndex();
        }
        if (patch.endIndex() != null) {
            this.endIndex = patch.endIndex();
        }
        if (this.startIndex != null && this.endIndex != null && this.startIndex > this.endIndex) {
            throw new DomainRuleException("start_index 不能大于 end_index");
        }
        if (patch.rangeDescription() != null) {
            this.rangeDescription = patch.rangeDescription();
        }
        if (patch.rangeType() != null) {
            this.rangeType = patch.rangeType();
        }
        if (patch.ownedVersionOrBusinessId() != null && StringUtils.isNotBlank(patch.ownedVersionOrBusinessId())) {
            this.ownedVersionOrBusinessId = patch.ownedVersionOrBusinessId().trim();
        }
        if (patch.rangeStatus() != null) {
            this.rangeStatus = patch.rangeStatus();
        }
        if (StringUtils.isNotBlank(patch.updaterId())) {
            this.updaterId = patch.updaterId();
        }
        this.updateTimestamp = patch.now();
    }

    /**
     * 将区段置为禁用状态。
     *
     * @param now 当前时间
     */
    public void disable(LocalDateTime now) {
        this.rangeStatus = 0;
        this.updateTimestamp = now;
    }

    /**
     * 判断区段是否归属指定产品。
     *
     * @param productId 产品 ID
     * @return 是否归属该产品
     */
    public boolean belongsToProduct(String productId) {
        return Objects.equals(this.ownedProductId, productId);
    }

    /**
     * 获取区段 ID。
     *
     * @return 区段 ID
     */
    public String getVersionRangeId() {
        return versionRangeId;
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
     * 获取归属类型 ID。
     *
     * @return 归属类型 ID
     */
    public String getOwnedTypeId() {
        return ownedTypeId;
    }

    /**
     * 获取起始序号。
     *
     * @return 起始序号
     */
    public Integer getStartIndex() {
        return startIndex;
    }

    /**
     * 获取结束序号。
     *
     * @return 结束序号
     */
    public Integer getEndIndex() {
        return endIndex;
    }

    /**
     * 获取区段描述。
     *
     * @return 区段描述
     */
    public String getRangeDescription() {
        return rangeDescription;
    }

    /**
     * 获取区段类型。
     *
     * @return 区段类型
     */
    public String getRangeType() {
        return rangeType;
    }

    /**
     * 获取归属版本/业务 ID。
     *
     * @return 归属版本/业务 ID
     */
    public String getOwnedVersionOrBusinessId() {
        return ownedVersionOrBusinessId;
    }

    /**
     * 获取区段状态。
     *
     * @return 区段状态
     */
    public Integer getRangeStatus() {
        return rangeStatus;
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
