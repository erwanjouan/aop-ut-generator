package com.theatomicity.aop.ut.generator.sample.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class SampleRepository {

    public void doSomething() {
        log.debug("Do Something");
    }

    public Long returnSomeThing() {
        return 1L;
    }

    public void throwsSomeException() throws Exception {
        throw new Exception("Some Exception");
    }

    public List<BigDecimal> returnSomeThingWithList() {
        return Arrays.asList(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3));
    }
}
