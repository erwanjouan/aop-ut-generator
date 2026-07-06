package com.theatomicity.aop.ut.generator.core.cu;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.theatomicity.aop.ut.generator.utils.GeneratorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static com.theatomicity.aop.ut.generator.core.UtGenerator.AUTO_TEST_SUFFIX;

@Component
public class UtCuGenerator {

    private static final Logger log = LoggerFactory.getLogger(UtCuGenerator.class);

    private final GeneratorUtils generatorUtils;

    public UtCuGenerator(final GeneratorUtils generatorUtils) {
        this.generatorUtils = generatorUtils;
    }

    public CompilationUnit getUtCu(final CompilationUnit originCu,
                                   final File testFile) throws IOException {
        if (testFile.exists()) {
            final String processedClass = Files.readString(testFile.toPath());
            final CompilationUnit editedUtCu = StaticJavaParser.parse(processedClass);
            //log.debug("Reuses Test Class \n{}", editedUtCu);
            return editedUtCu;
        } else {
            final CompilationUnit newUtCu = this.getNewUtCu(originCu);
            //log.debug("Generated Test Class \n{}", newUtCu);
            return newUtCu;
        }
    }

    private CompilationUnit getNewUtCu(final CompilationUnit originCu) {

        final TypeDeclaration<?> type = originCu.getType(0);
        final ClassOrInterfaceDeclaration originClass = (ClassOrInterfaceDeclaration) type;

        final CompilationUnit utCu = new CompilationUnit();
        // package
        this.setBasePackage(originCu, utCu);

        // basic imports
        this.addDefaultImports(originCu, utCu);

        // add annotated class
        this.addClassAndAnnotation(originCu, utCu);

        // class fields for dependencies
        this.addClassFields(originCu, utCu);

        // underTest
        this.addTestedClassField(originCu, utCu);

        return utCu;
    }


    private void setBasePackage(final CompilationUnit originCu, final CompilationUnit utCu) {
        originCu.getPackageDeclaration().ifPresent(
                utCu::setPackageDeclaration
        );
    }

    private void addDefaultImports(final CompilationUnit originCu, final CompilationUnit utCu) {
        utCu.setImports(originCu.getImports());
        this.generatorUtils.addImportIfNotExists(utCu, "java.util.Optional", false, false);
        this.generatorUtils.addImportIfNotExists(utCu, "java.util.ArrayList", false, false);
        this.generatorUtils.addImportIfNotExists(utCu, "java.util.List", false, false);
        this.generatorUtils.addImportIfNotExists(utCu, "java.util.Map", false, false);
    }

    private void addClassFields(final CompilationUnit originCu, final CompilationUnit utCu) {
        final ClassOrInterfaceDeclaration originClass = this.getClassOrInterfaceDeclaration(originCu);
        final ClassOrInterfaceDeclaration testClass = this.getClassOrInterfaceDeclaration(utCu);
        for (final FieldDeclaration field : originClass.getFields()) {
            if (!field.isStatic()) {
                final Type commonType = field.getCommonType();
                final VariableDeclarator variableDeclarator = field.getVariables().get(0);
                final SimpleName name = variableDeclarator.getName();
                final FieldDeclaration fieldDeclaration = testClass.addPrivateField(commonType, name.asString());
                fieldDeclaration.addMarkerAnnotation("Mock");
                this.generatorUtils.addImportIfNotExists(utCu, "org.mockito.Mock", false, false);
            }
        }
    }

    private void addTestedClassField(final CompilationUnit originCu, final CompilationUnit utCu) {
        final ClassOrInterfaceDeclaration originClass = this.getClassOrInterfaceDeclaration(originCu);
        final String originClassName = originClass.getName().asString();
        final ClassOrInterfaceType originType = StaticJavaParser.parseClassOrInterfaceType(originClassName);
        final String instanceName = this.generatorUtils.getInstanceName(originClassName);
        final ClassOrInterfaceDeclaration testClass = this.getClassOrInterfaceDeclaration(utCu);
        final FieldDeclaration underTest = testClass.addPrivateField(originType, instanceName);
        this.addAnnotation(utCu, underTest);
        underTest.addMarkerAnnotation("InjectMocks");
        this.generatorUtils.addImportIfNotExists(utCu, "org.mockito.InjectMocks", false, false);
    }

    private void addAnnotation(final CompilationUnit utCu, final FieldDeclaration underTest) {
        underTest.addMarkerAnnotation("Spy");
        this.generatorUtils.addImportIfNotExists(utCu, "org.mockito.Spy", false, false);
    }

    private void addClassAndAnnotation(final CompilationUnit originCu,
                                       final CompilationUnit utCu) {
        final ClassOrInterfaceDeclaration originClass = this.getClassOrInterfaceDeclaration(originCu);
        final SimpleName originClassName = originClass.getName();
        final String testName = String.format("%s%s", originClassName, AUTO_TEST_SUFFIX);
        final ClassOrInterfaceDeclaration testClass = utCu.addClass(testName).setPublic(true);
        testClass.addSingleMemberAnnotation("ExtendWith", "MockitoExtension.class");
        testClass.setPublic(false);
        this.generatorUtils.addImportIfNotExists(utCu, "org.junit.jupiter.api.extension.ExtendWith", false, false);
        this.generatorUtils.addImportIfNotExists(utCu, "org.mockito.junit.jupiter.MockitoExtension", false, false);
    }

    private ClassOrInterfaceDeclaration getClassOrInterfaceDeclaration(final CompilationUnit cu) {
        final TypeDeclaration<?> type = cu.getType(0);
        return (ClassOrInterfaceDeclaration) type;
    }

}

