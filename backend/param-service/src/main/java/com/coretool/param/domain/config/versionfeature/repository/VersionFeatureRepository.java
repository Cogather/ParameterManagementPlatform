package com.coretool.param.domain.config.versionfeature.repository;

import com.coretool.param.domain.config.versionfeature.VersionFeature;
import com.coretool.param.domain.support.PageSlice;

import java.util.Optional;

public interface VersionFeatureRepository {

    Optional<VersionFeature> findByFeatureId(String featureId);

    boolean existsSameFeatureNameCnInScope(
            String productId,
            String versionId,
            String featureNameCn,
            String excludeFeatureId);

    /** 同一产品+版本范围内按中文名查找“已删除/未启用”(status=0) 的特性，用于新增时自动恢复。 */
    Optional<VersionFeature> findDisabledByNameCnInScope(String productId, String versionId, String featureNameCn);

    void insert(VersionFeature feature);

    void update(VersionFeature feature);

    PageSlice<VersionFeature> pageByProductAndVersion(
            String productId, String versionId, int page, int size, String keyword);
}

