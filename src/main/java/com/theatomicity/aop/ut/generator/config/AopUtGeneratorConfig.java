package com.theatomicity.aop.ut.generator.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan(value = {"com.theatomicity.aop.ut.generator.core"})
public class AopUtGeneratorConfig {

}
