package org.waitlight.simple.jsonql.statement.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class InsertStatement extends JsonqlStatement {
    private String into;
    private Map<String, Object> values;
}