package com.theatomicity.aop.ut.generator.sample.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
public class SampleRepository {

    private static final Logger log = LoggerFactory.getLogger(SampleRepository.class);

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
