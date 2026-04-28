package com.coretool.param.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coretool.param.domain.config.projectteam.ProjectTeam;
import com.coretool.param.domain.config.projectteam.repository.ProjectTeamRepository;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.infrastructure.persistence.assembly.ProjectTeamAssembler;
import com.coretool.param.infrastructure.persistence.entity.ProjectTeamDictPo;
import com.coretool.param.infrastructure.persistence.mapper.ProjectTeamDictMapper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ProjectTeamRepositoryImpl implements ProjectTeamRepository {

    private final ProjectTeamDictMapper mapper;

    /**
     * 创建项目组仓储实现。
     *
     * @param mapper MyBatis-Plus Mapper
     */
    public ProjectTeamRepositoryImpl(ProjectTeamDictMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 按项目组 ID 查询项目组。
     *
     * @param teamId 项目组 ID
     * @return 项目组（可能为空）
     */
    @Override
    public Optional<ProjectTeam> findByTeamId(String teamId) {
        return Optional.ofNullable(mapper.selectById(teamId)).map(ProjectTeamAssembler::toDomain);
    }

    /**
     * 按名称查询同一产品下禁用的项目组（用于“启用复用”场景）。
     *
     * @param productId 产品 ID
     * @param teamName  项目组名称
     * @return 禁用项目组（可能为空）
     */
    @Override
    public Optional<ProjectTeam> findDisabledByNameInProduct(String productId, String teamName) {
        if (StringUtils.isBlank(productId) || StringUtils.isBlank(teamName)) {
            return Optional.empty();
        }
        LambdaQueryWrapper<ProjectTeamDictPo> w =
                new LambdaQueryWrapper<ProjectTeamDictPo>()
                        .eq(ProjectTeamDictPo::getOwnedProductId, productId)
                        .eq(ProjectTeamDictPo::getTeamName, teamName)
                        .eq(ProjectTeamDictPo::getTeamStatus, 0)
                        .last("LIMIT 1");
        return Optional.ofNullable(mapper.selectOne(w)).map(ProjectTeamAssembler::toDomain);
    }

    /**
     * 新增项目组。
     *
     * @param team 项目组
     */
    @Override
    public void insert(ProjectTeam team) {
        mapper.insert(ProjectTeamAssembler.toPo(team));
    }

    /**
     * 更新项目组。
     *
     * @param team 项目组
     */
    @Override
    public void update(ProjectTeam team) {
        mapper.updateById(ProjectTeamAssembler.toPo(team));
    }

    /**
     * 按产品分页查询启用的项目组。
     *
     * @param productId   产品 ID
     * @param page        页码（从 1 开始）
     * @param size        页大小
     * @param nameKeyword 名称关键字（可为空）
     * @return 分页切片
     */
    @Override
    public PageSlice<ProjectTeam> pageByProduct(
            String productId, int page, int size, String nameKeyword) {
        Page<ProjectTeamDictPo> p = new Page<>(page, size);
        LambdaQueryWrapper<ProjectTeamDictPo> w =
                new LambdaQueryWrapper<ProjectTeamDictPo>()
                        .eq(ProjectTeamDictPo::getOwnedProductId, productId)
                        .eq(ProjectTeamDictPo::getTeamStatus, 1)
                        .orderByDesc(ProjectTeamDictPo::getUpdateTimestamp);
        if (StringUtils.isNotBlank(nameKeyword)) {
            w.like(ProjectTeamDictPo::getTeamName, nameKeyword);
        }
        Page<ProjectTeamDictPo> result = mapper.selectPage(p, w);
        return new PageSlice<>(
                result.getRecords().stream()
                        .map(ProjectTeamAssembler::toDomain)
                        .collect(Collectors.toList()),
                result.getTotal(),
                page,
                size);
    }
}
