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
        metadataSource.registry(User.class);
        metadataSource.registry(Blog.class);

        metadata = MetadataBuilderFactory.createLocalBuilder(metadataSource).build();
    }

    @Test
    void getEntity_userEntityRegistered_returnsValidUserMetadata() {
        // 验证Users元数据
        PersistentClass userMetadata = metadata.getEntity(User.class.getSimpleName());
        assertNotNull(userMetadata, "User metadata should not be null");
        assertEquals("User", userMetadata.getEntityName(), "Entity name should be 'User'");
        assertEquals("user", userMetadata.getTableName(), "Table name should be 'user'");

        // 验证属性
        assertFalse(userMetadata.getProperties().isEmpty(), "User should have properties");

        // 验证基本属性
        userMetadata.getProperties().stream()
                .filter(prop -> prop.relationship() == null)
                .forEach(prop -> {
                    assertNotNull(prop.fieldName(), "Property name should not be null");
                    assertNotNull(prop.columnName(), "Column name should not be null");
                    assertNotNull(prop.fieldType(), "Type should not be null");
                });

        // 验证关联属性
        userMetadata.getProperties().stream()
                .filter(prop -> prop.relationship() != null)
                .forEach(prop -> {
                    assertNotNull(prop.relationship(), "Relationship type should not be null");
                    assertNotNull(prop.targetEntity(), "Target entity should not be null");

                    if (prop.relationship() == RelationshipType.ONE_TO_MANY) {
                        assertNotNull(prop.mappedBy(), "OneToMany should have mappedBy");
                    }

                    if (prop.relationship() == RelationshipType.MANY_TO_ONE) {
                        assertNotNull(prop.foreignKeyName(), "ManyToOne should have foreignKeyName");
                    }

                    if (prop.relationship() == RelationshipType.MANY_TO_MANY) {
                        assertNotNull(prop.joinTableName(), "ManyToMany should have joinTableName");
                    }
                });
    }

    @Test
    void getEntity_blogEntityRegistered_returnsValidBlogMetadata() {
        // 验证Blogs元数据
        PersistentClass blogMetadata = metadata.getEntity(Blog.class.getSimpleName());
        assertNotNull(blogMetadata, "Blog metadata should not be null");
        assertEquals("Blog", blogMetadata.getEntityName(), "Entity name should be 'Blog'");
        assertEquals("blog", blogMetadata.getTableName(), "Table name should be 'blog'");

        // 验证属性
        assertFalse(blogMetadata.getProperties().isEmpty(), "Blog should have properties");

        // 验证基本属性
        blogMetadata.getProperties().stream()
                .filter(prop -> prop.relationship() == null)
                .forEach(prop -> {
                    assertNotNull(prop.fieldName(), "Property name should not be null");
                    assertNotNull(prop.columnName(), "Column name should not be null");
                    assertNotNull(prop.fieldType(), "Type should not be null");
                });

        // 验证关联属性
        blogMetadata.getProperties().stream()
                .filter(prop -> prop.relationship() != null)
                .forEach(prop -> {
                    assertNotNull(prop.relationship(), "Relationship type should not be null");
                    assertNotNull(prop.targetEntity(), "Target entity should not be null");

                    if (prop.relationship() == RelationshipType.MANY_TO_ONE) {
                        assertNotNull(prop.foreignKeyName(), "ManyToOne should have foreignKeyName");
                    }
                });
    }
}
