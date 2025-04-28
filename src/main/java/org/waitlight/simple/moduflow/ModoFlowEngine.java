package org.waitlight.simple.moduflow;

import com.fasterxml.jackson.databind.JsonNode;
import org.waitlight.simple.moduflow.exception.ModuleException;
import org.waitlight.simple.moduflow.module.Module;
import org.waitlight.simple.moduflow.module.ModuleRegistry;

import java.util.Map;

/**
 * ModoFlow核心引擎
 */
public class ModoFlowEngine {
    private final ModuleRegistry moduleRegistry;

    public ModoFlowEngine() {
        this.moduleRegistry = new ModuleRegistry();
    }

    /**
     * 执行JSON定义的工作流
     *
     * @param workflowJson 工作流JSON定义
     * @param inputParams  输入参数
     * @return 执行结果
     */
    public JsonNode executeWorkflow(JsonNode workflowJson, Map<String, Object> inputParams) {
        DataPool dataPool = new DataPool(inputParams);

        // 解析并执行工作流中的每个模块
        for (JsonNode moduleNode : workflowJson.path("modules")) {
            String moduleName = moduleNode.path("name").asText();
            String moduleType = moduleNode.path("module").asText();

            Module module = moduleRegistry.getModule(moduleType);
            if (module == null) {
                throw new ModuleException("Module not found: " + moduleType);
            }

            try {
                module.execute(moduleNode.path("params"), dataPool);
            } catch (Exception e) {
                handleModuleError(moduleNode, e, dataPool);
            }
        }

        return dataPool.getReturnParams();
    }

    private void handleModuleError(JsonNode moduleNode, Exception e, DataPool dataPool) {
        // 实现错误处理逻辑
        if (moduleNode.has("error_handling")) {
            // 根据配置处理错误
        } else {
            throw new ModuleException("Module execution failed: " + moduleNode.path("name").asText() + ": " + e.getMessage(), e);
        }
    }
}
