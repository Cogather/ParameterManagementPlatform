package com.coretool.param.domain.config.category.repository;

import com.coretool.param.domain.config.category.BusinessCategory;
import com.coretool.param.domain.support.PageSlice;

import java.util.Optional;

public interface BusinessCategoryRepository {

    Optional<BusinessCategory> findByCategoryId(String categoryId);

    boolean existsSameChineseNameInProduct(String productId, String categoryNameCn, String excludeCategoryId);

    /** 同一产品下按分类中文名查找“已删除/未启用”(status=0) 的记录，用于新增时自动恢复。 */
    Optional<BusinessCategory> findDisabledByChineseNameInProduct(String productId, String categoryNameCn);

    void insert(BusinessCategory category);

    void update(BusinessCategory category);

    PageSlice<BusinessCategory> pageByProduct(String productId, int page, int size, String nameKeyword);
}

