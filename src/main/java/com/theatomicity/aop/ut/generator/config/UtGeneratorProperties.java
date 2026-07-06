package com.theatomicity.aop.ut.generator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "ut-generator.packages")
public class UtGeneratorProperties {

    private String base;

    private List<String> targets = new ArrayList<>();

    public List<String> getTargets() {
        return this.targets;
    }

    public void setTargets(final List<String> targets) {
        this.targets = targets;
    }

    public String getBase() {
        return this.base;
    }

    public void setBase(final String base) {
        this.base = base;
    }
}
