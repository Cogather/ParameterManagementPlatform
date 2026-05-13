package com.coretool.param.domain.config.category.repository;

import com.coretool.param.domain.config.category.BusinessCategory;
import com.coretool.param.domain.support.PageSlice;

import java.util.Optional;

/**
 * 领域仓储接口「BusinessCategoryRepository」，定义聚合持久化契约。
 *
 * @since 2026-04-28
 */

public interface BusinessCategoryRepository {

/**
 * findByCategoryId。
 *
 * @param categoryId 见方法签名
 * @return 可选结果
 */

    Optional<BusinessCategory> findByCategoryId(String categoryId);

/**
 * existsSameChineseNameInProduct。
 *
 * @param productId 见方法签名
 * @param categoryNameCn 见方法签名
 * @param excludeCategoryId 见方法签名
 * @return 布尔结果
 */

    boolean existsSameChineseNameInProduct(String productId, String categoryNameCn, String excludeCategoryId);

    /**
     * 同一产品下按分类中文名查找“已删除/未启用”(status=0) 的记录，用于新增时自动恢复。
     *
     * @param productId 产品 ID
     * @param categoryNameCn 业务分类中文名
     * @return 若存在则返回业务分类
     */
    Optional<BusinessCategory> findDisabledByChineseNameInProduct(String productId, String categoryNameCn);

/**
 * insert。
 *
 * @param category 见方法签名
 */

    void insert(BusinessCategory category);

/**
 * update。
 *
 * @param category 见方法签名
 */

    void update(BusinessCategory category);

/**
 * pageByProduct。
 *
 * @param productId 见方法签名
 * @param page 见方法签名
 * @param size 见方法签名
 * @param nameKeyword 见方法签名
 * @return 结果
 */

    PageSlice<BusinessCategory> pageByProduct(String productId, int page, int size, String nameKeyword);
}
