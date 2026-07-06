package com.theatomicity.aop.ut.generator.sample.service;

import org.springframework.stereotype.Service;

@Service
public class OtherService {

    public String sayHello(final String name) {
        return "Hello %s !".formatted(name);
    }

    public String sayInteger(final Integer integer) {
        return "Hey %d !".formatted(integer);
    }

    public String sayLong(final Long longValue) {
        return "Hey %d !".formatted(longValue);
    }

}
