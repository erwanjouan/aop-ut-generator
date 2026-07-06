package com.theatomicity.aop.ut.generator.sample.service;

import com.theatomicity.aop.ut.generator.sample.repository.SampleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SampleService {

    private static final Logger log = LoggerFactory.getLogger(SampleService.class);

    private final SampleRepository sampleRepository;

    private final OtherService otherService;

    public SampleService(final SampleRepository sampleRepository, final OtherService otherService) {
        this.sampleRepository = sampleRepository;
        this.otherService = otherService;
    }

    public void doSomething() {
        this.sampleRepository.doSomething();
    }

    public Long doSomethingElse() throws Exception {
        this.otherService.sayLong(1L);
        return this.sampleRepository.returnSomeThing();
    }

    public List<BigDecimal> doSomethingWithList(final String input) {
        return this.sampleRepository.returnSomeThingWithList(input);
    }

}
