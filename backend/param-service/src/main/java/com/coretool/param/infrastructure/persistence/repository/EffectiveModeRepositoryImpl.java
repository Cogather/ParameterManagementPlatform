package com.coretool.param.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coretool.param.domain.config.effectivemode.EffectiveMode;
import com.coretool.param.domain.config.effectivemode.repository.EffectiveModeRepository;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.infrastructure.persistence.assembly.EffectiveModeAssembler;
import com.coretool.param.infrastructure.persistence.entity.EntityEffectiveModeDictPo;
import com.coretool.param.infrastructure.persistence.mapper.EntityEffectiveModeDictMapper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class EffectiveModeRepositoryImpl implements EffectiveModeRepository {

    private final EntityEffectiveModeDictMapper mapper;

    /**
     * 创建生效方式仓储实现。
     *
     * @param mapper MyBatis-Plus Mapper
     */
    public EffectiveModeRepositoryImpl(EntityEffectiveModeDictMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 按生效方式 ID 查询生效方式。
     *
     * @param effectiveModeId 生效方式 ID
     * @return 生效方式（可能为空）
     */
    @Override
    public Optional<EffectiveMode> findById(String effectiveModeId) {
        return Optional.ofNullable(mapper.selectById(effectiveModeId)).map(EffectiveModeAssembler::toDomain);
    }

    /**
     * 按中文名查询同一产品下禁用的生效方式（用于“启用复用”场景）。
     *
     * @param productId           产品 ID
     * @param effectiveModeNameCn 生效方式中文名
     * @return 禁用生效方式（可能为空）
     */
    @Override
    public Optional<EffectiveMode> findDisabledByNameCnInProduct(String productId, String effectiveModeNameCn) {
        if (StringUtils.isBlank(productId) || StringUtils.isBlank(effectiveModeNameCn)) {
            return Optional.empty();
        }
        LambdaQueryWrapper<EntityEffectiveModeDictPo> w =
                new LambdaQueryWrapper<EntityEffectiveModeDictPo>()
                        .eq(EntityEffectiveModeDictPo::getOwnedProductId, productId)
                        .eq(EntityEffectiveModeDictPo::getEffectiveModeNameCn, effectiveModeNameCn)
                        .eq(EntityEffectiveModeDictPo::getEffectiveModeStatus, 0)
                        .last("LIMIT 1");
        return Optional.ofNullable(mapper.selectOne(w)).map(EffectiveModeAssembler::toDomain);
    }

    /**
     * 新增生效方式。
     *
     * @param mode 生效方式
     */
    @Override
    public void insert(EffectiveMode mode) {
        mapper.insert(EffectiveModeAssembler.toPo(mode));
    }

    /**
     * 更新生效方式。
     *
     * @param mode 生效方式
     */
    @Override
    public void update(EffectiveMode mode) {
        mapper.updateById(EffectiveModeAssembler.toPo(mode));
    }

    /**
     * 按产品分页查询启用的生效方式。
     *
     * @param productId 产品 ID
     * @param page      页码（从 1 开始）
     * @param size      页大小
     * @param keyword   关键字（可为空）
     * @return 分页切片
     */
    @Override
    public PageSlice<EffectiveMode> pageByProduct(
            String productId, int page, int size, String keyword) {
        Page<EntityEffectiveModeDictPo> p = new Page<>(page, size);
        LambdaQueryWrapper<EntityEffectiveModeDictPo> w =
                new LambdaQueryWrapper<EntityEffectiveModeDictPo>()
                        .eq(EntityEffectiveModeDictPo::getOwnedProductId, productId)
                        .eq(EntityEffectiveModeDictPo::getEffectiveModeStatus, 1)
                        .orderByDesc(EntityEffectiveModeDictPo::getUpdateTimestamp);
        if (StringUtils.isNotBlank(keyword)) {
            w.and(
                    x ->
                            x.like(EntityEffectiveModeDictPo::getEffectiveModeNameCn, keyword)
                                    .or()
                                    .like(EntityEffectiveModeDictPo::getEffectiveModeNameEn, keyword));
        }
        Page<EntityEffectiveModeDictPo> result = mapper.selectPage(p, w);
        return new PageSlice<>(
                result.getRecords().stream()
                        .map(EffectiveModeAssembler::toDomain)
                        .collect(Collectors.toList()),
                result.getTotal(),
                page,
                size);
    }
}
