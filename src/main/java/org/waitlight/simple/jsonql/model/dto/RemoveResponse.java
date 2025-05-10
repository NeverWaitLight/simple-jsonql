package org.waitlight.simple.jsonql.model.dto;

/**
 * 数据删除响应
 */
public class RemoveResponse {

    /**
     * 删除的记录数量
     */
    private int count;

    public RemoveResponse() {
    }

    public RemoveResponse(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
} 