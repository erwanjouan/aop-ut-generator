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

    public SampleService(final SampleRepository sampleRepository) {
        this.sampleRepository = sampleRepository;
    }

    public void doSomething() {
        this.sampleRepository.doSomething();
    }

    public Long doSomethingElse() throws Exception {
        final Long l = this.sampleRepository.returnSomeThing();
        if (l == 2L) {
            this.sampleRepository.throwsSomeException();
        }
        return l;
    }

    public List<BigDecimal> doSomethingWithList() throws Exception {
        final List<BigDecimal> list = this.sampleRepository.returnSomeThingWithList();
        if (list.isEmpty()) {
            this.sampleRepository.throwsSomeException();
        }
        return list;
    }

}
