package org.waitlight.simple.jsonql.builder;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.waitlight.simple.jsonql.entity.User;
import org.waitlight.simple.jsonql.metadata.MetadataBuilderFactory;
import org.waitlight.simple.jsonql.metadata.MetadataSource;
import org.waitlight.simple.jsonql.statement.SelectStatement;
import org.waitlight.simple.jsonql.statement.model.FilterCondition;
import org.waitlight.simple.jsonql.statement.model.FilterCriteria;
import org.waitlight.simple.jsonql.statement.model.MethodType;
import org.waitlight.simple.jsonql.statement.model.PageCriteria;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SelectSqlBuilderTest {

    private static SelectSqlBuilder selectSqlBuilder;

    @BeforeAll
    public static void setUp() {
        MetadataSource metadataSource = new MetadataSource();
        metadataSource.registry(User.class);
        selectSqlBuilder = new SelectSqlBuilder(MetadataBuilderFactory.createLocalBuilder(metadataSource).build());
    }

    @Test
    public void build_basicSelect_returnsValidSql() throws Exception {
        SelectStatement statement = new SelectStatement();
        statement.setEntityId("user");

        PreparedSql<SelectStatement> result = selectSqlBuilder.build(statement);

        assertNotNull(result);
        assertNotNull(result.getSql());
        assertTrue(result.getSql().contains("SELECT"));
        assertTrue(result.getSql().contains("user"));
    }

    @Test
    public void build_selectWithFilter_returnsValidSql() throws Exception {
        SelectStatement statement = new SelectStatement();
        statement.setEntityId("user");

        FilterCriteria filters = new FilterCriteria();
        filters.setRel("AND");

        FilterCondition condition = new FilterCondition();
        condition.setField("id");
        condition.setMethod(MethodType.EQ);
        condition.setValue(1L);

        filters.setConditions(List.of(condition));
        statement.setFilters(filters);

        PreparedSql<SelectStatement> result = selectSqlBuilder.build(statement);

        assertNotNull(result);
        assertNotNull(result.getSql());
        assertTrue(result.getSql().contains("SELECT"));
        assertTrue(result.getSql().contains("WHERE"));
    }

    @Test
    public void build_selectWithPagination_returnsValidSql() throws Exception {
        SelectStatement statement = new SelectStatement();
        statement.setEntityId("user");

        PageCriteria page = new PageCriteria();
        page.setSize(10);
        page.setNumber(1);
        statement.setPage(page);

        PreparedSql<SelectStatement> result = selectSqlBuilder.build(statement);

        assertNotNull(result);
        assertNotNull(result.getSql());
        assertTrue(result.getSql().contains("SELECT"));
        assertTrue(result.getSql().contains("LIMIT"));
    }
}