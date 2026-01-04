package com.theatomicity.aop.ut.generator.interceptor;

import com.theatomicity.aop.ut.generator.core.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class UtGenerateInterceptor {

    private final EntryPointAroundPointcut entryPointAroundPointcut;

    @Pointcut("execution(* com.theatomicity.aop.ut.generator.sample..*(..))")
    public void allMethodsInRootPackage() {
    }

    @Around("allMethodsInRootPackage()")
    public Object logAroundServiceMethods(final ProceedingJoinPoint joinPoint) throws Throwable {
        return entryPointAroundPointcut.logAroundServiceMethods(joinPoint);
    }
}
