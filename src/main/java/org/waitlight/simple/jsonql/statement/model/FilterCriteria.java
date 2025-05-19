package org.waitlight.simple.jsonql.statement.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FilterCriteria {
    private String rel;
    private List<FilterCondition> conditions;
}