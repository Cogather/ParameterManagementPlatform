package com.coretool.param.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coretool.param.domain.command.CommandTypeDefinition;
import com.coretool.param.domain.command.repository.CommandTypeDefinitionRepository;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.infrastructure.persistence.assembly.CommandTypeDefinitionAssembler;
import com.coretool.param.infrastructure.persistence.entity.CommandTypeDefinitionPo;
import com.coretool.param.infrastructure.persistence.mapper.CommandTypeDefinitionMapper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class CommandTypeDefinitionRepositoryImpl implements CommandTypeDefinitionRepository {

    private final CommandTypeDefinitionMapper mapper;

    /**
     * 创建命令类型定义仓储实现。
     *
     * @param mapper MyBatis-Plus Mapper
     */
    public CommandTypeDefinitionRepositoryImpl(CommandTypeDefinitionMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 按类型 ID 查询命令类型定义。
     *
     * @param commandTypeId 类型 ID
     * @return 类型定义（可能为空）
     */
    @Override
    public Optional<CommandTypeDefinition> findById(String commandTypeId) {
        return Optional.ofNullable(mapper.selectById(commandTypeId)).map(CommandTypeDefinitionAssembler::toDomain);
    }

    /**
     * 判断同一产品下是否存在同名且启用的命令类型定义。
     *
     * @param productId           产品 ID
     * @param commandTypeName     类型名称
     * @param excludeCommandTypeId 排除的类型 ID（可为空）
     * @return 是否存在
     */
    @Override
    public boolean existsSameNameInProduct(String productId, String commandTypeName, String excludeCommandTypeId) {
        if (StringUtils.isBlank(productId) || StringUtils.isBlank(commandTypeName)) {
            return false;
        }
        LambdaQueryWrapper<CommandTypeDefinitionPo> w =
                new LambdaQueryWrapper<CommandTypeDefinitionPo>()
                        .eq(CommandTypeDefinitionPo::getOwnedProductId, productId)
                        .eq(CommandTypeDefinitionPo::getCommandTypeName, commandTypeName)
                        .eq(CommandTypeDefinitionPo::getCommandTypeStatus, 1);
        if (excludeCommandTypeId != null) {
            w.ne(CommandTypeDefinitionPo::getCommandTypeId, excludeCommandTypeId);
        }
        Long n = mapper.selectCount(w);
        return n != null && n > 0;
    }

    /**
     * 按名称查询同一产品下禁用的类型定义（用于“启用复用”场景）。
     *
     * @param productId       产品 ID
     * @param commandTypeName 类型名称
     * @return 禁用类型定义（可能为空）
     */
    @Override
    public Optional<CommandTypeDefinition> findDisabledByNameInProduct(String productId, String commandTypeName) {
        if (StringUtils.isBlank(productId) || StringUtils.isBlank(commandTypeName)) {
            return Optional.empty();
        }
        LambdaQueryWrapper<CommandTypeDefinitionPo> w =
                new LambdaQueryWrapper<CommandTypeDefinitionPo>()
                        .eq(CommandTypeDefinitionPo::getOwnedProductId, productId)
                        .eq(CommandTypeDefinitionPo::getCommandTypeName, commandTypeName)
                        .eq(CommandTypeDefinitionPo::getCommandTypeStatus, 0)
                        .last("LIMIT 1");
        return Optional.ofNullable(mapper.selectOne(w)).map(CommandTypeDefinitionAssembler::toDomain);
    }

    /**
     * 新增命令类型定义。
     *
     * @param type 类型定义
     */
    @Override
    public void insert(CommandTypeDefinition type) {
        mapper.insert(CommandTypeDefinitionAssembler.toPo(type));
    }

    /**
     * 更新命令类型定义。
     *
     * @param type 类型定义
     */
    @Override
    public void update(CommandTypeDefinition type) {
        mapper.updateById(CommandTypeDefinitionAssembler.toPo(type));
    }

    /**
     * 按产品分页查询启用的命令类型定义。
     *
     * @param productId 产品 ID
     * @param page      页码（从 1 开始）
     * @param size      页大小
     * @param keyword   名称关键字（可为空）
     * @return 分页切片
     */
    @Override
    public PageSlice<CommandTypeDefinition> pageByProduct(String productId, int page, int size, String keyword) {
        Page<CommandTypeDefinitionPo> p = new Page<>(page, size);
        LambdaQueryWrapper<CommandTypeDefinitionPo> w =
                new LambdaQueryWrapper<CommandTypeDefinitionPo>()
                        .eq(CommandTypeDefinitionPo::getOwnedProductId, productId)
                        .eq(CommandTypeDefinitionPo::getCommandTypeStatus, 1)
                        .orderByDesc(CommandTypeDefinitionPo::getUpdateTimestamp);
        if (StringUtils.isNotBlank(keyword)) {
            w.like(CommandTypeDefinitionPo::getCommandTypeName, keyword.trim());
        }
        Page<CommandTypeDefinitionPo> result = mapper.selectPage(p, w);
        return new PageSlice<>(
                result.getRecords().stream()
                        .map(CommandTypeDefinitionAssembler::toDomain)
                        .collect(Collectors.toList()),
                result.getTotal(),
                page,
                size);
    }
}
