package com.coretool.param.domain.config.projectteam;

import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

/** 项目组（project_team_dict） */
public class ProjectTeam {

    public record Registration(
            String ownedProductId,
            String teamId,
            String teamName,
            String teamDescription,
            Integer teamStatus,
            String ownerList,
            String creatorId,
            String updaterId,
            LocalDateTime now) {}

    public record Snapshot(
            String ownedProductId,
            String teamId,
            String teamName,
            String teamDescription,
            Integer teamStatus,
            String ownerList,
            String creatorId,
            LocalDateTime creationTimestamp,
            String updaterId,
            LocalDateTime updateTimestamp) {}

    public record Patch(
            String teamName,
            String teamDescription,
            Integer teamStatus,
            String ownerList,
            String updaterId,
            LocalDateTime now) {}

    private String ownedProductId;
    private String teamId;
    private String teamName;
    private String teamDescription;
    private int teamStatus;
    private String ownerList;
    private String creatorId;
    private LocalDateTime creationTimestamp;
    private String updaterId;
    private LocalDateTime updateTimestamp;

    /**
     * 注册新项目组（创建聚合/实体）。
     *
     * @param input 注册入参，各字段与 {@link Registration} 记录组件一一对应
     * @return 新项目组
     */
    public static ProjectTeam registerNew(Registration input) {
        if (input == null) {
            throw new DomainRuleException("注册参数不能为空");
        }
        if (StringUtils.isAnyBlank(input.teamId(), input.teamName())) {
            throw new DomainRuleException("项目组ID与名称不能为空");
        }
        if (StringUtils.isBlank(input.ownerList())) {
            throw new DomainRuleException("PROJECT_TEAM_OWNER_REQUIRED: 责任人必填");
        }
        ProjectTeam t = new ProjectTeam();
        t.ownedProductId = Objects.requireNonNull(input.ownedProductId());
        t.teamId = input.teamId().trim();
        t.teamName = input.teamName().trim();
        t.teamDescription = input.teamDescription();
        t.teamStatus = input.teamStatus() == null ? 1 : input.teamStatus();
        t.ownerList = input.ownerList().trim();
        String c = StringUtils.defaultIfBlank(input.creatorId(), "system");
        t.creatorId = c;
        t.creationTimestamp = input.now();
        t.updaterId = StringUtils.defaultIfBlank(input.updaterId(), c);
        t.updateTimestamp = input.now();
        return t;
    }

    /**
     * 从持久化数据重建项目组（rehydrate）。
     *
     * @param input 持久化快照，各字段与 {@link Snapshot} 记录组件一一对应
     * @return 重建后的项目组
     */
    public static ProjectTeam rehydrate(Snapshot input) {
        if (input == null) {
            return null;
        }
        ProjectTeam t = new ProjectTeam();
        t.ownedProductId = input.ownedProductId();
        t.teamId = input.teamId();
        t.teamName = input.teamName();
        t.teamDescription = input.teamDescription();
        t.teamStatus = input.teamStatus() == null ? 1 : input.teamStatus();
        t.ownerList = input.ownerList();
        t.creatorId = input.creatorId();
        t.creationTimestamp = input.creationTimestamp();
        t.updaterId = input.updaterId();
        t.updateTimestamp = input.updateTimestamp();
        return t;
    }

    /**
     * 应用可编辑字段的局部更新（更新后校验责任人必填）。
     *
     * @param patch 局部变更，各字段与 {@link Patch} 记录组件一一对应
     */
    public void applyPatch(Patch patch) {
        if (patch == null) {
            return;
        }
        if (StringUtils.isNotBlank(patch.teamName())) {
            this.teamName = patch.teamName().trim();
        }
        if (patch.teamDescription() != null) {
            this.teamDescription = patch.teamDescription();
        }
        if (patch.teamStatus() != null) {
            this.teamStatus = patch.teamStatus();
        }
        if (StringUtils.isNotBlank(patch.ownerList())) {
            this.ownerList = patch.ownerList().trim();
        }
        if (StringUtils.isNotBlank(patch.updaterId())) {
            this.updaterId = patch.updaterId();
        }
        this.updateTimestamp = patch.now();
        assertOwnerRequired();
    }

    private void assertOwnerRequired() {
        if (StringUtils.isBlank(ownerList)) {
            throw new DomainRuleException("PROJECT_TEAM_OWNER_REQUIRED: 责任人必填");
        }
    }

    /**
     * 将项目组置为禁用状态。
     *
     * @param now 当前时间
     */
    public void disable(LocalDateTime now) {
        this.teamStatus = 0;
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
     * 判断项目组是否归属指定产品。
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
     * 获取项目组 ID。
     *
     * @return 项目组 ID
     */
    public String getTeamId() {
        return teamId;
    }

    /**
     * 获取项目组名称。
     *
     * @return 项目组名称
     */
    public String getTeamName() {
        return teamName;
    }

    /**
     * 获取项目组描述。
     *
     * @return 描述
     */
    public String getTeamDescription() {
        return teamDescription;
    }

    /**
     * 获取项目组状态。
     *
     * @return 状态
     */
    public int getTeamStatus() {
        return teamStatus;
    }

    /**
     * 获取责任人列表。
     *
     * @return 责任人列表
     */
    public String getOwnerList() {
        return ownerList;
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
