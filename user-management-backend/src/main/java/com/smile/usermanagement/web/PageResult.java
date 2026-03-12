package com.smile.usermanagement.web;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;

public class PageResult<T> {

    private List<T> items;
    private long total;
    private long page;
    private long size;

    public static <T> PageResult<T> from(IPage<T> p) {
        PageResult<T> r = new PageResult<>();
        r.setItems(p.getRecords());
        r.setTotal(p.getTotal());
        r.setPage(p.getCurrent());
        r.setSize(p.getSize());
        return r;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getPage() {
        return page;
    }

    public void setPage(long page) {
        this.page = page;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
