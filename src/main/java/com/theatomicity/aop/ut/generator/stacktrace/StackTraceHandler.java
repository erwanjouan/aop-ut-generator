package com.theatomicity.aop.ut.generator.stacktrace;

import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class StackTraceHandler {

    private static final Logger log = LoggerFactory.getLogger(StackTraceHandler.class);

    @Value("${ut-generator.packages.base}")
    private String basePackage;

    public Optional<StackTraceElement> findCaller(final MethodInvocation invocation, final StackTraceElement[] stackTrace) {
        final String target = invocation.getThis().toString();
        if (target.startsWith("org.springframework.data.jpa.repository.support.SimpleJpaRepository")) {
            return this.walkStackJpaRepository(invocation, stackTrace);
        } else {
            return this.walkStackNonRepo(invocation, stackTrace);
        }
    }

    private Optional<StackTraceElement> walkStackJpaRepository(final MethodInvocation invocation, final StackTraceElement[] stackTrace) {
        final String proxyName = invocation.getThis().getClass().getName();
        for (int i = 0; i < stackTrace.length - 1; i++) {
            final StackTraceElement frame = stackTrace[i];
            if (frame.getClassName().equals(proxyName)) {
                final StackTraceElement caller = stackTrace[i + 1];
                log.info("Found caller of \n{} in stack trace \n[{}]", invocation.getThis(), caller);
                return Optional.of(caller);
            }
        }
        return Optional.empty();
    }

    private Optional<StackTraceElement> walkStackNonRepo(final MethodInvocation invocation, final StackTraceElement[] stackTrace) {
        for (int i = 0; i < stackTrace.length - 1; i++) {
            final StackTraceElement frame = stackTrace[i];
            if (this.isCglibProxy(frame) && frame.getClassName().startsWith(this.basePackage)) {
                final StackTraceElement caller = stackTrace[i + 1];
                if (caller.getClassName().startsWith(this.basePackage) && !this.isCglibProxy(caller)) {
                    log.info("Found caller of \n{} in stack trace \n[{}]", invocation.getThis(), caller);
                    return Optional.of(caller);
                }
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private boolean isCglibProxy(final StackTraceElement frame) {
        return frame.getClassName().contains("$$");
    }

    public void logFiltered(final StackTraceElement[] stackTrace) {
        final List<StackTraceElement> relevant = new ArrayList<>(Arrays.stream(stackTrace)
                .filter(e -> e.getClassName().startsWith(this.basePackage))
                .toList());
        Collections.reverse(relevant);
        for (int i = 0; i < relevant.size(); i++) {
            final StackTraceElement e = relevant.get(i);
            final String indent = "\t".repeat(i);
            log.debug("{}-> {}.{}({}:{})",
                    indent, e.getClassName(), e.getMethodName(), e.getFileName(), e.getLineNumber());
        }
    }
}
