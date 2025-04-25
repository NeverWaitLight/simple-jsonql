package org.waitlight.simple.jsonql;

import jakarta.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Property {
    private String name;
    private String column;
    private String type;
    private String relationshipType; // OneToMany, ManyToOne, etc
    private Class<?> targetEntity; // For relationships
    private String mappedBy; // For bidirectional relationships
    private String foreignKeyName; // For foreign key constraints
    private boolean nullable; // Whether column is nullable
    private String joinTableName; // For ManyToMany join table
    private JoinColumn[] joinColumns; // For ManyToMany join columns
    private JoinColumn[] inverseJoinColumns; // For ManyToMany inverse join columns

    public Property(String name, String column, String type) {
        this.name = name;
        this.column = column;
        this.type = type;
    }

    public void setJoinTableName(String joinTableName) {
        this.joinTableName = joinTableName;
    }

    public void setJoinColumns(JoinColumn[] joinColumns) {
        this.joinColumns = joinColumns;
    }

    public void setInverseJoinColumns(JoinColumn[] inverseJoinColumns) {
        this.inverseJoinColumns = inverseJoinColumns;
    }
}
