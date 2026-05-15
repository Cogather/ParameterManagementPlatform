/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.domain.config.effectivemode.repository;

import com.coretool.param.domain.config.effectivemode.EffectiveMode;
import com.coretool.param.domain.support.PageSlice;

import java.util.Optional;

/**
 * 领域仓储接口「EffectiveModeRepository」，定义聚合持久化契约。
 *
 * @since 2026-04-28
 */

public interface EffectiveModeRepository {

/**
 * findById。
 *
 * @param effectiveModeId 见方法签名
 * @return 可选结果
 */

    Optional<EffectiveMode> findById(String effectiveModeId);

    /**
     * 同一产品下按中文名查找“已删除/未启用”(status=0) 的记录，用于新增时自动恢复。
     *
     * @param productId 产品 ID
     * @param effectiveModeNameCn 生效方式中文名
     * @return 若存在则返回生效方式
     */
    Optional<EffectiveMode> findDisabledByNameCnInProduct(String productId, String effectiveModeNameCn);

/**
 * insert。
 *
 * @param mode 见方法签名
 */

    void insert(EffectiveMode mode);

/**
 * update。
 *
 * @param mode 见方法签名
 */

    void update(EffectiveMode mode);

/**
 * pageByProduct。
 *
 * @param productId 见方法签名
 * @param page 见方法签名
 * @param size 见方法签名
 * @param keyword 见方法签名
 * @return 结果
 */

    PageSlice<EffectiveMode> pageByProduct(String productId, int page, int size, String keyword);
}
