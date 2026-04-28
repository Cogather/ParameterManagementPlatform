package com.coretool.param.ui.vo;

import lombok.Data;

/**
 * 分页 + 按归属类型 ID 过滤（如类型版本区段列表）。
 */
@Data
public class ListPageWithTypeFilterQuery {
    private int page = 1;
    private int size = 20;
    private String ownedTypeId;

    public static ListPageWithTypeFilterQuery of(int page, int size, String ownedTypeId) {
        ListPageWithTypeFilterQuery q = new ListPageWithTypeFilterQuery();
        q.setPage(page);
        q.setSize(size);
        q.setOwnedTypeId(ownedTypeId);
        return q;
    }
}
