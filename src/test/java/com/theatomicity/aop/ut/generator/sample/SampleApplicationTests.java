package com.theatomicity.aop.ut.generator.sample;

import com.theatomicity.aop.ut.generator.sample.web.Controller;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Manual test")
class SampleApplicationTests {

    @Autowired
    private Controller controller;

    @Test
    void contextLoads() throws Exception {
        this.controller.doSomethingFromRest();
    }

}
