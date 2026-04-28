package com.coretool.param.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coretool.param.domain.config.category.BusinessCategory;
import com.coretool.param.domain.config.category.repository.BusinessCategoryRepository;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.infrastructure.persistence.assembly.BusinessCategoryAssembler;
import com.coretool.param.infrastructure.persistence.entity.EntityBusinessCategoryPo;
import com.coretool.param.infrastructure.persistence.mapper.EntityBusinessCategoryMapper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class BusinessCategoryRepositoryImpl implements BusinessCategoryRepository {

    private final EntityBusinessCategoryMapper mapper;

    /**
     * 创建业务分类仓储实现。
     *
     * @param mapper MyBatis-Plus Mapper
     */
    public BusinessCategoryRepositoryImpl(EntityBusinessCategoryMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 按分类 ID 查询业务分类。
     *
     * @param categoryId 分类 ID
     * @return 业务分类（可能为空）
     */
    @Override
    public Optional<BusinessCategory> findByCategoryId(String categoryId) {
        return Optional.ofNullable(mapper.selectById(categoryId)).map(BusinessCategoryAssembler::toDomain);
    }

    /**
     * 判断同一产品下是否存在同名且启用的业务分类（按中文名）。
     *
     * @param productId         产品 ID
     * @param categoryNameCn    分类中文名
     * @param excludeCategoryId 排除的分类 ID（可为空）
     * @return 是否存在
     */
    @Override
    public boolean existsSameChineseNameInProduct(
            String productId, String categoryNameCn, String excludeCategoryId) {
        LambdaQueryWrapper<EntityBusinessCategoryPo> w =
                new LambdaQueryWrapper<EntityBusinessCategoryPo>()
                        .eq(EntityBusinessCategoryPo::getOwnedProductId, productId)
                        .eq(EntityBusinessCategoryPo::getCategoryNameCn, categoryNameCn)
                        .eq(EntityBusinessCategoryPo::getCategoryStatus, 1);
        if (excludeCategoryId != null) {
            w.ne(EntityBusinessCategoryPo::getCategoryId, excludeCategoryId);
        }
        Long cnt = mapper.selectCount(w);
        return cnt != null && cnt > 0;
    }

    /**
     * 按中文名查询同一产品下禁用的业务分类（用于“启用复用”场景）。
     *
     * @param productId      产品 ID
     * @param categoryNameCn 分类中文名
     * @return 禁用业务分类（可能为空）
     */
    @Override
    public Optional<BusinessCategory> findDisabledByChineseNameInProduct(String productId, String categoryNameCn) {
        if (StringUtils.isBlank(productId) || StringUtils.isBlank(categoryNameCn)) {
            return Optional.empty();
        }
        LambdaQueryWrapper<EntityBusinessCategoryPo> w =
                new LambdaQueryWrapper<EntityBusinessCategoryPo>()
                        .eq(EntityBusinessCategoryPo::getOwnedProductId, productId)
                        .eq(EntityBusinessCategoryPo::getCategoryNameCn, categoryNameCn)
                        .eq(EntityBusinessCategoryPo::getCategoryStatus, 0)
                        .last("LIMIT 1");
        return Optional.ofNullable(mapper.selectOne(w)).map(BusinessCategoryAssembler::toDomain);
    }

    /**
     * 新增业务分类。
     *
     * @param category 业务分类
     */
    @Override
    public void insert(BusinessCategory category) {
        mapper.insert(BusinessCategoryAssembler.toPo(category));
    }

    /**
     * 更新业务分类。
     *
     * @param category 业务分类
     */
    @Override
    public void update(BusinessCategory category) {
        mapper.updateById(BusinessCategoryAssembler.toPo(category));
    }

    /**
     * 按产品分页查询启用的业务分类。
     *
     * @param productId    产品 ID
     * @param page         页码（从 1 开始）
     * @param size         页大小
     * @param nameKeyword  名称关键字（可为空）
     * @return 分页切片
     */
    @Override
    public PageSlice<BusinessCategory> pageByProduct(
            String productId, int page, int size, String nameKeyword) {
        Page<EntityBusinessCategoryPo> p = new Page<>(page, size);
        LambdaQueryWrapper<EntityBusinessCategoryPo> w =
                new LambdaQueryWrapper<EntityBusinessCategoryPo>()
                        .eq(EntityBusinessCategoryPo::getOwnedProductId, productId)
                        .eq(EntityBusinessCategoryPo::getCategoryStatus, 1)
                        .orderByDesc(EntityBusinessCategoryPo::getUpdateTimestamp);
        if (StringUtils.isNotBlank(nameKeyword)) {
            w.like(EntityBusinessCategoryPo::getCategoryNameCn, nameKeyword);
        }
        Page<EntityBusinessCategoryPo> result = mapper.selectPage(p, w);
        return new PageSlice<>(
                result.getRecords().stream()
                        .map(BusinessCategoryAssembler::toDomain)
                        .collect(Collectors.toList()),
                result.getTotal(),
                page,
                size);
    }
}
