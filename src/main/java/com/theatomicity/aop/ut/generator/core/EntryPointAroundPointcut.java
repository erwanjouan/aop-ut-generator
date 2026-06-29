package com.theatomicity.aop.ut.generator.core;

import com.theatomicity.aop.ut.generator.cache.MethodExecutionCache;
import com.theatomicity.aop.ut.generator.model.InterceptedParam;
import com.theatomicity.aop.ut.generator.model.MethodExecution;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Aspect
@Component
public class EntryPointAroundPointcut {

    private static final Logger log = LoggerFactory.getLogger(EntryPointAroundPointcut.class);

    private final TestClassGenerator testClassGenerator;

    private final MethodExecutionCache cache;

    public EntryPointAroundPointcut(final TestClassGenerator testClassGenerator, final MethodExecutionCache cache) {
        this.testClassGenerator = testClassGenerator;
        this.cache = cache;
    }

    @Pointcut("execution(* org.springframework.data.repository.CrudRepository+.*(..))")
    public void crudRepositoryPointcut() {
    }

    @Around("crudRepositoryPointcut()")
    public Object aroundCrudRepository(final ProceedingJoinPoint joinPoint) throws Throwable {
        return this.proceed(joinPoint);
    }

    public @Nullable Object intercept(final MethodInvocation invocation) throws Throwable {
        final MethodExecution methodExecution = MethodExecution.from(invocation);
        final Object result;
        try {
            result = invocation.proceed();
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
