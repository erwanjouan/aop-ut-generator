package com.theatomicity.aop.ut.generator.sample.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Pojo {

    @Id
    private Long id;

    private String name;

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
