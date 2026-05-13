package com.coretool.param.domain.config.projectteam.repository;

import com.coretool.param.domain.config.projectteam.ProjectTeam;
import com.coretool.param.domain.support.PageSlice;

import java.util.Optional;

/**
 * 领域仓储接口「ProjectTeamRepository」，定义聚合持久化契约。
 *
 * @since 2026-04-28
 */

public interface ProjectTeamRepository {

/**
 * findByTeamId。
 *
 * @param teamId 见方法签名
 * @return 可选结果
 */

    Optional<ProjectTeam> findByTeamId(String teamId);

    /**
     * 同一产品下按项目组名称查找“已删除/未启用”(status=0) 的记录，用于新增时自动恢复。
     *
     * @param productId 产品 ID
     * @param teamName 项目组名称
     * @return 若存在则返回项目组
     */
    Optional<ProjectTeam> findDisabledByNameInProduct(String productId, String teamName);

/**
 * insert。
 *
 * @param team 见方法签名
 */

    void insert(ProjectTeam team);

/**
 * update。
 *
 * @param team 见方法签名
 */

    void update(ProjectTeam team);

/**
 * pageByProduct。
 *
 * @param productId 见方法签名
 * @param page 见方法签名
 * @param size 见方法签名
 * @param nameKeyword 见方法签名
 * @return 结果
 */

    PageSlice<ProjectTeam> pageByProduct(String productId, int page, int size, String nameKeyword);
}
