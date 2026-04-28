package com.coretool.param.ui.response;

import java.util.List;

public class PageResponse<T> {
    private List<T> records;
    private long total;
    private int page;
    private int size;

    /**
     * 获取当前页记录。
     *
     * @return 当前页记录
     */
    public List<T> getRecords() {
        return records;
    }

    /**
     * 设置当前页记录。
     *
     * @param records 当前页记录
     */
    public void setRecords(List<T> records) {
        this.records = records;
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
     * 设置总记录数。
     *
     * @param total 总记录数
     */
    public void setTotal(long total) {
        this.total = total;
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
     * 设置页码（从 1 开始）。
     *
     * @param page 页码
     */
    public void setPage(int page) {
        this.page = page;
    }

    /**
     * 获取页大小。
     *
     * @return 页大小
     */
    public int getSize() {
        return size;
    }

    /**
     * 设置页大小。
     *
     * @param size 页大小
     */
    public void setSize(int size) {
        this.size = size;
    }
}

