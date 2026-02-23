package com.theatomicity.aop.ut.generator.core;

import com.theatomicity.aop.ut.generator.cache.MethodExecutionCache;
import com.theatomicity.aop.ut.generator.model.InterceptedParam;
import com.theatomicity.aop.ut.generator.model.MethodExecution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class EntryPointAroundPointcut {

    private final TestClassGenerator testClassGenerator;

    private final MethodExecutionCache cache;

    @Around("@annotation(com.theatomicity.aop.ut.generator.annotation.GenerateMockitoUt)")
    public Object logAroundServiceMethods(final ProceedingJoinPoint joinPoint) throws Throwable {
        return this.proceed(joinPoint);
    }

    @Pointcut("execution(* org.springframework.data.repository.CrudRepository+.*(..))")
    public void crudRepositoryPointcut() {
    }

    @Around("crudRepositoryPointcut()")
    public Object aroundCrudRepository(final ProceedingJoinPoint joinPoint) throws Throwable {
        return this.proceed(joinPoint);
    }

    private @Nullable Object proceed(final ProceedingJoinPoint joinPoint) throws Throwable {
        final MethodExecution methodExecution = MethodExecution.from(joinPoint);
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
            methodExecution.setEndTime(System.currentTimeMillis());
            methodExecution.log();
            final boolean isNewMethodExecution = this.cache.add(methodExecution);
            final boolean isRepository = methodExecution.getSimpleClassName().contains("Repository");
            if (isNewMethodExecution && !isRepository) {
                this.testClassGenerator.generateUnitTest(methodExecution);
            }
        }
        return result;
    }
}
