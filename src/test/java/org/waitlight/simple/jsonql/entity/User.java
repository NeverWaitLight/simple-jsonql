package org.waitlight.simple.jsonql.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class User {
    @Id
    private Long id;
    @Column
    private String name;
    @OneToMany(mappedBy = "Blog", targetEntity = org.waitlight.simple.jsonql.entity.Blog.class)
    private List<Blog> blogs;
}
