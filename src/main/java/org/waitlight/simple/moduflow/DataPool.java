package org.waitlight.simple.moduflow;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据池 - 存储模块间共享数据
 */
public class DataPool {
    private final Map<String, Object> passParams = new ConcurrentHashMap<>();
    private final Map<String, Object> returnParams = new ConcurrentHashMap<>();

    public DataPool(Map<String, Object> inputParams) {
        this.passParams.putAll(inputParams);
    }

    public void addPassParam(String key, Object value) {
        passParams.put(key, value);
    }

    public Object getPassParam(String key) {
        return passParams.get(key);
    }

    public void addReturnParam(String key, Object value) {
        returnParams.put(key, value);
    }

    public JsonNode getReturnParams() {
        // 实际实现需要将returnParams转换为JsonNode
        return null; 
    }
}
