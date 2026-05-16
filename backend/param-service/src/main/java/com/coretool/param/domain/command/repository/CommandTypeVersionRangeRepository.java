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
     * 按区段 ID 查询类型版本区段。
     *
     * @param rangeId 区段 ID
     * @return 区段聚合；不存在时为空
     */
    Optional<CommandTypeVersionRange> findById(String rangeId);

    /**
     * 与候选区段同一产品/命令/类型/版本作用域下的启用区段，用于序号区间重叠校验。
     *
     * @param productId                  产品 ID
     * @param ownedCommandId             归属命令 ID
     * @param ownedTypeId                归属类型 ID
     * @param ownedVersionOrBusinessId   归属版本或业务 ID
     * @return 启用区段列表
     */
    List<CommandTypeVersionRange> listEnabledInScope(
            String productId, String ownedCommandId, String ownedTypeId, String ownedVersionOrBusinessId);

    /**
     * 插入类型版本区段。
     *
     * @param range 区段聚合
     */
    void insert(CommandTypeVersionRange range);

    /**
     * 更新类型版本区段。
     *
     * @param range 区段聚合
     */
    void update(CommandTypeVersionRange range);

    /**
     * 按产品分页查询类型版本区段。
     *
     * @param productId          产品 ID
     * @param page               页码（从 1 开始）
     * @param size               页大小
     * @param ownedTypeIdFilter  类型 ID 过滤（可为空表示不过滤）
     * @return 分页切片
     */
    PageSlice<CommandTypeVersionRange> pageByProduct(String productId, int page, int size, String ownedTypeIdFilter);
}
