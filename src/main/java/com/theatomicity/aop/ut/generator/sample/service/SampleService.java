package com.theatomicity.aop.ut.generator.sample.service;

import com.theatomicity.aop.ut.generator.sample.repository.SampleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SampleService {

    private final SampleRepository sampleRepository;

    public void doSomething() {
        sampleRepository.doSomething();
    }

}
