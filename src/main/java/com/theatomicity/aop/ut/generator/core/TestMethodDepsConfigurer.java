package com.theatomicity.aop.ut.generator.core;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.Type;
import com.theatomicity.aop.ut.generator.cache.MethodExecutionCache;
import com.theatomicity.aop.ut.generator.model.InterceptedParam;
import com.theatomicity.aop.ut.generator.model.MethodExecution;
import com.theatomicity.aop.ut.generator.utils.GeneratorUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TestMethodDepsConfigurer {

    public static final Pattern REPOSITORY_PATTERN = Pattern.compile(".*this.(.*Repository).*");

    private final MethodExecutionCache cache;

    private final GeneratorUtils generatorUtils;

    public void handle(final CompilationUnit originCompilationUnit, final MethodDeclaration originMethod, final BlockStmt blockStmt,
                       final CompilationUnit testCompilationUnit) {
        this.cache.findNameMatchingExecution(originCompilationUnit, originMethod)
                .map(this.cache::findTimeCompatibleExecutions)
                .ifPresent(compatibleExecutions -> compatibleExecutions.stream()
                        //.filter(compatibleExecution -> this.hasMethodNameInBody(originMethod, compatibleExecution.getName()))
                        //.filter(compatibleExecution -> this.hasCompatibleClassNameInBody(originMethod, compatibleExecution.getSimpleClassName()))
                        .forEach(compatibleExecution -> this.processCompatibleExecution(originCompilationUnit, originMethod, compatibleExecution, blockStmt, testCompilationUnit)));
    }

    // is not a requirement, dependencies can be called from private method call
    private Boolean hasMethodNameInBody(final MethodDeclaration originMethod, final String methodName) {
        return originMethod.getBody()
                .map(BlockStmt::getStatements)
                .map(NodeList::toString)
                .map(statements -> statements.contains(methodName))
                .orElse(false);
    }

    // is not a requirement, dependencies can be called from private method call
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

    private void processCompatibleExecution(final CompilationUnit originCompilationUnit, final MethodDeclaration originMethod,
                                            final MethodExecution compatibleExecution, final BlockStmt blockStmt,
                                            final CompilationUnit testCompilationUnit) {
        final String dependencyName = this.getDependencyName(originCompilationUnit, compatibleExecution, originMethod);
        final String dependencyMethodName = compatibleExecution.getName();
        final String dependencyArgs = this.getDependencyArgs(compatibleExecution);
        final String dependencyResult = this.getDependencyResult(compatibleExecution, testCompilationUnit);
        final String dependencyExpression = this.getDependencyExpression(dependencyResult, dependencyName, dependencyMethodName, dependencyArgs);
        blockStmt.addStatement(dependencyExpression);
    }

    private String getDependencyExpression(final String dependencyResult, final String dependencyName, final String dependencyMethodName, final String dependencyArgs) {
        if (Objects.isNull(dependencyResult)) {
            return String.format("doNothing().when(%s).%s(%s);",
                    dependencyName, dependencyMethodName, dependencyArgs);
        } else {
            return String.format("doReturn(%s).when(%s).%s(%s);",
                    dependencyResult, dependencyName, dependencyMethodName, dependencyArgs);
        }
    }

    private String getDependencyResult(final MethodExecution compatibleExecution, final CompilationUnit testCompilationUnit) {
        final InterceptedParam result = compatibleExecution.getResult();
        if (Objects.isNull(result)) {
            return null;
        }
        final String typeFullName = result.getType().getName();
        testCompilationUnit.getImports().add(new ImportDeclaration(typeFullName, false, false));
        final Class<?> type = result.getType();
        if (type.equals(String.class)) {
            return String.format("\"%s\"", result.getValue());
        } else if (type.equals(Character.class)) {
            return String.format("'%s'", result.getValue());
        } else if (type.equals(Long.class)) {
            return result.getValue() + "L";
        } else if (type.equals(Integer.class)) {
            return (String) result.getValue();
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

    // TODO in case of primitive?
    private String normalizeArg(final Object o) {
        return "any()";
    }

    // Match explicit dep or any parent class
    private String getDependencyName(final CompilationUnit originCompilationUnit,
                                     final MethodExecution compatibleExecution,
                                     final MethodDeclaration originMethod) {
        return this.getFromClassFields(originCompilationUnit, compatibleExecution)
                .orElseGet(() -> this.getDependencyFromBody(compatibleExecution, originMethod));
    }

    private Optional<String> getFromClassFields(final CompilationUnit originCompilationUnit, final MethodExecution methodExecution) {
        final Optional<String> hasFieldMatchingExecutionType = originCompilationUnit.getType(0).getFields().stream()
                .filter(field -> this.matchesType(field, methodExecution.getSimpleClassName()))
                .map(this::getNameAsString)
                .findFirst();
        final Optional<String> hasFieldMatchingCrudRepository = originCompilationUnit.getType(0).getFields().stream()
                .filter(field -> this.matchesCrudRepository(field, methodExecution))
                .map(this::getNameAsString)
                .findFirst();
        return hasFieldMatchingExecutionType.or(() -> hasFieldMatchingCrudRepository);

    }

    private boolean matchesCrudRepository(final FieldDeclaration field, final MethodExecution compatibleMethodExecution) {
        final Optional<String> hasCrudRepository = Optional.ofNullable(compatibleMethodExecution)
                .filter(methodExecution -> "CrudRepository".equals(methodExecution.getSimpleClassName()))
                .map(MethodExecution::getInputParams)
                .filter(params -> params.size() == 1)
                .map(params -> params.get(0))
                .map(InterceptedParam::getValue)
                .map(Object::getClass)
                .map(Class::getSimpleName)
                .map(this::getInterpolatedRepositoryName)
                .filter(customRepositoryName -> this.fieldMatchesCrudName(field, customRepositoryName));
        return hasCrudRepository.isPresent();
    }

    private String getInterpolatedRepositoryName(final String dependencyType) {
        return String.format("%sRepository", dependencyType);
    }

    private boolean fieldMatchesCrudName(final FieldDeclaration field, final String customRepositoryName) {
        final NodeList<VariableDeclarator> variables = field.getVariables();
        final VariableDeclarator variableDeclarator = variables.get(0);
        final Type type = variableDeclarator.getType();
        final String string = type.asString();
        return customRepositoryName.equals(string);
    }

    private boolean matchesType(final FieldDeclaration field, final String simpleClassName) {
        final VariableDeclarator variableDeclarator = field.getVariables().get(0);
        final Type type = variableDeclarator.getType();
        final String string = type.asString();
        return string.equals(simpleClassName);
    }

    private String getNameAsString(final FieldDeclaration field) {
        final VariableDeclarator variableDeclarator = field.getVariables().get(0);
        return variableDeclarator.getNameAsString();
    }

    private String getDependencyFromBody(final MethodExecution compatibleExecution,
                                         final MethodDeclaration originMethod) {
        final Optional<String> dependencyFromBody = originMethod.getBody()
                .map(BlockStmt::getStatements)
                .map(NodeList::toString)
                .map(REPOSITORY_PATTERN::matcher)
                .filter(Matcher::find)
                .map(matcher -> matcher.group(1));
        return dependencyFromBody.orElse(null);
    }
}
