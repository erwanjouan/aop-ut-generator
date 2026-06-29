package com.theatomicity.aop.ut.generator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "ut-generator")
public class UtGeneratorProperties {

    private List<String> packages = new ArrayList<>();

    public List<String> getPackages() { return packages; }
    public void setPackages(final List<String> packages) { this.packages = packages; }
}
