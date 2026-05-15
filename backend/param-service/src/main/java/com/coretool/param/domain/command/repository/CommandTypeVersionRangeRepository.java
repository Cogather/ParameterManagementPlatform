/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.domain.command.repository;

import com.coretool.param.domain.command.CommandTypeVersionRange;
import com.coretool.param.domain.support.PageSlice;

import java.util.List;
import java.util.Optional;

/**
 * 领域仓储接口「CommandTypeVersionRangeRepository」，定义聚合持久化契约。
 *
 * @since 2026-04-28
 */

public interface CommandTypeVersionRangeRepository {

/**
 * findById。
 *
 * @param rangeId 见方法签名
 * @return 可选结果
 */

    Optional<CommandTypeVersionRange> findById(String rangeId);

    /**
     * 与候选区段同一产品/命令/类型/版本作用域下的启用区段，用于序号区间重叠校验。
     */
    List<CommandTypeVersionRange> listEnabledInScope(
            String productId, String ownedCommandId, String ownedTypeId, String ownedVersionOrBusinessId);

/**
 * insert。
 *
 * @param range 见方法签名
 */

    void insert(CommandTypeVersionRange range);

/**
 * update。
 *
 * @param range 见方法签名
 */

    void update(CommandTypeVersionRange range);

/**
 * pageByProduct。
 *
 * @param productId 见方法签名
 * @param page 见方法签名
 * @param size 见方法签名
 * @param ownedTypeIdFilter 见方法签名
 * @return 结果
 */

    PageSlice<CommandTypeVersionRange> pageByProduct(String productId, int page, int size, String ownedTypeIdFilter);
}
