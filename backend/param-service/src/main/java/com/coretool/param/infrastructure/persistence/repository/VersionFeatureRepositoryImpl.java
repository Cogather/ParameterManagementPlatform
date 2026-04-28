package com.coretool.param.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coretool.param.domain.config.versionfeature.VersionFeature;
import com.coretool.param.domain.config.versionfeature.repository.VersionFeatureRepository;
import com.coretool.param.domain.support.PageSlice;
import com.coretool.param.infrastructure.persistence.assembly.VersionFeatureAssembler;
import com.coretool.param.infrastructure.persistence.entity.VersionFeatureDictPo;
import com.coretool.param.infrastructure.persistence.mapper.VersionFeatureDictMapper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class VersionFeatureRepositoryImpl implements VersionFeatureRepository {

    private final VersionFeatureDictMapper mapper;

    /**
     * 创建版本特性仓储实现。
     *
     * @param mapper MyBatis-Plus Mapper
     */
    public VersionFeatureRepositoryImpl(VersionFeatureDictMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 按特性 ID 查询版本特性。
     *
     * @param featureId 特性 ID
     * @return 版本特性（可能为空）
     */
    @Override
    public Optional<VersionFeature> findByFeatureId(String featureId) {
        return Optional.ofNullable(mapper.selectById(featureId)).map(VersionFeatureAssembler::toDomain);
    }

    /**
     * 判断同一产品+版本范围内是否存在同名且启用的特性（按中文名）。
     *
     * @param productId       产品 ID
     * @param versionId       版本 ID
     * @param featureNameCn   特性中文名
     * @param excludeFeatureId 排除的特性 ID（可为空）
     * @return 是否存在
     */
    @Override
    public boolean existsSameFeatureNameCnInScope(
            String productId,
            String versionId,
            String featureNameCn,
            String excludeFeatureId) {
        LambdaQueryWrapper<VersionFeatureDictPo> w =
                new LambdaQueryWrapper<VersionFeatureDictPo>()
                        .eq(VersionFeatureDictPo::getOwnedProductPbiId, productId)
                        .eq(VersionFeatureDictPo::getOwnedVersionId, versionId)
                        .eq(VersionFeatureDictPo::getFeatureNameCn, featureNameCn)
                        .eq(VersionFeatureDictPo::getFeatureStatus, 1);
        if (excludeFeatureId != null) {
            w.ne(VersionFeatureDictPo::getFeatureId, excludeFeatureId);
        }
        Long c = mapper.selectCount(w);
        return c != null && c > 0;
    }

    /**
     * 按中文名查询同一产品+版本范围内禁用的特性（用于“启用复用”场景）。
     *
     * @param productId     产品 ID
     * @param versionId     版本 ID
     * @param featureNameCn 特性中文名
     * @return 禁用特性（可能为空）
     */
    @Override
    public Optional<VersionFeature> findDisabledByNameCnInScope(String productId, String versionId, String featureNameCn) {
        if (StringUtils.isBlank(productId) || StringUtils.isBlank(versionId) || StringUtils.isBlank(featureNameCn)) {
            return Optional.empty();
        }
        LambdaQueryWrapper<VersionFeatureDictPo> w =
                new LambdaQueryWrapper<VersionFeatureDictPo>()
                        .eq(VersionFeatureDictPo::getOwnedProductPbiId, productId)
                        .eq(VersionFeatureDictPo::getOwnedVersionId, versionId)
                        .eq(VersionFeatureDictPo::getFeatureNameCn, featureNameCn)
                        .eq(VersionFeatureDictPo::getFeatureStatus, 0)
                        .last("LIMIT 1");
        return Optional.ofNullable(mapper.selectOne(w)).map(VersionFeatureAssembler::toDomain);
    }

    /**
     * 新增版本特性。
     *
     * @param feature 版本特性
     */
    @Override
    public void insert(VersionFeature feature) {
        mapper.insert(VersionFeatureAssembler.toPo(feature));
    }

    /**
     * 更新版本特性。
     *
     * @param feature 版本特性
     */
    @Override
    public void update(VersionFeature feature) {
        mapper.updateById(VersionFeatureAssembler.toPo(feature));
    }

    /**
     * 按产品与版本分页查询启用的特性。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @param page      页码（从 1 开始）
     * @param size      页大小
     * @param keyword   关键字（可为空）
     * @return 分页切片
     */
    @Override
    public PageSlice<VersionFeature> pageByProductAndVersion(
            String productId, String versionId, int page, int size, String keyword) {
        Page<VersionFeatureDictPo> p = new Page<>(page, size);
        LambdaQueryWrapper<VersionFeatureDictPo> w =
                new LambdaQueryWrapper<VersionFeatureDictPo>()
                        .eq(VersionFeatureDictPo::getOwnedProductPbiId, productId)
                        .eq(VersionFeatureDictPo::getOwnedVersionId, versionId)
                        .eq(VersionFeatureDictPo::getFeatureStatus, 1)
                        .orderByDesc(VersionFeatureDictPo::getUpdateTimestamp);
        if (StringUtils.isNotBlank(keyword)) {
            w.like(VersionFeatureDictPo::getFeatureNameCn, keyword);
        }
        Page<VersionFeatureDictPo> result = mapper.selectPage(p, w);
        return new PageSlice<>(
                result.getRecords().stream()
                        .map(VersionFeatureAssembler::toDomain)
                        .collect(Collectors.toList()),
                result.getTotal(),
                page,
                size);
    }
}
