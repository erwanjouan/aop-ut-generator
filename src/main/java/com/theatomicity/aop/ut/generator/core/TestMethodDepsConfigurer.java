package com.theatomicity.aop.ut.generator.core;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TestMethodDepsConfigurer {

    public static final Pattern REPOSITORY_PATTERN = Pattern.compile(".*this.(.*Repository).*");

    private final MethodExecutionCache cache;

    private final GeneratorUtils generatorUtils;

    public void handle(final CompilationUnit originCompilationUnit, final MethodDeclaration originMethod, final BlockStmt blockStmt) {
        this.cache.findNameMatchingExecution(originCompilationUnit, originMethod)
                .map(this.cache::findTimeCompatibleExecutions)
                .ifPresent(compatibleExecutions -> compatibleExecutions.stream()
                        .filter(compatibleExecution -> this.hasMethodNameInBody(originMethod, compatibleExecution.getName()))
                        .filter(compatibleExecution -> this.hasCompatibleClassNameInBody(originMethod, compatibleExecution.getSimpleClassName()))
                        .forEach(compatibleExecution -> this.processCompatibleExecution(originCompilationUnit, originMethod, compatibleExecution, blockStmt)));
    }

    private boolean hasCompatibleClassNameInBody(final MethodDeclaration originMethod, final String simpleClassName) {
        return originMethod.getBody()
                .map(BlockStmt::getStatements)
                .map(NodeList::toString)
                .map(statements -> this.hasCompatibleClassName(statements, simpleClassName))
                .orElse(false);
    }

    private Boolean hasCompatibleClassName(final String statements, final String simpleClassName) {
        final String instanceName = this.generatorUtils.getInstanceName(simpleClassName);
        final boolean simpleMatch = statements.contains(instanceName);
        final boolean repositoryMatch = REPOSITORY_PATTERN.matcher(statements).find();
        return simpleMatch || repositoryMatch;
    }

    private Boolean hasMethodNameInBody(final MethodDeclaration originMethod, final String methodName) {
        return originMethod.getBody()
                .map(BlockStmt::getStatements)
                .map(NodeList::toString)
                .map(statements -> statements.contains(methodName))
                .orElse(false);
    }

    private void processCompatibleExecution(final CompilationUnit originCompilationUnit, final MethodDeclaration originMethod, final MethodExecution compatibleExecution, final BlockStmt blockStmt) {
        final String dependencyName = this.getDependencyName(originCompilationUnit, compatibleExecution, originMethod);
        final String dependencyMethodName = compatibleExecution.getName();
        final String dependencyArgs = this.getDependencyArgs(compatibleExecution);
        final String dependencyResult = this.getDependencyResult(compatibleExecution);
        final String dependencyExpression = getDependencyExpression(dependencyResult, dependencyName, dependencyMethodName, dependencyArgs);
        blockStmt.addStatement(dependencyExpression);
    }

    private String getDependencyExpression(String dependencyResult, String dependencyName, String dependencyMethodName, String dependencyArgs) {
        if(Objects.isNull(dependencyResult)){
            return String.format("doNothing().when(%s).%s(%s);",
                    dependencyName, dependencyMethodName, dependencyArgs);
        }
        else {
            return String.format("doReturn(%s).when(%s).%s(%s);",
                    dependencyResult, dependencyName, dependencyMethodName, dependencyArgs);
        }
    }

    private String getDependencyResult(final MethodExecution compatibleExecution) {
        final InterceptedParam result = compatibleExecution.getResult();
        if (Objects.isNull(result)){
            return null;
        }
        final Class<?> type = result.getType();
        if (type.equals(String.class)) {
            return String.format("\"%s\"", result.getValue());
        } else if (type.equals(Character.class)) {
            return String.format("'%s'", result.getValue());
        } else if (type.equals(Long.class)) {
            return result.getValue() + "L";
        } else if (type.equals(Optional.class)) {
            final Optional<?> optional = (Optional<?>) result.getValue();
            return optional.map(Object::getClass)
                    .map(clazz -> String.format("mock(%s.class)", clazz.getSimpleName()))
                    .map(mock -> String.format("Optional.of(%s)", mock))
                    .orElse("Optional.empty()");
        } else {
            return "mock(" + type.getSimpleName() + ".class)";
        }
    }

    private @NonNull String getDependencyArgs(final MethodExecution compatibleExecution) {
        return compatibleExecution.getInputParams().stream()
                .map(InterceptedParam::getValue)
                .map(this::normalizeArg)
                .collect(Collectors.joining(", "));
    }

    private String normalizeArg(final Object o) {
        if (o instanceof String) {
            return String.format("\"%s\"", o);
        } else if (o instanceof Character) {
            return String.format("'%s'", o);
        } else if (o instanceof Long) {
            return o + "L";
        } else {
            return "any(" + o.getClass().getSimpleName() + ".class)";
        }
    }

    private String getDependencyName(final CompilationUnit originCompilationUnit, final MethodExecution compatibleExecution, final MethodDeclaration originMethod) {
        final String simpleClassName = compatibleExecution.getSimpleClassName();
        return originCompilationUnit.getType(0).getFields().stream()
                .filter(field -> field.getVariables().get(0).getType().asString().equals(simpleClassName))
                .map(field -> field.getVariables().get(0).getNameAsString())
                .findFirst()
                .orElse(this.getCrudRepository(originMethod));
    }

    private String getCrudRepository(final MethodDeclaration originMethod) {
        return originMethod.getBody()
                .map(BlockStmt::getStatements)
                .map(NodeList::toString)
                .map(REPOSITORY_PATTERN::matcher)
                .filter(Matcher::find)
                .map(matcher -> matcher.group(1))
                .orElse(null);
    }
}
