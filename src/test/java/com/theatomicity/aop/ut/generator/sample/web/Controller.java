package com.theatomicity.aop.ut.generator.sample.web;

import com.theatomicity.aop.ut.generator.sample.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

    @Autowired
    private SampleService sampleService;

    @GetMapping
    public Long doSomethingFromRest() throws Exception {
        this.sampleService.doSomething();
        final Long l = this.sampleService.doSomethingElse();
        this.sampleService.doSomethingWithList("3");
        return l;
    }
}
