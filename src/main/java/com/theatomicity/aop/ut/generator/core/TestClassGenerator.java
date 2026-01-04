package com.theatomicity.aop.ut.generator.core;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.printer.DefaultPrettyPrinterVisitor;
import com.github.javaparser.printer.configuration.DefaultConfigurationOption;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import com.github.javaparser.printer.configuration.Indentation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TestClassGenerator {

    private final TestMethodGenerator testMethodGenerator;

    private final GeneratorUtils generatorUtils;

    public static final String SRC_BASEPATH = "src/main/java";
    public static final String TEST_BASEPATH = "src/test/java";
    public static final String AUTO_TEST_SUFFIX = "AutoTest";
    public static final String JAVA_EXTENSION = ".java";

    public void generateUnitTest(final MethodExecution methodExecution) {
        final File sourceFile = this.getJavaFile(methodExecution, SRC_BASEPATH, "");
        try {
            final String processedClass = Files.readString(sourceFile.toPath());
            final CompilationUnit originCompilationUnit = StaticJavaParser.parse(processedClass);
            final MethodDeclaration originMethod = this.findOriginMethodDeclaration(originCompilationUnit, methodExecution);
            final MethodDeclaration newTestMethodDeclaration = this.testMethodGenerator.processOriginMethod(originCompilationUnit, originMethod);
            this.dumpTestClass(originCompilationUnit, methodExecution, newTestMethodDeclaration);
        } catch (final IOException e) {
            log.info("{} {}", e.getClass().getName(), e.getMessage());
        }
    }

    private MethodDeclaration findOriginMethodDeclaration(final CompilationUnit originCompilationUnit,
                                                          final MethodExecution methodExecution) {
        final ClassOrInterfaceDeclaration originClass = this.generatorUtils.getMainType(originCompilationUnit);
        final List<? extends Class<?>> paramTypes = methodExecution.getInputParams().stream()
                .map(InterceptedParam::getType)
                .toList();
        final Class<?>[] paramTypesArray = new Class[paramTypes.size()];
        paramTypes.toArray(paramTypesArray);
        return originClass.getMethods().stream()
                .filter(methodDeclaration -> methodDeclaration.getNameAsString().equals(methodExecution.getName()))
                .filter(methodDeclaration -> methodDeclaration.hasParametersOfType(paramTypesArray))
                .findFirst()
                .orElseThrow();
    }


    private void dumpTestClass(final CompilationUnit originCompilationUnit,
                               final MethodExecution methodExecution,
                               final MethodDeclaration testMethodDeclaration) {
        final File testFile = this.getJavaFile(methodExecution, TEST_BASEPATH, AUTO_TEST_SUFFIX);
        try {
            final CompilationUnit testCompilationUnit = this.getTestCompilationUnit(originCompilationUnit, testFile);
            this.copyMethodContentToTest(methodExecution, testMethodDeclaration, testCompilationUnit);
            this.createOrUpdateNewTestFile(testCompilationUnit, testFile);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private CompilationUnit getTestCompilationUnit(final CompilationUnit originCompilationUnit,
                                                   final File testFile) throws IOException {
        if (testFile.exists()) {
            final String processedClass = Files.readString(testFile.toPath());
            return StaticJavaParser.parse(processedClass);
        } else {
            return this.initTestClass(originCompilationUnit);
        }
    }

    private void copyMethodContentToTest(final MethodExecution methodExecution, final MethodDeclaration testMethodDeclaration, final CompilationUnit testCompilationUnit) {
        final String testMethodName = String.format("%s_%d", methodExecution.getName(), methodExecution.getHashCode());
        final MethodDeclaration methodDeclaration = testCompilationUnit.getType(0)
                .addMethod(testMethodName)
                .addAnnotation("Test");
        testMethodDeclaration.getBody().ifPresent(methodDeclaration::setBody);
    }

    private CompilationUnit initTestClass(final CompilationUnit originCompilationUnit) {

        final TypeDeclaration<?> type = originCompilationUnit.getType(0);
        final ClassOrInterfaceDeclaration originClass = (ClassOrInterfaceDeclaration) type;

        final CompilationUnit testCompilationUnit = new CompilationUnit();
        // package
        this.setPackage(originCompilationUnit, testCompilationUnit);

        // import
        this.addImports(originCompilationUnit, testCompilationUnit);

        // class and annotation
        final ClassOrInterfaceDeclaration testClass = this.getClassAndAnnotation(originClass, testCompilationUnit);

        // class fields for dependencies
        this.addClassFields(originClass, testClass);

        // underTest
        this.addTestedClassField(originClass, testClass);

        return testCompilationUnit;
    }

    private void setPackage(final CompilationUnit originCompilationUnit, final CompilationUnit testCompilationUnit) {
        originCompilationUnit.getPackageDeclaration().ifPresent(
                testCompilationUnit::setPackageDeclaration
        );
    }


    private void addImports(final CompilationUnit originCompilationUnit, final CompilationUnit testCompilationUnit) {
        testCompilationUnit.setImports(originCompilationUnit.getImports());
        testCompilationUnit.addImport("java.util.Optional");
        testCompilationUnit.addImport("java.util.ArrayList");
        testCompilationUnit.addImport("org.junit.jupiter.api.extension.ExtendWith");
        testCompilationUnit.addImport("org.mockito.junit.jupiter.MockitoExtension");
        testCompilationUnit.addImport("org.mockito.Mock");
        testCompilationUnit.addImport("org.mockito.Spy");
        testCompilationUnit.addImport("org.mockito.InjectMocks");
        testCompilationUnit.addImport("org.junit.jupiter.api.Test");
        testCompilationUnit.addImport("org.mockito.Mockito.doReturn", true, false);
        testCompilationUnit.addImport("org.mockito.Mockito.doNothing", true, false);
        testCompilationUnit.addImport("org.mockito.Mockito.mock", true, false);
        testCompilationUnit.addImport("org.mockito.ArgumentMatchers.any", true, false);
        testCompilationUnit.addImport("org.junit.jupiter.api.Assertions.assertNotNull", true, false);
    }

    private ClassOrInterfaceDeclaration getClassAndAnnotation(final ClassOrInterfaceDeclaration originClass,
                                                              final CompilationUnit testCompilationUnit) {
        final SimpleName originClassName = originClass.getName();
        final String testName = String.format("%s%s", originClassName, AUTO_TEST_SUFFIX);
        final ClassOrInterfaceDeclaration testClass = testCompilationUnit.addClass(testName).setPublic(true);
        testClass.addSingleMemberAnnotation("ExtendWith", "MockitoExtension.class");
        return testClass;
    }

    private void addClassFields(final ClassOrInterfaceDeclaration originClass, final ClassOrInterfaceDeclaration testClass) {
        for (final FieldDeclaration field : originClass.getFields()) {
            final Type commonType = field.getCommonType();
            final VariableDeclarator variableDeclarator = field.getVariables().get(0);
            final SimpleName name = variableDeclarator.getName();
            final FieldDeclaration fieldDeclaration = testClass.addPrivateField(commonType, name.asString());
            fieldDeclaration.addMarkerAnnotation("Mock");
        }
    }

    private void addTestedClassField(final ClassOrInterfaceDeclaration originClass, final ClassOrInterfaceDeclaration testClass) {
        final String originClassName = originClass.getName().asString();
        final ClassOrInterfaceType originType = StaticJavaParser.parseClassOrInterfaceType(originClassName);
        final String instanceName = this.generatorUtils.getInstanceName(originClassName);
        final FieldDeclaration underTest = testClass.addPrivateField(originType, instanceName);
        underTest.addMarkerAnnotation("Spy");
        underTest.addMarkerAnnotation("InjectMocks");
    }

    private void createOrUpdateNewTestFile(final CompilationUnit compilationUnit, final File testFile) throws IOException {
        final Path testFilePath = testFile.toPath();
        final DefaultPrinterConfiguration printerConfiguration = new DefaultPrinterConfiguration();
        printerConfiguration.addOption(new DefaultConfigurationOption(DefaultPrinterConfiguration.ConfigOption.INDENTATION,
                new Indentation(Indentation.IndentType.SPACES, 2)));
        final DefaultPrettyPrinterVisitor visitor = new DefaultPrettyPrinterVisitor(printerConfiguration);
        compilationUnit.accept(visitor, null);
        final String formatted = visitor.toString();
        Files.createDirectories(testFilePath.getParent());
        if (!testFile.exists()) {
            Files.createFile(testFilePath);
        }
        Files.writeString(testFilePath, formatted);
    }

    private File getJavaFile(final MethodExecution methodExecution, final String basePath, final String suffix) {
        final String className = methodExecution.getClassName();
        final String path = className.replaceAll("\\.", "/");
        return new File(basePath, path + suffix + JAVA_EXTENSION);
    }

}
