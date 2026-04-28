package com.coretool.param.ui.vo;

import lombok.Data;

/**
 * 通用列表分页 + 关键字（产品/版本下字典分页等场景复用 query 参数绑定）。
 */
@Data
public class ListPageWithKeywordQuery {
    private int page = 1;
    private int size = 20;
    private String keyword;

    /** 供导出/全量拉取等场景从分页参数构造。 */
    public static ListPageWithKeywordQuery of(int page, int size, String keyword) {
        ListPageWithKeywordQuery q = new ListPageWithKeywordQuery();
        q.setPage(page);
        q.setSize(size);
        q.setKeyword(keyword);
        return q;
    }
}
