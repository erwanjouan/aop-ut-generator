package com.theatomicity.aop.ut.generator.config;

import com.theatomicity.aop.ut.generator.cache.MethodExecutionCache;
import com.theatomicity.aop.ut.generator.core.GeneratorUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan(value = {"com.theatomicity.aop.ut.generator.core"})
public class AopUtGeneratorConfig {

    @Bean
    public MethodExecutionCache methodExecutionCache(final GeneratorUtils generatorUtils) {
        return new MethodExecutionCache(generatorUtils);
    }
}
