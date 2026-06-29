package com.theatomicity.aop.ut.generator.config;

import com.theatomicity.aop.ut.generator.cache.MethodExecutionCache;
import com.theatomicity.aop.ut.generator.core.EntryPointAroundPointcut;
import com.theatomicity.aop.ut.generator.utils.GeneratorUtils;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Conditional;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.util.stream.Collectors;

@AutoConfiguration
@ComponentScan(value = {"com.theatomicity.aop.ut.generator.core"})
@EnableConfigurationProperties(UtGeneratorProperties.class)
public class AopUtGeneratorConfig {

    @Bean
    public GeneratorUtils generatorUtils() {
        return new GeneratorUtils();
    }

    @Bean
    public MethodExecutionCache methodExecutionCache(final GeneratorUtils generatorUtils) {
        return new MethodExecutionCache(generatorUtils);
    }

    @Bean
    @Conditional(UtGeneratorCondition.class)
    public DefaultPointcutAdvisor dynamicPackageAdvisor(
            final UtGeneratorProperties properties,
            final EntryPointAroundPointcut entryPointAroundPointcut) {
        final String expression = properties.getPackages().stream()
                .map(pkg -> "execution(* " + pkg + "..*.*(..))")
                .collect(Collectors.joining(" || "));
        final AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression(expression);
        return new DefaultPointcutAdvisor(pointcut, (MethodInterceptor) entryPointAroundPointcut::intercept);
    }
}
