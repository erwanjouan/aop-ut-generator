package com.theatomicity.aop.ut.generator.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Arrays;


public class UtGeneratorCondition implements Condition {

    public static final String UT_GENERATOR_PROFILE = "ut-generator";

    @Override
    public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
        final String firstPackage = context.getEnvironment().getProperty("ut-generator.packages[0]");
        if (firstPackage == null || firstPackage.isBlank()) {
            return false;
        }
        return Arrays.stream(context.getEnvironment().getActiveProfiles())
                .anyMatch(p -> p.equals(UT_GENERATOR_PROFILE));
    }
}
