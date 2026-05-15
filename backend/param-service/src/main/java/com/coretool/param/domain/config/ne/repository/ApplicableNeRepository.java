/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.domain.config.ne.repository;

import com.coretool.param.domain.config.ne.ApplicableNe;
import com.coretool.param.domain.support.PageSlice;

import java.util.Optional;

/**
 * 领域仓储接口「ApplicableNeRepository」，定义聚合持久化契约。
 *
 * @since 2026-04-28
 */

public interface ApplicableNeRepository {

/**
 * findByNeTypeId。
 *
 * @param neTypeId 见方法签名
 * @return 可选结果
 */

    Optional<ApplicableNe> findByNeTypeId(String neTypeId);

/**
 * existsSameNameInProduct。
 *
 * @param productId 见方法签名
 * @param neTypeNameCn 见方法签名
 * @param excludeNeTypeId 见方法签名
 * @return 布尔结果
 */

    boolean existsSameNameInProduct(String productId, String neTypeNameCn, String excludeNeTypeId);

    /**
     * 同一产品下按网元名称查找“已删除/未启用”(status=0) 的记录，用于新增时自动恢复。
     *
     * @param productId 产品 ID
     * @param neTypeNameCn 网元类型中文名称
     * @return 若存在则返回适用网元字典项
     */
    Optional<ApplicableNe> findDisabledByNameInProduct(String productId, String neTypeNameCn);

/**
 * insert。
 *
 * @param ne 见方法签名
 */

    void insert(ApplicableNe ne);

/**
 * update。
 *
 * @param ne 见方法签名
 */

    void update(ApplicableNe ne);

/**
 * pageByProduct。
 *
 * @param productId 见方法签名
 * @param page 见方法签名
 * @param size 见方法签名
 * @param nameKeyword 见方法签名
 * @return 结果
 */

    PageSlice<ApplicableNe> pageByProduct(String productId, int page, int size, String nameKeyword);
}
