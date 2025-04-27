package org.waitlight.simple.jsonql.statement.model;

public abstract class JsonqlStatement {
    private StatementType statement;

    public StatementType getStatement() {
        return statement;
    }

    public void setStatement(StatementType statement) {
        this.statement = statement;
    }
} 