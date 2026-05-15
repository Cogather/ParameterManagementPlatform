/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.domain.config.effectiveform.repository;

import com.coretool.param.domain.config.effectiveform.EffectiveForm;
import com.coretool.param.domain.support.PageSlice;

import java.util.Optional;

/**
 * 领域仓储接口「EffectiveFormRepository」，定义聚合持久化契约。
 *
 * @since 2026-04-28
 */

public interface EffectiveFormRepository {

/**
 * findById。
 *
 * @param effectiveFormId 见方法签名
 * @return 可选结果
 */

    Optional<EffectiveForm> findById(String effectiveFormId);

    /**
     * 同一产品下按中文名查找“已删除/未启用”(status=0) 的记录，用于新增时自动恢复。
     *
     * @param productId 产品 ID
     * @param effectiveFormNameCn 生效形态中文名
     * @return 若存在则返回生效形态
     */
    Optional<EffectiveForm> findDisabledByNameCnInProduct(String productId, String effectiveFormNameCn);

/**
 * insert。
 *
 * @param form 见方法签名
 */

    void insert(EffectiveForm form);

/**
 * update。
 *
 * @param form 见方法签名
 */

    void update(EffectiveForm form);

/**
 * pageByProduct。
 *
 * @param productId 见方法签名
 * @param page 见方法签名
 * @param size 见方法签名
 * @param keyword 见方法签名
 * @return 结果
 */

    PageSlice<EffectiveForm> pageByProduct(String productId, int page, int size, String keyword);
}
