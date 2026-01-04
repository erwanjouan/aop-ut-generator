package com.theatomicity.aop.ut.generator;

import com.theatomicity.aop.ut.generator.sample.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SampleApplication implements CommandLineRunner {

    public static void main(final String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }

    @Autowired
    private SampleService sampleService;

    @Override
    public void run(String... args) throws Exception {
        sampleService.doSomething();
    }
}
