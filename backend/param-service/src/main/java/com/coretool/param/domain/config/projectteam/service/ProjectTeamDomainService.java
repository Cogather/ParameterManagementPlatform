package com.coretool.param.domain.config.projectteam.service;

import com.coretool.param.domain.config.projectteam.ProjectTeam;
import com.coretool.param.domain.config.projectteam.repository.ProjectTeamRepository;
import com.coretool.param.domain.exception.DomainRuleException;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

/** 项目组领域服务：封装归属校验、状态变更等规则。 */
public class ProjectTeamDomainService {

    public record CreateCommand(
            String productId,
            String teamId,
            String teamName,
            String teamDescription,
            Integer teamStatus,
            String ownerList,
            String creatorId,
            String updaterId,
            LocalDateTime now) {}

    public record UpdateCommand(
            String productId,
            String teamId,
            String teamName,
            String teamDescription,
            Integer teamStatus,
            String ownerList,
            String updaterId,
            LocalDateTime now) {}

    private final ProjectTeamRepository repository;

    /**
     * 创建项目组领域服务。
     *
     * @param repository 项目组仓储
     */
    public ProjectTeamDomainService(ProjectTeamRepository repository) {
        this.repository = repository;
    }

    /**
     * 创建新项目组。
     *
     * @param input 创建入参，各字段与 {@link CreateCommand} 记录组件一一对应
     * @return 新项目组
     */
    public ProjectTeam createNew(CreateCommand input) {
        if (input == null) {
            throw new DomainRuleException("创建参数不能为空");
        }
        return ProjectTeam.registerNew(
                new ProjectTeam.Registration(
                        input.productId(),
                        input.teamId(),
                        input.teamName(),
                        input.teamDescription(),
                        input.teamStatus(),
                        input.ownerList(),
                        input.creatorId(),
                        input.updaterId(),
                        input.now()));
    }

    /**
     * 更新既有项目组（无字段变更时仅刷新更新时间）。
     *
     * @param input 更新入参，各字段与 {@link UpdateCommand} 记录组件一一对应
     * @return 更新后的项目组
     */
    public ProjectTeam updateExisting(UpdateCommand input) {
        if (input == null) {
            throw new DomainRuleException("更新参数不能为空");
        }
        ProjectTeam existing = requireOwned(input.productId(), input.teamId());
        if (StringUtils.isNotBlank(input.teamName())
                || input.teamDescription() != null
                || input.teamStatus() != null
                || StringUtils.isNotBlank(input.ownerList())
                || StringUtils.isNotBlank(input.updaterId())) {
            existing.applyPatch(
                    new ProjectTeam.Patch(
                            input.teamName(),
                            input.teamDescription(),
                            input.teamStatus(),
                            input.ownerList(),
                            input.updaterId(),
                            input.now()));
        } else {
            existing.touch(input.now());
        }
        return existing;
    }

    /**
     * 禁用项目组（含归属校验）。
     *
     * @param productId 产品 ID
     * @param teamId    项目组 ID
     * @param now       当前时间
     * @return 禁用后的项目组
     */
    public ProjectTeam disable(String productId, String teamId, LocalDateTime now) {
        ProjectTeam existing = requireOwned(productId, teamId);
        existing.disable(now);
        return existing;
    }

    /**
     * 获取并校验项目组归属产品（不存在或不归属则抛出领域异常）。
     *
     * @param productId 产品 ID
     * @param teamId    项目组 ID
     * @return 项目组
     */
    public ProjectTeam requireOwned(String productId, String teamId) {
        return repository
                .findByTeamId(teamId)
                .filter(t -> t.belongsToProduct(productId))
                .orElseThrow(() -> new DomainRuleException("项目组不存在或不属于该产品"));
    }
}

