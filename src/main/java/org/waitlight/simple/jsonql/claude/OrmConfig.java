package org.waitlight.simple.jsonql.claude;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ORM配置类
 */
@Configuration
public class OrmConfig {

    /**
     * 配置实体映射
     */
    @Bean
    public void configureEntityMappings(CustomEntityManager entityManager, JsonqlParser jsonqlParser) {
        // 注册实体类
        jsonqlParser.registerEntity("User", User.class);
        jsonqlParser.registerEntity("Post", Post.class);
        jsonqlParser.registerEntity("Category", Category.class);
        jsonqlParser.registerEntity("Product", Product.class);

        // 配置User实体映射
        entityManager.registerEntity(User.class, "users", "id")
                .mapColumn(User.class, "id", "id", "BIGINT")
                .mapColumn(User.class, "username", "username", "VARCHAR")
                .mapColumn(User.class, "email", "email", "VARCHAR")
                .mapColumn(User.class, "createdAt", "created_at", "TIMESTAMP")
                .mapOneToMany(User.class, "posts", Post.class, "userId");

        // 配置Post实体映射
        entityManager.registerEntity(Post.class, "posts", "id")
                .mapColumn(Post.class, "id", "id", "BIGINT")
                .mapColumn(Post.class, "title", "title", "VARCHAR")
                .mapColumn(Post.class, "content", "content", "TEXT")
                .mapColumn(Post.class, "published", "published", "BOOLEAN")
                .mapColumn(Post.class, "createdAt", "created_at", "TIMESTAMP")
                .mapManyToOne(Post.class, "user", User.class, "userId");

        // 配置Category实体映射
        entityManager.registerEntity(Category.class, "categories", "id")
                .mapColumn(Category.class, "id", "id", "BIGINT")
                .mapColumn(Category.class, "name", "name", "VARCHAR")
                .mapOneToMany(Category.class, "products", Product.class, "categoryId");

        // 配置Product实体映射
    }
}