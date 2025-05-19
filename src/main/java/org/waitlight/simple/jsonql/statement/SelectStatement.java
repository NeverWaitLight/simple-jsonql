package org.waitlight.simple.jsonql.statement;

import lombok.Getter;
import lombok.Setter;
import org.waitlight.simple.jsonql.statement.model.FilterCriteria;
import org.waitlight.simple.jsonql.statement.model.PageCriteria;
import org.waitlight.simple.jsonql.statement.model.SortCriteria;

import java.util.List;

@Getter
@Setter
public class SelectStatement extends JsonQLStatement {
    private FilterCriteria filters;
    private List<SortCriteria> sort;
    private PageCriteria page;
}