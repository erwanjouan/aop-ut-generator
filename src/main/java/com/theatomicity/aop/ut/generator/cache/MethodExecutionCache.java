package com.theatomicity.aop.ut.generator.cache;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.theatomicity.aop.ut.generator.core.GeneratorUtils;
import com.theatomicity.aop.ut.generator.core.InterceptedParam;
import com.theatomicity.aop.ut.generator.core.MethodExecution;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.*;


@Getter
@Setter
@Component
@RequiredArgsConstructor
public class MethodExecutionCache {

    private final GeneratorUtils generatorUtils;

    private final List<MethodExecution> cache = new ArrayList<>();

    public boolean add(final MethodExecution methodExecution) {
        if (this.isNewMethodExecution(methodExecution)) {
            return this.cache.add(methodExecution);
        }
        return false;
    }

    private boolean isNewMethodExecution(final MethodExecution methodExecution) {
        return this.cache.stream()
                .filter(cachedMethodExecution -> this.matchesClass(methodExecution, cachedMethodExecution))
                .filter(cachedMethodExecution -> this.matchesMethod(methodExecution, cachedMethodExecution))
                .findAny()
                .isEmpty();

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

    private boolean matchesMethod(final MethodExecution methodExecution, final MethodExecution cachedMethodExecution) {
        final String methodExecutionName = methodExecution.getName();
        final String cachedMethodExecutionName = cachedMethodExecution.getName();
        return methodExecutionName.equals(cachedMethodExecutionName);
    }

    private boolean matchesClass(final MethodExecution methodExecution, final MethodExecution cachedMethodExecution) {
        final String className = methodExecution.getClassName();
        final String cachedClassName = cachedMethodExecution.getClassName();
        return className.equals(cachedClassName);
    }

    public Object findInputParamValue(final CompilationUnit originCompilationUnit,
                                      final MethodDeclaration originMethod,
                                      final Parameter parameter) {
        final String methodNameAsString = originMethod.getNameAsString();
        final String originFullClassName = this.generatorUtils.getOriginFullClassName(originCompilationUnit);
        final String parameterNameAsString = parameter.getNameAsString();
        final String parameterTypeAsString = parameter.getType().toString();
        return this.cache.stream()
                .filter(entry -> entry.getName().equals(methodNameAsString))
                .filter(entry -> entry.getClassName().equals(originFullClassName))
                .map(MethodExecution::getInputParams)
                .flatMap(Collection::stream)
                .filter(objectParam -> objectParam.getName().equals(parameterNameAsString))
                .filter(objectParam -> objectParam.getType().getSimpleName().equals(parameterTypeAsString))
                .findFirst()
                .map(InterceptedParam::getValue)
                .orElse(null);
    }


    public Optional<MethodExecution> findNameMatchingExecution(final CompilationUnit originCompilationUnit,
                                                               final MethodDeclaration originMethod) {
        final String methodNameAsString = originMethod.getNameAsString();
        final String fullClassName = this.generatorUtils.getOriginFullClassName(originCompilationUnit);
        return this.cache.stream()
                .filter(entry -> entry.getName().equals(methodNameAsString))
                .filter(entry -> entry.getClassName().equals(fullClassName))
                .findFirst();
    }

    public List<MethodExecution> findTimeCompatibleExecutions(final MethodExecution methodExecution) {
        final long startTime = methodExecution.getStartTime();
        final long endTime = methodExecution.getEndTime();
        return this.cache.stream()
                .filter(entry -> entry.getStartTime() > startTime)
                .filter(entry -> entry.getEndTime() < endTime)
                .toList();
    }
}
