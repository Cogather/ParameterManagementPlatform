package com.coretool.param.domain.config.projectteam.repository;

import com.coretool.param.domain.config.projectteam.ProjectTeam;
import com.coretool.param.domain.support.PageSlice;

import java.util.Optional;

public interface ProjectTeamRepository {

    Optional<ProjectTeam> findByTeamId(String teamId);

    /** 同一产品下按项目组名称查找“已删除/未启用”(status=0) 的记录，用于新增时自动恢复。 */
    Optional<ProjectTeam> findDisabledByNameInProduct(String productId, String teamName);

    void insert(ProjectTeam team);

    void update(ProjectTeam team);

    PageSlice<ProjectTeam> pageByProduct(String productId, int page, int size, String nameKeyword);
}

