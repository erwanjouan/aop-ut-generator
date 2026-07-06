package com.theatomicity.aop.ut.generator.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;


public class UtGeneratorCondition implements Condition {

    @Override
    public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
        final String firstPackage = context.getEnvironment().getProperty("ut-generator.packages.targets[0]");
        return firstPackage != null && !firstPackage.isBlank();
    }
}
