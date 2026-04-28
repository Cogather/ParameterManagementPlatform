package com.coretool.param.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coretool.param.domain.command.Command;
import com.coretool.param.domain.command.repository.CommandRepository;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.infrastructure.persistence.assembly.CommandAssembler;
import com.coretool.param.infrastructure.persistence.entity.EntityCommandMappingPo;
import com.coretool.param.infrastructure.persistence.mapper.EntityCommandMappingMapper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class CommandRepositoryImpl implements CommandRepository {

    private final EntityCommandMappingMapper mapper;

    /**
     * 创建命令仓储实现。
     *
     * @param mapper MyBatis-Plus Mapper
     */
    public CommandRepositoryImpl(EntityCommandMappingMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 按命令 ID 查询命令。
     *
     * @param commandId 命令 ID
     * @return 命令（可能为空）
     */
    @Override
    public Optional<Command> findById(String commandId) {
        return Optional.ofNullable(mapper.selectById(commandId)).map(CommandAssembler::toDomain);
    }

    /**
     * 判断同一产品下是否存在同名且启用的命令。
     *
     * @param productId        产品 ID
     * @param commandName      命令名称
     * @param excludeCommandId 排除的命令 ID（可为空）
     * @return 是否存在
     */
    @Override
    public boolean existsSameNameInProduct(String productId, String commandName, String excludeCommandId) {
        if (StringUtils.isBlank(productId) || StringUtils.isBlank(commandName)) {
            return false;
        }
        LambdaQueryWrapper<EntityCommandMappingPo> w =
                new LambdaQueryWrapper<EntityCommandMappingPo>()
                        .eq(EntityCommandMappingPo::getOwnedProductId, productId)
                        .eq(EntityCommandMappingPo::getCommandName, commandName)
                        .eq(EntityCommandMappingPo::getCommandStatus, 1);
        if (excludeCommandId != null) {
            w.ne(EntityCommandMappingPo::getCommandId, excludeCommandId);
        }
        Long n = mapper.selectCount(w);
        return n != null && n > 0;
    }

    /**
     * 按名称查询同一产品下禁用的命令（用于“启用复用”场景）。
     *
     * @param productId   产品 ID
     * @param commandName 命令名称
     * @return 禁用命令（可能为空）
     */
    @Override
    public Optional<Command> findDisabledByNameInProduct(String productId, String commandName) {
        if (StringUtils.isBlank(productId) || StringUtils.isBlank(commandName)) {
            return Optional.empty();
        }
        LambdaQueryWrapper<EntityCommandMappingPo> w =
                new LambdaQueryWrapper<EntityCommandMappingPo>()
                        .eq(EntityCommandMappingPo::getOwnedProductId, productId)
                        .eq(EntityCommandMappingPo::getCommandName, commandName)
                        .eq(EntityCommandMappingPo::getCommandStatus, 0)
                        .last("LIMIT 1");
        return Optional.ofNullable(mapper.selectOne(w)).map(CommandAssembler::toDomain);
    }

    /**
     * 新增命令。
     *
     * @param command 命令
     */
    @Override
    public void insert(Command command) {
        mapper.insert(CommandAssembler.toPo(command));
    }

    /**
     * 更新命令。
     *
     * @param command 命令
     */
    @Override
    public void update(Command command) {
        mapper.updateById(CommandAssembler.toPo(command));
    }

    /**
     * 按产品分页查询启用的命令。
     *
     * @param productId 产品 ID
     * @param page      页码（从 1 开始）
     * @param size      页大小
     * @param keyword   名称关键字（可为空）
     * @return 分页切片
     */
    @Override
    public PageSlice<Command> pageByProduct(String productId, int page, int size, String keyword) {
        Page<EntityCommandMappingPo> p = new Page<>(page, size);
        LambdaQueryWrapper<EntityCommandMappingPo> w =
                new LambdaQueryWrapper<EntityCommandMappingPo>()
                        .eq(EntityCommandMappingPo::getOwnedProductId, productId)
                        .eq(EntityCommandMappingPo::getCommandStatus, 1)
                        .orderByDesc(EntityCommandMappingPo::getUpdateTimestamp);
        if (StringUtils.isNotBlank(keyword)) {
            w.like(EntityCommandMappingPo::getCommandName, keyword.trim());
        }
        Page<EntityCommandMappingPo> result = mapper.selectPage(p, w);
        return new PageSlice<>(
                result.getRecords().stream().map(CommandAssembler::toDomain).collect(Collectors.toList()),
                result.getTotal(),
                page,
                size);
    }
}

