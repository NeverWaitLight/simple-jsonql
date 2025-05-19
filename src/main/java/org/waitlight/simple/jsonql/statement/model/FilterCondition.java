package org.waitlight.simple.jsonql.statement.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FilterCondition {
    private String field;
    private MethodType method;
    private Object value;
    private List<Object> values;
}