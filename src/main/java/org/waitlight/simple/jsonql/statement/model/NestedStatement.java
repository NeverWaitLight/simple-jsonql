package org.waitlight.simple.jsonql.statement.model;

import lombok.Getter;
import lombok.Setter;
import org.waitlight.simple.jsonql.statement.JsonQLStatement;

import java.util.List;

@Getter
@Setter
public class NestedStatement extends JsonQLStatement {
    private String dataId;
    private List<FieldStatement> fields;
}