package com.coretool.param.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coretool.param.domain.config.effectiveform.EffectiveForm;
import com.coretool.param.domain.config.effectiveform.repository.EffectiveFormRepository;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.infrastructure.persistence.assembly.EffectiveFormAssembler;
import com.coretool.param.infrastructure.persistence.entity.EntityEffectiveFormDictPo;
import com.coretool.param.infrastructure.persistence.mapper.EntityEffectiveFormDictMapper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class EffectiveFormRepositoryImpl implements EffectiveFormRepository {

    private final EntityEffectiveFormDictMapper mapper;

    /**
     * 创建生效形态仓储实现。
     *
     * @param mapper MyBatis-Plus Mapper
     */
    public EffectiveFormRepositoryImpl(EntityEffectiveFormDictMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 按生效形态 ID 查询生效形态。
     *
     * @param effectiveFormId 生效形态 ID
     * @return 生效形态（可能为空）
     */
    @Override
    public Optional<EffectiveForm> findById(String effectiveFormId) {
        return Optional.ofNullable(mapper.selectById(effectiveFormId)).map(EffectiveFormAssembler::toDomain);
    }

    /**
     * 按中文名查询同一产品下禁用的生效形态（用于“启用复用”场景）。
     *
     * @param productId           产品 ID
     * @param effectiveFormNameCn 生效形态中文名
     * @return 禁用生效形态（可能为空）
     */
    @Override
    public Optional<EffectiveForm> findDisabledByNameCnInProduct(String productId, String effectiveFormNameCn) {
        if (StringUtils.isBlank(productId) || StringUtils.isBlank(effectiveFormNameCn)) {
            return Optional.empty();
        }
        LambdaQueryWrapper<EntityEffectiveFormDictPo> w =
                new LambdaQueryWrapper<EntityEffectiveFormDictPo>()
                        .eq(EntityEffectiveFormDictPo::getOwnedProductId, productId)
                        .eq(EntityEffectiveFormDictPo::getEffectiveFormNameCn, effectiveFormNameCn)
                        .eq(EntityEffectiveFormDictPo::getEffectiveFormStatus, 0)
                        .last("LIMIT 1");
        return Optional.ofNullable(mapper.selectOne(w)).map(EffectiveFormAssembler::toDomain);
    }

    /**
     * 新增生效形态。
     *
     * @param form 生效形态
     */
    @Override
    public void insert(EffectiveForm form) {
        mapper.insert(EffectiveFormAssembler.toPo(form));
    }

    /**
     * 更新生效形态。
     *
     * @param form 生效形态
     */
    @Override
    public void update(EffectiveForm form) {
        mapper.updateById(EffectiveFormAssembler.toPo(form));
    }

    /**
     * 按产品分页查询启用的生效形态。
     *
     * @param productId 产品 ID
     * @param page      页码（从 1 开始）
     * @param size      页大小
     * @param keyword   关键字（可为空）
     * @return 分页切片
     */
    @Override
    public PageSlice<EffectiveForm> pageByProduct(
            String productId, int page, int size, String keyword) {
        Page<EntityEffectiveFormDictPo> p = new Page<>(page, size);
        LambdaQueryWrapper<EntityEffectiveFormDictPo> w =
                new LambdaQueryWrapper<EntityEffectiveFormDictPo>()
                        .eq(EntityEffectiveFormDictPo::getOwnedProductId, productId)
                        .eq(EntityEffectiveFormDictPo::getEffectiveFormStatus, 1)
                        .orderByDesc(EntityEffectiveFormDictPo::getUpdateTimestamp);
        if (StringUtils.isNotBlank(keyword)) {
            w.and(
                    x ->
                            x.like(EntityEffectiveFormDictPo::getEffectiveFormNameCn, keyword)
                                    .or()
                                    .like(EntityEffectiveFormDictPo::getEffectiveFormNameEn, keyword));
        }
        Page<EntityEffectiveFormDictPo> result = mapper.selectPage(p, w);
        return new PageSlice<>(
                result.getRecords().stream()
                        .map(EffectiveFormAssembler::toDomain)
                        .collect(Collectors.toList()),
                result.getTotal(),
                page,
                size);
    }
}
