package org.waitlight.simple.moduflow.module;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模块注册表 - 管理所有可用模块
 */
public class ModuleRegistry {
    private final Map<String, Module> modules = new ConcurrentHashMap<>();

    /**
     * 注册模块
     *
     * @param moduleType 模块类型名称
     * @param module     模块实例
     */
    public void registerModule(String moduleType, Module module) {
        modules.put(moduleType, module);
    }

    /**
     * 获取模块实例
     *
     * @param moduleType 模块类型名称
     * @return 模块实例，未找到返回null
     */
    public Module getModule(String moduleType) {
        return modules.get(moduleType);
    }
}
