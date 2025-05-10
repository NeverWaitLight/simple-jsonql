package org.waitlight.simple.jsonql.model.dto;

import java.util.List;

/**
 * 分页查询响应对象
 *
 * @param <T> 数据项类型
 */
public class PageResponse<T> {
    private int code;
    private String message;
    private List<T> data;
    private long total;
    private int page;
    private int size;
    private int totalPages;

    public PageResponse() {
    }

    public PageResponse(int code, String message, List<T> data, long total, int page, int size, int totalPages) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.total = total;
        this.page = page;
        this.size = size;
        this.totalPages = totalPages;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
} 