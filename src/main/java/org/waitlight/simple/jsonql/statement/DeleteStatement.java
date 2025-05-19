package org.waitlight.simple.jsonql.statement;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class DeleteStatement extends JsonQLStatement {
    private List<String> ids;
}