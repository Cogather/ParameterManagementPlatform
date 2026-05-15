/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.domain.config.versionfeature.repository;

import com.coretool.param.domain.config.versionfeature.VersionFeature;
import com.coretool.param.domain.support.PageSlice;

import java.util.Optional;

/**
 * 领域仓储接口「VersionFeatureRepository」，定义聚合持久化契约。
 *
 * @since 2026-04-28
 */

public interface VersionFeatureRepository {

    /**
     * 按特性主键查找。
     *
     * @param featureId 特性 ID
     * @return 存在则返回特性
     */
    Optional<VersionFeature> findByFeatureId(String featureId);

    /**
     * 判断同一产品、版本范围内是否已存在相同中文名称的特性（可排除指定特性 ID）。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @param featureNameCn 特性中文名
     * @param excludeFeatureId 排除的特性 ID（可传 null）
     * @return 若已存在则返回 true
     */
    boolean existsSameFeatureNameCnInScope(
            String productId,
            String versionId,
            String featureNameCn,
            String excludeFeatureId);

    /**
     * 同一产品+版本范围内按中文名查找“已删除/未启用”(status=0) 的特性，用于新增时自动恢复。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @param featureNameCn 特性中文名
     * @return 若存在则返回版本特性
     */
    Optional<VersionFeature> findDisabledByNameCnInScope(String productId, String versionId, String featureNameCn);

    /**
     * 新增版本特性记录。
     *
     * @param feature 领域聚合
     */
    void insert(VersionFeature feature);

    /**
     * 更新版本特性记录。
     *
     * @param feature 领域聚合
     */
    void update(VersionFeature feature);

    /**
     * 分页查询某产品、某版本下的版本特性列表。
     *
     * @param productId 产品 ID
     * @param versionId 版本 ID
     * @param page 页码（从 1 开始）
     * @param size 页大小
     * @param keyword 名称关键字
     * @return 分页切片
     */
    PageSlice<VersionFeature> pageByProductAndVersion(
            String productId, String versionId, int page, int size, String keyword);
}
