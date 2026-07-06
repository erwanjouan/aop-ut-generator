package com.theatomicity.aop.ut.generator.core;

import com.theatomicity.aop.ut.generator.cache.MethodExecutionCache;
import com.theatomicity.aop.ut.generator.model.InterceptedParam;
import com.theatomicity.aop.ut.generator.model.MethodExecution;
import com.theatomicity.aop.ut.generator.stacktrace.StackTraceHandler;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Aspect
@Component
public class EntryPointAroundPointcut {

    private static final Logger log = LoggerFactory.getLogger(EntryPointAroundPointcut.class);

    private final UtGenerator utGenerator;

    private final MethodExecutionCache cache;

    private final StackTraceHandler stackTraceHandler;

    public EntryPointAroundPointcut(final UtGenerator utGenerator, final MethodExecutionCache cache, final StackTraceHandler stackTraceHandler) {
        this.utGenerator = utGenerator;
        this.cache = cache;
        this.stackTraceHandler = stackTraceHandler;
    }

    /*
        @Pointcut("execution(* org.springframework.data.repository.CrudRepository+.*(..))")
        public void crudRepositoryPointcut() {
        }

        @Around("crudRepositoryPointcut()")
        public Object aroundCrudRepository(final ProceedingJoinPoint joinPoint) throws Throwable {
            return this.proceed(joinPoint);
        }
    */
    public @Nullable Object intercept(final MethodInvocation invocation) throws Throwable {
        log.debug("Intercepting method invocation: {} {}", invocation.getThis(), invocation.getMethod().getName());
        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        final Optional<StackTraceElement> caller = this.stackTraceHandler.findCaller(invocation, stackTrace);
        final MethodExecution methodExecution = MethodExecution.from(invocation, caller);
        final Object result;
        try {
            result = invocation.proceed();
            if (Objects.nonNull(result)) {
                methodExecution.setResult(new InterceptedParam("result", result.getClass(), result));
            }
        } catch (final Throwable e) {
            log.error("Error during execution of method: {} {}", methodExecution.getClassName(), methodExecution.getMethodName(), e);
            throw e;
        } finally {
            log.debug("Entered in finally : {}", invocation);
            methodExecution.setEndTime(System.currentTimeMillis());
            final boolean isNewMethodExecution = this.cache.add(methodExecution);
            final boolean isRepository = methodExecution.getSimpleClassName().contains("Repository");
            log.debug("isNewMethodExecution {} isRepository {}: {}", isNewMethodExecution, isRepository, invocation);
            if (isNewMethodExecution && !isRepository) {
                log.debug("generateUnitTest for {}", methodExecution);
                this.utGenerator.generateUnitTest(methodExecution);
            }
        }
        return result;
    }
}
