package com.theatomicity.aop.ut.generator.sample.repository;

import com.theatomicity.aop.ut.generator.sample.model.Pojo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface SampleRepository extends JpaRepository<Pojo, Long> {

    Logger log = LoggerFactory.getLogger(SampleRepository.class);

    default void doSomething() {
        log.debug("Do Something from repository : {}", this);
    }

    default Long returnSomeThing() {
        return 1L;
    }

    default void throwsSomeException() throws Exception {
        throw new Exception("Some Exception");
    }

    default List<BigDecimal> returnSomeThingWithList(final String input) {
        log.debug("returnSomeThingWithList : {}", input);
        return List.of(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3));
    }
}
