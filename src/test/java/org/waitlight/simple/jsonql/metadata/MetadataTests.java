package org.waitlight.simple.jsonql.metadata;

import org.junit.Test;
import org.waitlight.simple.jsonql.entity.Blog;
import org.waitlight.simple.jsonql.entity.User;

public class MetadataTests {

    @Test
    public void test() {
        // 1. 创建MetadataSources并加载Entity类
        MetadataSources metadataSources = new MetadataSources();
        metadataSources.addAnnotatedClass(User.class);
        metadataSources.addAnnotatedClass(Blog.class);

        // 2. 构建元数据
        Metadata metadata = metadataSources.buildMetadata();

        // 3. 验证Users元数据
        PersistentClass userMetadata = metadata.getEntityBinding(User.class.getSimpleName());
        System.out.println("User Entity: " + userMetadata.getEntityName());
        System.out.println("User Table: " + userMetadata.getTableName());
        System.out.println("User Properties:");
        userMetadata.getProperties().forEach(prop -> {
            System.out.println(" - " + prop.getName() +
                    (prop.getColumn() != null ? " (column: " + prop.getColumn() + ")" : "") +
                    (prop.getRelationshipType() != null ? " (relationship: " + prop.getRelationshipType() + ")" : "") +
                    (prop.getForeignKeyName() != null ? " (fk: " + prop.getForeignKeyName() + ")" : "") +
                    (prop.getJoinTableName() != null ? " (joinTable: " + prop.getJoinTableName() + ")" : "") +
                    " (nullable: " + prop.isNullable() + ")");
        });

        // 4. 验证Blogs元数据
        PersistentClass blogMetadata = metadata.getEntityBinding(Blog.class.getSimpleName());
        System.out.println("\nBlog Entity: " + blogMetadata.getEntityName());
        System.out.println("Blog Table: " + blogMetadata.getTableName());
        System.out.println("Blog Properties:");
        blogMetadata.getProperties().forEach(prop -> {
            System.out.println(" - " + prop.getName() +
                    (prop.getColumn() != null ? " (column: " + prop.getColumn() + ")" : "") +
                    (prop.getRelationshipType() != null ? " (relationship: " + prop.getRelationshipType() + ")" : ""));
        });
    }
}
