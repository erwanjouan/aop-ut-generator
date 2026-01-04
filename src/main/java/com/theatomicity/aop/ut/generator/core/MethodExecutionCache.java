package com.theatomicity.aop.ut.generator.core;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;


@Getter
@Setter
@Component
@RequiredArgsConstructor
public class MethodExecutionCache {

    private final GeneratorUtils generatorUtils;

    private final List<MethodExecution> cache = new ArrayList<>();

    public void add(final MethodExecution methodExecution) {
        this.cache.add(methodExecution);
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
                .map(InterceptedParam::getValue)
                .findFirst()
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
