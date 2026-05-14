package com.theatomicity.aop.ut.generator.sample.service;

import com.theatomicity.aop.ut.generator.sample.repository.SampleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SampleService {

    private final SampleRepository sampleRepository;

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
