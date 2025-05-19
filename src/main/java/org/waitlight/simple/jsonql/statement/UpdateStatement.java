package org.waitlight.simple.jsonql.statement;

import lombok.Getter;
import lombok.Setter;
import org.waitlight.simple.jsonql.statement.model.FieldStatement;

import java.util.List;

@Setter
@Getter
public class UpdateStatement extends JsonQLStatement {
    private String dataId;
    private List<FieldStatement> fields;
} 