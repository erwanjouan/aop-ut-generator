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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class TestMethodDepsConfigurer {

    private static final Logger log = LoggerFactory.getLogger(TestMethodDepsConfigurer.class);

    public static final Pattern REPOSITORY_PATTERN = Pattern.compile(".*this.(.*Repository).*");
    public static final String MOCK_OBJECT_PATTERN = "mock(%s.class)";

    private final MethodExecutionCache cache;

    private final GeneratorUtils generatorUtils;

    public TestMethodDepsConfigurer(final MethodExecutionCache cache, final GeneratorUtils generatorUtils) {
        this.cache = cache;
        this.generatorUtils = generatorUtils;
    }

    public void handle(final CompilationUnit originCompilationUnit, final MethodDeclaration originMethod, final BlockStmt blockStmt,
                       final CompilationUnit testCompilationUnit) {
        this.cache.findNameMatchingExecution(originCompilationUnit, originMethod)
                .map(this.cache::findTimeCompatibleExecutions)
                .ifPresent(compatibleExecutions -> compatibleExecutions.stream()
                        .forEach(compatibleExecution -> this.processCompatibleExecution(originCompilationUnit, originMethod, compatibleExecution, blockStmt, testCompilationUnit)));
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
            return "doNothing().when(%s).%s(%s);".formatted(
                    dependencyName, dependencyMethodName, dependencyArgs);
        } else {
            return "doReturn(%s).when(%s).%s(%s);".formatted(
                    dependencyResult, dependencyName, dependencyMethodName, dependencyArgs);
        }
    }

    // doReturn(<?>).when (or doNothing().when)
    String getDependencyResult(final MethodExecution compatibleExecution, final CompilationUnit testCompilationUnit) {
        final InterceptedParam result = compatibleExecution.getResult();
        if (Objects.isNull(result)) {
            return null;
        }
        final Object value = result.getValue();
        return this.processDependencyResultRecursive(value, testCompilationUnit);
    }

    private String processDependencyResultRecursive(final Object value, final CompilationUnit testCompilationUnit) {
        final Class<?> clazz = value.getClass();
        if (this.isPrimitiveType(clazz)) {
            return this.getDependencyResultAsBoxedOrPrimitiveType(value);
        } else if (this.isBoxedType(clazz)) {
            return this.getDependencyResultAsBoxedOrPrimitiveType(value);
        } else if (this.isStringType(clazz)) {
            return this.getDependencyResultAsStringType(value);
        } else if (this.isCollection(clazz)) {
            return this.getDependencyResultAsCollectionType(value, testCompilationUnit);
        } else if (this.isMap(clazz)) {
            return this.getDependencyResultAsMapType(value, testCompilationUnit);
        } else if (this.isOptional(clazz)) {
            return this.getDependencyResultAsOptionalType(value, testCompilationUnit);
        } else {
            return this.getDependencyResultAsPlainObject(value, testCompilationUnit);
        }
    }

    private boolean isOptional(final Class<?> clazz) {
        return Optional.class.isAssignableFrom(clazz);
    }

    private boolean isMap(final Class<?> clazz) {
        return Map.class.isAssignableFrom(clazz);
    }

    private boolean isStringType(final Class<?> aClass) {
        return String.class.equals(aClass);
    }

    private boolean isPrimitiveType(final Class<?> aClass) {
        return aClass.isPrimitive();
    }

    private boolean isBoxedType(final Class<?> clazz) {
        return clazz == Integer.class ||
                clazz == Long.class ||
                clazz == Double.class ||
                clazz == Float.class ||
                clazz == Boolean.class ||
                clazz == Character.class ||
                clazz == Byte.class ||
                clazz == Short.class;
    }

    private boolean isCollection(final Class<?> clazz) {
        return Collection.class.isAssignableFrom(clazz);
    }

    private String getDependencyResultAsStringType(final Object value) {
        return "\"%s\"".formatted(value);
    }

    private String getDependencyResultAsBoxedOrPrimitiveType(final Object value) {
        if (value instanceof final Byte b) return "'%d'".formatted(b);
        if (value instanceof final Short s) return "'%d'".formatted(s);
        if (value instanceof final Character c) return "'%c'".formatted(c);
        if (value instanceof final Integer i) return "%d".formatted(i);
        if (value instanceof final Long l) return "%dL".formatted(l);
        if (value instanceof final Float f) return "%f".formatted(f);
        if (value instanceof final Double d) return "%f".formatted(d);
        if (value instanceof final Boolean b) return "%b".formatted(b);
        throw new IllegalStateException("Unexpected value: " + value);
    }

    private String getDependencyResultAsCollectionType(final Object value, final CompilationUnit testCompilationUnit) {
        final Collection collection = (Collection) value;
        final String simpleName = value.getClass().getSimpleName(); // List, Set...
        final String typeFullName = value.getClass().getName();
        testCompilationUnit.getImports().add(new ImportDeclaration(typeFullName, false, false));
        if (collection.isEmpty()) {
            return MOCK_OBJECT_PATTERN.formatted(simpleName);
        } else {
            final Object first = collection.iterator().next();
            final String innerString = this.processDependencyResultRecursive(first, testCompilationUnit);
            if (List.class.isAssignableFrom(value.getClass())) {
                return "List.of(%s)".formatted(innerString);
            } else if (Set.class.isAssignableFrom(value.getClass())) {
                return "Set.of(%s)".formatted(innerString);
            } else {
                return "Queue.of(%s)".formatted(innerString);
            }
        }
    }

    private String getDependencyResultAsMapType(final Object value, final CompilationUnit testCompilationUnit) {
        final String simpleName = value.getClass().getSimpleName(); // Map
        final Map map = (Map) value;
        if (map.isEmpty()) {
            return MOCK_OBJECT_PATTERN.formatted(simpleName);
        } else {
            final Set set = map.entrySet();
            final Map.Entry entry = (Map.Entry) set.iterator().next();
            final String keyString = this.processDependencyResultRecursive(entry.getKey(), testCompilationUnit);
            final String valueString = this.processDependencyResultRecursive(entry.getValue(), testCompilationUnit);
            return "%s.of(%s,%s)".formatted(simpleName, keyString, valueString);
        }
    }

    private String getDependencyResultAsPlainObject(final Object value, final CompilationUnit testCompilationUnit) {
        final String simpleName = value.getClass().getSimpleName();
        final String typeFullName = value.getClass().getName();
        testCompilationUnit.getImports().add(new ImportDeclaration(typeFullName, false, false));
        return MOCK_OBJECT_PATTERN.formatted(simpleName);
    }

    private String getDependencyArgs(final MethodExecution compatibleExecution) {
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
                .orElseGet(() -> this.getDependencyFromBody(originMethod));
    }

    private Optional<String> getFromClassFields(final CompilationUnit originCompilationUnit,
                                                final MethodExecution methodExecution) {
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

    private boolean matchesCrudRepository(final FieldDeclaration field,
                                          final MethodExecution compatibleMethodExecution) {
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

    private String getDependencyResultAsOptionalType(final Object value, final CompilationUnit testCompilationUnit) {
        final Optional<?> optional = (Optional<?>) value;
        final String typeFullName = value.getClass().getName();
        testCompilationUnit.getImports().add(new ImportDeclaration(typeFullName, false, false));
        return optional.map(Object::getClass)
                .map(clazz -> String.format(MOCK_OBJECT_PATTERN, clazz.getSimpleName()))
                .map(mock -> String.format("Optional.of(%s)", mock))
                .orElse("Optional.empty()");
    }

    private String getInterpolatedRepositoryName(final String dependencyType) {
        return "%sRepository".formatted(dependencyType);
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

    private String getDependencyFromBody(final MethodDeclaration originMethod) {
        final Optional<String> dependencyFromBody = originMethod.getBody()
                .map(BlockStmt::getStatements)
                .map(NodeList::toString)
                .map(REPOSITORY_PATTERN::matcher)
                .filter(Matcher::find)
                .map(matcher -> matcher.group(1));
        return dependencyFromBody.orElse(null);
    }
}
