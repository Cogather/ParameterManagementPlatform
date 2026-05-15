/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2012-2026. All rights reserved.
 */

package com.coretool.param.ui.vo;

import lombok.Data;

/**
 * 分页查询参数，并支持按归属类型 ID 过滤（如类型版本区段列表）。
 *
 * @since 2026-04-28
 */
@Data
public class ListPageWithTypeFilterQuery {
    private int page = 1;
    private int size = 20;
    private String ownedTypeId;

    /**
     * 构造分页 + 类型过滤查询对象。
     *
     * @param page        页码（从 1 开始）
     * @param size        页大小
     * @param ownedTypeId 归属类型 ID（可 null）
     * @return 查询对象
     */
    public static ListPageWithTypeFilterQuery of(int page, int size, String ownedTypeId) {
        ListPageWithTypeFilterQuery q = new ListPageWithTypeFilterQuery();
        q.setPage(page);
        q.setSize(size);
        q.setOwnedTypeId(ownedTypeId);
        return q;
    }
}
