package com.theatomicity.aop.ut.generator.core;

import com.theatomicity.aop.ut.generator.cache.MethodExecutionCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class EntryPointAroundPointcut {

    private final TestClassGenerator testClassGenerator;

    private final MethodExecutionCache cache;

    public Object logAroundServiceMethods(final ProceedingJoinPoint joinPoint) throws Throwable {
        final MethodExecution methodExecution = MethodExecution.from(joinPoint);
        log.info("Before MethodExecution: {} {} {}", methodExecution.getClassName(), methodExecution.getName(), methodExecution.getInputParams());
        final Object result;
        try {
            result = joinPoint.proceed();
            if (Objects.nonNull(result)) {
                methodExecution.setResult(new InterceptedParam("result", result.getClass(), result));
            }
        } catch (final Throwable e) {
            log.error("Error during execution of method: {} {}", methodExecution.getClassName(), methodExecution.getName(), e);
            throw e;
        } finally {
            log.info("End MethodExecution: {} {} {}", methodExecution.getClassName(), methodExecution.getName(), methodExecution.getResult());
            methodExecution.setEndTime(System.nanoTime());
            final boolean isNewMethodExecution = this.cache.add(methodExecution);
            final boolean isRepository = methodExecution.getSimpleClassName().contains("Repository");
            if (isNewMethodExecution && !isRepository) {
                this.testClassGenerator.generateUnitTest(methodExecution);
            }
        }
        return result;
    }
}
