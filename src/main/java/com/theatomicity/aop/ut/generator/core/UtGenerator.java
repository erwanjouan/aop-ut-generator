package com.theatomicity.aop.ut.generator.core;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.printer.DefaultPrettyPrinterVisitor;
import com.github.javaparser.printer.configuration.DefaultConfigurationOption;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import com.github.javaparser.printer.configuration.Indentation;
import com.theatomicity.aop.ut.generator.core.cu.UtCuGenerator;
import com.theatomicity.aop.ut.generator.core.method.UtMethodGenerator;
import com.theatomicity.aop.ut.generator.model.InterceptedParam;
import com.theatomicity.aop.ut.generator.model.MethodExecution;
import com.theatomicity.aop.ut.generator.utils.GeneratorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class UtGenerator {

    private static final Logger log = LoggerFactory.getLogger(UtGenerator.class);

    private final UtMethodGenerator utMethodGenerator;

    private final GeneratorUtils generatorUtils;

    private final UtCuGenerator cuGenerator;

    public UtGenerator(final UtMethodGenerator utMethodGenerator, final GeneratorUtils generatorUtils, final UtCuGenerator cuGenerator) {
        this.utMethodGenerator = utMethodGenerator;
        this.generatorUtils = generatorUtils;
        this.cuGenerator = cuGenerator;
    }

    public static final String SRC_BASEPATH = "src/main/java";
    public static final String TEST_BASEPATH = "src/test/java";
    public static final String AUTO_TEST_SUFFIX = "AutoTest";
    public static final String JAVA_EXTENSION = ".java";

    public void generateUnitTest(final MethodExecution methodExecution) {
        File sourceFile = this.getJavaFile(methodExecution, SRC_BASEPATH, "");
        if (!sourceFile.exists()) {
            sourceFile = this.getJavaFile(methodExecution, TEST_BASEPATH, "");
        }
        try {
            final String processedClass = Files.readString(sourceFile.toPath());
            final CompilationUnit originCu = StaticJavaParser.parse(processedClass);
            final MethodDeclaration originMethod = this.findOriginMethodDeclaration(originCu, methodExecution);
            log.debug("Generating Unit Test Class for {} {}", sourceFile.getName(), originMethod);
            final File testFile = this.getJavaFile(methodExecution, TEST_BASEPATH, AUTO_TEST_SUFFIX);
            log.debug("testFile {}", testFile.getName());
            final CompilationUnit utCu = this.cuGenerator.getUtCu(originCu, testFile);
            final MethodDeclaration newTestMethodDeclaration = this.utMethodGenerator.processOriginMethod(originCu, originMethod, utCu);
            log.debug("generated test method {}", newTestMethodDeclaration);
            this.dumpTestClass(utCu, methodExecution, newTestMethodDeclaration, testFile);
        } catch (final IOException e) {
            log.debug("{} {}", e.getClass().getName(), e.getMessage());
        }
    }

    private MethodDeclaration findOriginMethodDeclaration(final CompilationUnit originCu,
                                                          final MethodExecution methodExecution) {
        final ClassOrInterfaceDeclaration originClass = this.generatorUtils.getMainType(originCu);
        final List<? extends Class<?>> paramTypes = methodExecution.getInputParams().stream()
                .map(InterceptedParam::getType)
                .toList();
        final Class<?>[] paramTypesArray = new Class[paramTypes.size()];
        paramTypes.toArray(paramTypesArray);
        return originClass.getMethods().stream()
                .filter(methodDeclaration -> methodDeclaration.getNameAsString().equals(methodExecution.getMethodName()))
                .filter(methodDeclaration -> methodDeclaration.hasParametersOfType(paramTypesArray))
                .findFirst()
                .orElseThrow();
    }


    private void dumpTestClass(final CompilationUnit utCu,
                               final MethodExecution methodExecution,
                               final MethodDeclaration testMethodDeclaration, final File testFile) {
        try {
            this.copyMethodContentToTest(methodExecution, testMethodDeclaration, utCu);
            this.createOrUpdateNewTestFile(utCu, testFile);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void copyMethodContentToTest(final MethodExecution methodExecution,
                                         final MethodDeclaration testMethodDeclaration,
                                         final CompilationUnit utCu) {
        final String testMethodName = String.format("%s_%d", methodExecution.getMethodName(), methodExecution.getHashCode());
        final MethodDeclaration methodDeclaration = utCu.getType(0)
                .addMethod(testMethodName)
                .addAnnotation("Test");
        methodDeclaration.setThrownExceptions(testMethodDeclaration.getThrownExceptions());
        testMethodDeclaration.getBody().ifPresent(methodDeclaration::setBody);
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
