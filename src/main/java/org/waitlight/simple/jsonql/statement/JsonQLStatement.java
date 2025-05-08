package org.waitlight.simple.jsonql.statement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.waitlight.simple.jsonql.statement.model.StatementType;

@Data
public abstract class JsonQLStatement {

    // 应用程序唯一标识符
    @JsonProperty("appId")
    private String appId;

    // 表单模板唯一标识符
    @JsonProperty("formId")
    private String formId;

    // 业务实体唯一标识符
    @JsonProperty("entityId")
    private String entityId;

    @JsonIgnore // This is set separately, not from JSON
    private StatementType statement;
}