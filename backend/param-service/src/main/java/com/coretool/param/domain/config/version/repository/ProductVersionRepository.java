package com.coretool.param.domain.config.version.repository;

import com.coretool.param.domain.config.version.ProductVersion;
import com.coretool.param.domain.support.PageSlice;

import java.util.Optional;

/**
 * 领域仓储接口「ProductVersionRepository」，定义聚合持久化契约。
 *
 * @since 2026-04-28
 */

public interface ProductVersionRepository {

/**
 * findById。
 *
 * @param versionId 见方法签名
 * @return 可选结果
 */

    Optional<ProductVersion> findById(String versionId);

/**
 * existsSameNameInProduct。
 *
 * @param productId 见方法签名
 * @param versionName 见方法签名
 * @param excludeVersionId 见方法签名
 * @return 布尔结果
 */

    boolean existsSameNameInProduct(String productId, String versionName, String excludeVersionId);

    /**
     * 同一产品下按版本名称查找“已删除/未启用”(status=0) 的记录，用于新增时自动恢复。
     *
     * @param productId 产品 ID
     * @param versionName 版本名称
     * @return 若存在则返回产品版本
     */
    Optional<ProductVersion> findDisabledByNameInProduct(String productId, String versionName);

/**
 * insert。
 *
 * @param version 见方法签名
 */

    void insert(ProductVersion version);

/**
 * update。
 *
 * @param version 见方法签名
 */

    void update(ProductVersion version);

/**
 * pageByProduct。
 *
 * @param productId 见方法签名
 * @param page 见方法签名
 * @param size 见方法签名
 * @return 结果
 */

    PageSlice<ProductVersion> pageByProduct(String productId, int page, int size);
}
