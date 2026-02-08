package com.theatomicity.aop.ut.generator.sample.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SampleRepository {

    public void doSomething() {
        log.info("Do Something");
    }

    public Long returnSomeThing() {
        return 1L;
    }

    public void throwsSomeException() throws Exception {
        throw new Exception("Some Exception");
    }

}
