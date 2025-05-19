package org.waitlight.simple.jsonql.statement;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public class StatementParser {
    private final ObjectMapper objectMapper;

    public StatementParser() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 将JSON字符串解析为指定类型的JsonQLStatement对象
     *
     * @param jsonQL              JSON字符串
     * @param jsonQLStatementType 目标JsonQLStatement类型
     * @return 指定类型的JsonQLStatement对象
     * @throws JsonQLStatementException 解析异常
     */
    public <T extends JsonQLStatement> T parse(String jsonQL, Class<T> jsonQLStatementType) throws JsonQLStatementException {
        if (StringUtils.isBlank(jsonQL)) {
            throw new JsonQLStatementException("JsonQL cannot be empty");
        }
        if (Objects.isNull(jsonQLStatementType)) {
            throw new JsonQLStatementException("JsonQLStatement type cannot be null");
        }

        T statement;
        try {
            statement = objectMapper.readValue(jsonQL, jsonQLStatementType);
        } catch (Exception e) {
            throw new JsonQLStatementException(
                    "Failed to parse JsonQL to " + jsonQLStatementType.getSimpleName() + ": " + e.getMessage(), e);
        }

        return statement;
    }

} 