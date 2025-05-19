package org.waitlight.simple.jsonql.statement;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class JsonQLStatement {
    private String appId;
    private String formId;
    private String entityId;
}