package com.coretool.param.domain.support;

import java.util.List;

/**
 * 领域分页切片（不含 UI 层类型）。
 */
public class PageSlice<T> {
    private final List<T> records;
    private final long total;
    private final int page;
    private final int size;

    /**
     * 创建分页切片。
     *
     * @param records 当前页记录
     * @param total   总记录数
     * @param page    页码（从 1 开始）
     * @param size    页大小
     */
    public PageSlice(List<T> records, long total, int page, int size) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    /**
     * 获取当前页记录。
     *
     * @return 当前页记录
     */
    public List<T> getRecords() {
        return records;
    }

    /**
     * 获取总记录数。
     *
     * @return 总记录数
     */
    public long getTotal() {
        return total;
    }

    /**
     * 获取页码（从 1 开始）。
     *
     * @return 页码
     */
    public int getPage() {
        return page;
    }

    /**
     * 获取页大小。
     *
     * @return 页大小
     */
    public int getSize() {
        return size;
    }
}
