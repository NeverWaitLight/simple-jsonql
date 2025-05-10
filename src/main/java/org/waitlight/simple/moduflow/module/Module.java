package org.waitlight.simple.moduflow.module;

import com.fasterxml.jackson.databind.JsonNode;
import org.waitlight.simple.moduflow.DataPool;

/**
 * 模块接口 - 所有模块必须实现
 */
public interface Module {
    /**
     * 执行模块功能
     *
     * @param params   模块参数
     * @param dataPool 数据池
     * @throws Exception 模块执行异常
     */
    void execute(JsonNode params, DataPool dataPool) throws Exception;
}
