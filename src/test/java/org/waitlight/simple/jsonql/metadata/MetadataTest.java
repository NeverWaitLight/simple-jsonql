package org.waitlight.simple.jsonql.metadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.waitlight.simple.jsonql.entity.Blog;
import org.waitlight.simple.jsonql.entity.User;

import static org.junit.jupiter.api.Assertions.*;

public class MetadataTest {
    private Metadata metadata;

    @BeforeEach
    void setUp() {
        MetadataSource metadataSource = new MetadataSource();
        metadataSource.addAnnotatedClass(User.class);
        metadataSource.addAnnotatedClass(Blog.class);

        metadata = MetadataBuilderFactory.createLocalBuilder(metadataSource).build();
    }

    @Test
    void testUserMetadata() {
        // 验证Users元数据
        PersistentClass userMetadata = metadata.getEntity(User.class.getSimpleName().toLowerCase());
        assertNotNull(userMetadata, "User metadata should not be null");
        assertEquals("User", userMetadata.getEntityName(), "Entity name should be 'User'");
        assertEquals("user", userMetadata.getTableName(), "Table name should be 'user'");

        // 验证属性
        assertFalse(userMetadata.getProperties().isEmpty(), "User should have properties");

        // 验证基本属性
        userMetadata.getProperties().stream()
                .filter(prop -> prop.getRelationship() == null)
                .forEach(prop -> {
                    assertNotNull(prop.getFieldName(), "Property name should not be null");
                    assertNotNull(prop.getColumnName(), "Column name should not be null");
                    assertNotNull(prop.getType(), "Type should not be null");
                });

        // 验证关联属性
        userMetadata.getProperties().stream()
                .filter(prop -> prop.getRelationship() != null)
                .forEach(prop -> {
                    assertNotNull(prop.getRelationship(), "Relationship type should not be null");
                    assertNotNull(prop.getTargetEntity(), "Target entity should not be null");

                    if (prop.getRelationship() == RelationshipType.ONE_TO_MANY) {
                        assertNotNull(prop.getMappedBy(), "OneToMany should have mappedBy");
                    }

                    if (prop.getRelationship() == RelationshipType.MANY_TO_ONE) {
                        assertNotNull(prop.getForeignKeyName(), "ManyToOne should have foreignKeyName");
                    }

                    if (prop.getRelationship() == RelationshipType.MANY_TO_MANY) {
                        assertNotNull(prop.getJoinTableName(), "ManyToMany should have joinTableName");
                    }
                });
    }

    @Test
    void testBlogMetadata() {
        // 验证Blogs元数据
        PersistentClass blogMetadata = metadata.getEntity(Blog.class.getSimpleName().toLowerCase());
        assertNotNull(blogMetadata, "Blog metadata should not be null");
        assertEquals("Blog", blogMetadata.getEntityName(), "Entity name should be 'Blog'");
        assertEquals("blog", blogMetadata.getTableName(), "Table name should be 'blog'");

        // 验证属性
        assertFalse(blogMetadata.getProperties().isEmpty(), "Blog should have properties");

        // 验证基本属性
        blogMetadata.getProperties().stream()
                .filter(prop -> prop.getRelationship() == null)
                .forEach(prop -> {
                    assertNotNull(prop.getFieldName(), "Property name should not be null");
                    assertNotNull(prop.getColumnName(), "Column name should not be null");
                    assertNotNull(prop.getType(), "Type should not be null");
                });

        // 验证关联属性
        blogMetadata.getProperties().stream()
                .filter(prop -> prop.getRelationship() != null)
                .forEach(prop -> {
                    assertNotNull(prop.getRelationship(), "Relationship type should not be null");
                    assertNotNull(prop.getTargetEntity(), "Target entity should not be null");

                    if (prop.getRelationship() == RelationshipType.MANY_TO_ONE) {
                        assertNotNull(prop.getForeignKeyName(), "ManyToOne should have foreignKeyName");
                    }
                });
    }
}
