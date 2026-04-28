package com.coretool.param.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coretool.param.domain.config.ne.ApplicableNe;
import com.coretool.param.domain.config.ne.repository.ApplicableNeRepository;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.infrastructure.persistence.assembly.ApplicableNeAssembler;
import com.coretool.param.infrastructure.persistence.entity.EntityApplicableNeDictPo;
import com.coretool.param.infrastructure.persistence.mapper.EntityApplicableNeDictMapper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ApplicableNeRepositoryImpl implements ApplicableNeRepository {

    private final EntityApplicableNeDictMapper mapper;

    /**
     * 创建适用网元仓储实现。
     *
     * @param mapper MyBatis-Plus Mapper
     */
    public ApplicableNeRepositoryImpl(EntityApplicableNeDictMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 按网元类型 ID 查询网元条目。
     *
     * @param neTypeId 网元类型 ID
     * @return 网元条目（可能为空）
     */
    @Override
    public Optional<ApplicableNe> findByNeTypeId(String neTypeId) {
        return Optional.ofNullable(mapper.selectById(neTypeId)).map(ApplicableNeAssembler::toDomain);
    }

    /**
     * 判断同一产品下是否存在同名且启用的网元条目。
     *
     * @param productId        产品 ID
     * @param neTypeNameCn     网元中文名
     * @param excludeNeTypeId  排除的网元类型 ID（可为空）
     * @return 是否存在
     */
    @Override
    public boolean existsSameNameInProduct(String productId, String neTypeNameCn, String excludeNeTypeId) {
        LambdaQueryWrapper<EntityApplicableNeDictPo> w =
                new LambdaQueryWrapper<EntityApplicableNeDictPo>()
                        .eq(EntityApplicableNeDictPo::getOwnedProductId, productId)
                        .eq(EntityApplicableNeDictPo::getNeTypeNameCn, neTypeNameCn)
                        .eq(EntityApplicableNeDictPo::getNeTypeStatus, 1);
        if (excludeNeTypeId != null) {
            w.ne(EntityApplicableNeDictPo::getNeTypeId, excludeNeTypeId);
        }
        Long c = mapper.selectCount(w);
        return c != null && c > 0;
    }

    /**
     * 按名称查询同一产品下禁用的网元条目（用于“启用复用”场景）。
     *
     * @param productId    产品 ID
     * @param neTypeNameCn 网元中文名
     * @return 禁用网元条目（可能为空）
     */
    @Override
    public Optional<ApplicableNe> findDisabledByNameInProduct(String productId, String neTypeNameCn) {
        if (StringUtils.isBlank(productId) || StringUtils.isBlank(neTypeNameCn)) {
            return Optional.empty();
        }
        LambdaQueryWrapper<EntityApplicableNeDictPo> w =
                new LambdaQueryWrapper<EntityApplicableNeDictPo>()
                        .eq(EntityApplicableNeDictPo::getOwnedProductId, productId)
                        .eq(EntityApplicableNeDictPo::getNeTypeNameCn, neTypeNameCn)
                        .eq(EntityApplicableNeDictPo::getNeTypeStatus, 0)
                        .last("LIMIT 1");
        return Optional.ofNullable(mapper.selectOne(w)).map(ApplicableNeAssembler::toDomain);
    }

    /**
     * 新增网元条目。
     *
     * @param ne 网元条目
     */
    @Override
    public void insert(ApplicableNe ne) {
        mapper.insert(ApplicableNeAssembler.toPo(ne));
    }

    /**
     * 更新网元条目。
     *
     * @param ne 网元条目
     */
    @Override
    public void update(ApplicableNe ne) {
        mapper.updateById(ApplicableNeAssembler.toPo(ne));
    }

    /**
     * 按产品分页查询启用的网元条目。
     *
     * @param productId    产品 ID
     * @param page         页码（从 1 开始）
     * @param size         页大小
     * @param nameKeyword  名称关键字（可为空）
     * @return 分页切片
     */
    @Override
    public PageSlice<ApplicableNe> pageByProduct(
            String productId, int page, int size, String nameKeyword) {
        Page<EntityApplicableNeDictPo> p = new Page<>(page, size);
        LambdaQueryWrapper<EntityApplicableNeDictPo> w =
                new LambdaQueryWrapper<EntityApplicableNeDictPo>()
                        .eq(EntityApplicableNeDictPo::getOwnedProductId, productId)
                        .eq(EntityApplicableNeDictPo::getNeTypeStatus, 1)
                        .orderByDesc(EntityApplicableNeDictPo::getUpdateTimestamp);
        if (StringUtils.isNotBlank(nameKeyword)) {
            w.like(EntityApplicableNeDictPo::getNeTypeNameCn, nameKeyword);
        }
        Page<EntityApplicableNeDictPo> result = mapper.selectPage(p, w);
        return new PageSlice<>(
                result.getRecords().stream()
                        .map(ApplicableNeAssembler::toDomain)
                        .collect(Collectors.toList()),
                result.getTotal(),
                page,
                size);
    }
}
