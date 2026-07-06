package com.theatomicity.aop.ut.generator.cache;

import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.theatomicity.aop.ut.generator.model.InterceptedParam;
import com.theatomicity.aop.ut.generator.model.MethodExecution;
import com.theatomicity.aop.ut.generator.utils.GeneratorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


@Component
public class MethodExecutionCache {

    private static final Logger log = LoggerFactory.getLogger(MethodExecutionCache.class);

    private final GeneratorUtils generatorUtils;

    private final List<MethodExecution> cache = new ArrayList<>();

    public MethodExecutionCache(final GeneratorUtils generatorUtils) {
        this.generatorUtils = generatorUtils;
    }

    public List<MethodExecution> getCache() {
        return this.cache;
    }

    public boolean add(final MethodExecution methodExecution) {
        final String methodName = methodExecution.getMethodName();
        final String className = methodExecution.getClassName();
        final int hashCode = methodExecution.getHashCode();
        final boolean alreadyInCache = this.cache.stream().filter(Objects::nonNull)
                .filter(method -> method.getMethodName().equals(methodName))
                .filter(method -> method.getClassName().equals(className))
                .filter(method -> method.getHashCode() == hashCode)
                .findAny()
                .isPresent();
        if (alreadyInCache) {
            return false;
        }
        log.debug("Adding method execution to cache: {}", methodExecution);
        this.cache.add(methodExecution);
        return true;
    }

    private boolean matchesArguments(final MethodExecution methodExecution, final MethodExecution cachedMethodExecution) {
        final List<InterceptedParam> methodExecutionInputParams = methodExecution.getInputParams();
        final List<InterceptedParam> cachedMethodExecutionInputParams = cachedMethodExecution.getInputParams();
        if (methodExecutionInputParams.size() == cachedMethodExecutionInputParams.size()) {
            for (int i = 0; i < methodExecutionInputParams.size(); i++) {
                final InterceptedParam interceptedParam = methodExecutionInputParams.get(i);
                final InterceptedParam interceptedParam1 = cachedMethodExecutionInputParams.get(i);
                if (!interceptedParam.getType().equals(interceptedParam1.getType())) {
                    return false;
                }
                if (Objects.isNull(interceptedParam.getValue()) && Objects.nonNull(interceptedParam1.getValue())) {
                    return false;
                }
                if (Objects.nonNull(interceptedParam.getValue()) && Objects.isNull(interceptedParam1.getValue())) {
                    return false;
                }
                if (Objects.nonNull(interceptedParam.getValue()) && Objects.nonNull(interceptedParam1.getValue())) {
                    if (!interceptedParam.getValue().equals(interceptedParam1.getValue())) {
                        return false;
                    }
                }
            }
        }
        return false;
    }

    public Object findInputParamValue(final CompilationUnit originCu,
                                      final MethodDeclaration originMethod,
                                      final Parameter parameter) {
        final String methodNameAsString = originMethod.getNameAsString();
        final String originFullClassName = this.generatorUtils.getOriginFullClassName(originCu);
        final String parameterNameAsString = parameter.getNameAsString();
        final String parameterTypeAsString = parameter.getType().toString();
        return this.cache.stream()
                .filter(entry -> entry.getMethodName().equals(methodNameAsString))
                .filter(entry -> entry.getClassName().equals(originFullClassName))
                .map(MethodExecution::getInputParams)
                .flatMap(Collection::stream)
                .filter(objectParam -> objectParam.getName().equals(parameterNameAsString))
                .filter(objectParam -> objectParam.getType().getSimpleName().equals(parameterTypeAsString))
                .findFirst()
                .map(InterceptedParam::getValue)
                .orElse(null);
    }


    public Optional<MethodExecution> findOriginMethodInCache(final CompilationUnit originCu,
                                                             final MethodDeclaration originMethod) {
        final String methodNameAsString = originMethod.getNameAsString();
        final String fullClassName = this.generatorUtils.getOriginFullClassName(originCu);
        return this.cache.stream()
                .filter(entry -> entry.getMethodName().equals(methodNameAsString))
                .filter(entry -> entry.getClassName().equals(fullClassName))
                .findFirst();
    }

    public List<MethodExecution> findChildExecutions(final MethodExecution methodExecution,
                                                     final MethodDeclaration originMethod) {
        log.debug("findChildExecutions for {}", methodExecution);
        for (final MethodExecution cachedExecution : this.cache) {
            log.debug("cache content {}", cachedExecution);
        }
        log.debug("cache content {}", methodExecution);
        final List<MethodExecution> childExecutions = this.cache.stream()
                .filter(cacheEntry -> this.isNotItSelf(cacheEntry, methodExecution))
                .filter(cacheEntry -> this.matchesTime(cacheEntry, methodExecution))
                .filter(cacheEntry -> this.isCalledBy(cacheEntry, methodExecution, originMethod))
                .toList();
        log.debug("Number of childExecution : {}", childExecutions.size());
        for (final MethodExecution childExecution : childExecutions) {
            log.debug("Number of childExecution : {}", childExecution);
        }
        return childExecutions;
    }

    private boolean isNotItSelf(final MethodExecution cacheEntry, final MethodExecution methodExecution) {
        final boolean sameMethod = cacheEntry.getMethodName().equals(methodExecution.getMethodName());
        final boolean sameClass = cacheEntry.getClassName().equals(methodExecution.getClassName());
        final boolean isItSelf = sameMethod && sameClass;
        return !isItSelf;
    }

    private boolean matchesTime(final MethodExecution cacheEntry, final MethodExecution methodExecution) {
        final boolean startsAfter = cacheEntry.getStartTime() >= methodExecution.getStartTime();
        final boolean endsBefore = cacheEntry.getEndTime() <= methodExecution.getEndTime();
        return startsAfter && endsBefore;
    }

    private boolean isCalledBy(final MethodExecution cacheEntry,
                               final MethodExecution targetMethodExecution,
                               final MethodDeclaration originMethod) {
        final String targetMethodExecutionName = targetMethodExecution.getMethodName();
        final String methodExecutionClassName = targetMethodExecution.getClassName();
        return cacheEntry.getCaller()
                .map(caller -> {
                    final String callerClassName = caller.getClassName();
                    final boolean matchesClassName = callerClassName.equals(methodExecutionClassName);
                    final String methodName = caller.getMethodName();
                    final boolean matchesMethodName = methodName.equals(targetMethodExecutionName);
                    final Optional<Range> range = originMethod.getRange();
                    final Boolean matechesPosition = range
                            .map(r -> r.contains(new Position(caller.getLineNumber(), 0)))
                            .orElse(false);
                    return matchesClassName && matchesMethodName && matechesPosition;
                }).orElse(false);
    }
}
