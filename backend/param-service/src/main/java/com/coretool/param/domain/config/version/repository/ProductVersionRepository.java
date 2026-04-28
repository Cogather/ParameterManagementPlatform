package com.coretool.param.domain.config.version.repository;

import com.coretool.param.domain.config.version.ProductVersion;
import com.coretool.param.domain.support.PageSlice;

import java.util.Optional;

public interface ProductVersionRepository {

    Optional<ProductVersion> findById(String versionId);

    boolean existsSameNameInProduct(String productId, String versionName, String excludeVersionId);

    /** 同一产品下按版本名称查找“已删除/未启用”(status=0) 的记录，用于新增时自动恢复。 */
    Optional<ProductVersion> findDisabledByNameInProduct(String productId, String versionName);

    void insert(ProductVersion version);

    void update(ProductVersion version);

    PageSlice<ProductVersion> pageByProduct(String productId, int page, int size);
}

