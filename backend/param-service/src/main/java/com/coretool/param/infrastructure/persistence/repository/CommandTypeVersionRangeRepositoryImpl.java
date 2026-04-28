package com.coretool.param.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coretool.param.domain.command.CommandTypeVersionRange;
import com.coretool.param.domain.command.repository.CommandTypeVersionRangeRepository;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.infrastructure.persistence.assembly.CommandTypeVersionRangeAssembler;
import com.coretool.param.infrastructure.persistence.entity.CommandTypeVersionRangePo;
import com.coretool.param.infrastructure.persistence.mapper.CommandTypeVersionRangeMapper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class CommandTypeVersionRangeRepositoryImpl implements CommandTypeVersionRangeRepository {

    private final CommandTypeVersionRangeMapper mapper;

    /**
     * 创建命令类型版本区段仓储实现。
     *
     * @param mapper MyBatis-Plus Mapper
     */
    public CommandTypeVersionRangeRepositoryImpl(CommandTypeVersionRangeMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 按区段 ID 查询区段。
     *
     * @param rangeId 区段 ID
     * @return 区段（可能为空）
     */
    @Override
    public Optional<CommandTypeVersionRange> findById(String rangeId) {
        return Optional.ofNullable(mapper.selectById(rangeId)).map(CommandTypeVersionRangeAssembler::toDomain);
    }

    /**
     * 列出同一产品/命令/类型/版本范围内启用的区段（按起始序号升序）。
     *
     * @param productId               产品 ID
     * @param ownedCommandId          归属命令 ID
     * @param ownedTypeId             归属类型 ID
     * @param ownedVersionOrBusinessId 归属版本/业务 ID
     * @return 区段列表
     */
    @Override
    public List<CommandTypeVersionRange> listEnabledInScope(
            String productId, String ownedCommandId, String ownedTypeId, String ownedVersionOrBusinessId) {
        return mapper
                .selectList(
                        new LambdaQueryWrapper<CommandTypeVersionRangePo>()
                                .eq(CommandTypeVersionRangePo::getOwnedProductId, productId)
                                .eq(CommandTypeVersionRangePo::getOwnedCommandId, ownedCommandId)
                                .eq(CommandTypeVersionRangePo::getOwnedTypeId, ownedTypeId)
                                .eq(CommandTypeVersionRangePo::getOwnedVersionOrBusinessId, ownedVersionOrBusinessId)
                                .eq(CommandTypeVersionRangePo::getRangeStatus, 1)
                                .orderByAsc(CommandTypeVersionRangePo::getStartIndex))
                .stream()
                .map(CommandTypeVersionRangeAssembler::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * 新增区段。
     *
     * @param range 区段
     */
    @Override
    public void insert(CommandTypeVersionRange range) {
        mapper.insert(CommandTypeVersionRangeAssembler.toPo(range));
    }

    /**
     * 更新区段。
     *
     * @param range 区段
     */
    @Override
    public void update(CommandTypeVersionRange range) {
        mapper.updateById(CommandTypeVersionRangeAssembler.toPo(range));
    }

    /**
     * 按产品分页查询启用的区段。
     *
     * @param productId        产品 ID
     * @param page             页码（从 1 开始）
     * @param size             页大小
     * @param ownedTypeIdFilter 类型 ID 过滤（可为空）
     * @return 分页切片
     */
    @Override
    public PageSlice<CommandTypeVersionRange> pageByProduct(
            String productId, int page, int size, String ownedTypeIdFilter) {
        Page<CommandTypeVersionRangePo> p = new Page<>(page, size);
        LambdaQueryWrapper<CommandTypeVersionRangePo> w =
                new LambdaQueryWrapper<CommandTypeVersionRangePo>()
                        .eq(CommandTypeVersionRangePo::getOwnedProductId, productId)
                        .eq(CommandTypeVersionRangePo::getRangeStatus, 1)
                        .orderByDesc(CommandTypeVersionRangePo::getUpdateTimestamp);
        if (StringUtils.isNotBlank(ownedTypeIdFilter)) {
            w.eq(CommandTypeVersionRangePo::getOwnedTypeId, ownedTypeIdFilter.trim());
        }
        Page<CommandTypeVersionRangePo> result = mapper.selectPage(p, w);
        return new PageSlice<>(
                result.getRecords().stream()
                        .map(CommandTypeVersionRangeAssembler::toDomain)
                        .collect(Collectors.toList()),
                result.getTotal(),
                page,
                size);
    }
}
