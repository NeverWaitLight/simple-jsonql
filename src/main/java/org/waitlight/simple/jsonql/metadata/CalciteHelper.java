package org.waitlight.simple.jsonql.metadata;

import lombok.Getter;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.schema.SchemaPlus;

@Getter
public class CalciteHelper {
    private final SchemaPlus schema = CalciteSchema.createRootSchema(true, true).plus();
}
