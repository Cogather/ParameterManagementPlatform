package com.coretool.param.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coretool.param.domain.config.version.ProductVersion;
import com.coretool.param.domain.config.version.repository.ProductVersionRepository;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.infrastructure.persistence.assembly.ProductVersionAssembler;
import com.coretool.param.infrastructure.persistence.entity.EntityVersionInfoPo;
import com.coretool.param.infrastructure.persistence.mapper.EntityVersionInfoMapper;

import org.springframework.stereotype.Repository;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ProductVersionRepositoryImpl implements ProductVersionRepository {

    private final EntityVersionInfoMapper mapper;

    /**
     * 创建产品版本仓储实现。
     *
     * @param mapper MyBatis-Plus Mapper
     */
    public ProductVersionRepositoryImpl(EntityVersionInfoMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 按版本 ID 查询产品版本。
     *
     * @param versionId 版本 ID
     * @return 产品版本（可能为空）
     */
    @Override
    public Optional<ProductVersion> findById(String versionId) {
        EntityVersionInfoPo po = mapper.selectById(versionId);
        return Optional.ofNullable(po).map(ProductVersionAssembler::toDomain);
    }

    /**
     * 判断同一产品下是否存在同名且启用的版本。
     *
     * @param productId       产品 ID
     * @param versionName     版本名称
     * @param excludeVersionId 排除的版本 ID（可为空）
     * @return 是否存在
     */
    @Override
    public boolean existsSameNameInProduct(String productId, String versionName, String excludeVersionId) {
        LambdaQueryWrapper<EntityVersionInfoPo> w =
                new LambdaQueryWrapper<EntityVersionInfoPo>()
                        .eq(EntityVersionInfoPo::getOwnedProductId, productId)
                        .eq(EntityVersionInfoPo::getVersionName, versionName)
                        .eq(EntityVersionInfoPo::getVersionStatus, 1);
        if (excludeVersionId != null) {
            w.ne(EntityVersionInfoPo::getVersionId, excludeVersionId);
        }
        Long cnt = mapper.selectCount(w);
        return cnt != null && cnt > 0;
    }

    /**
     * 按名称查询同一产品下禁用的版本（用于“启用复用”场景）。
     *
     * @param productId   产品 ID
     * @param versionName 版本名称
     * @return 禁用版本（可能为空）
     */
    @Override
    public Optional<ProductVersion> findDisabledByNameInProduct(String productId, String versionName) {
        if (StringUtils.isBlank(productId) || StringUtils.isBlank(versionName)) {
            return Optional.empty();
        }
        EntityVersionInfoPo po =
                mapper.selectOne(
                        new LambdaQueryWrapper<EntityVersionInfoPo>()
                                .eq(EntityVersionInfoPo::getOwnedProductId, productId)
                                .eq(EntityVersionInfoPo::getVersionName, versionName)
                                .eq(EntityVersionInfoPo::getVersionStatus, 0)
                                .last("LIMIT 1"));
        return Optional.ofNullable(po).map(ProductVersionAssembler::toDomain);
    }

    /**
     * 新增产品版本。
     *
     * @param version 产品版本
     */
    @Override
    public void insert(ProductVersion version) {
        mapper.insert(ProductVersionAssembler.toPo(version));
    }

    /**
     * 更新产品版本。
     *
     * @param version 产品版本
     */
    @Override
    public void update(ProductVersion version) {
        mapper.updateById(ProductVersionAssembler.toPo(version));
    }

    /**
     * 按产品分页查询启用的版本。
     *
     * @param productId 产品 ID
     * @param page      页码（从 1 开始）
     * @param size      页大小
     * @return 分页切片
     */
    @Override
    public PageSlice<ProductVersion> pageByProduct(String productId, int page, int size) {
        Page<EntityVersionInfoPo> p = new Page<>(page, size);
        Page<EntityVersionInfoPo> result =
                mapper.selectPage(
                        p,
                        new LambdaQueryWrapper<EntityVersionInfoPo>()
                                .eq(EntityVersionInfoPo::getOwnedProductId, productId)
                                .eq(EntityVersionInfoPo::getVersionStatus, 1)
                                .orderByDesc(EntityVersionInfoPo::getUpdateTimestamp));
        return new PageSlice<>(
                result.getRecords().stream()
                        .map(ProductVersionAssembler::toDomain)
                        .collect(Collectors.toList()),
                result.getTotal(),
                page,
                size);
    }
}
