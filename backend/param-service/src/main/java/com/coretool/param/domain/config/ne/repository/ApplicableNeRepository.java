package com.coretool.param.domain.config.ne.repository;

import com.coretool.param.domain.config.ne.ApplicableNe;
import com.coretool.param.domain.support.PageSlice;

import java.util.Optional;

public interface ApplicableNeRepository {

    Optional<ApplicableNe> findByNeTypeId(String neTypeId);

    boolean existsSameNameInProduct(String productId, String neTypeNameCn, String excludeNeTypeId);

    /** 同一产品下按网元名称查找“已删除/未启用”(status=0) 的记录，用于新增时自动恢复。 */
    Optional<ApplicableNe> findDisabledByNameInProduct(String productId, String neTypeNameCn);

    void insert(ApplicableNe ne);

    void update(ApplicableNe ne);

    PageSlice<ApplicableNe> pageByProduct(String productId, int page, int size, String nameKeyword);
}

