package com.theatomicity.aop.ut.generator.core.method;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import com.theatomicity.aop.ut.generator.utils.GeneratorUtils;
import org.springframework.stereotype.Component;

@Component
public class UtMethodGenerator {

    private final GeneratorUtils generatorUtils;

    private final TestMethodInputParamsGenerator testMethodInputParamsGenerator;

    private final TestMethodDepsConfigurer testMethodDepsConfigurer;

    public UtMethodGenerator(final GeneratorUtils generatorUtils,
                             final TestMethodInputParamsGenerator testMethodInputParamsGenerator,
                             final TestMethodDepsConfigurer testMethodDepsConfigurer) {
        this.generatorUtils = generatorUtils;
        this.testMethodInputParamsGenerator = testMethodInputParamsGenerator;
        this.testMethodDepsConfigurer = testMethodDepsConfigurer;
    }

    public MethodDeclaration processOriginMethod(final CompilationUnit originCu,
                                                 final MethodDeclaration originMethod,
                                                 final CompilationUnit utCu) {
        // creates empty @Test method
        final MethodDeclaration testMethodDeclaration = this.initEmptyMethod(originMethod, utCu);
        // Create new blockStmt
        final BlockStmt blockStmt = new BlockStmt();
        // adds calling parameters
        final NodeList<Expression> callingParameters = this.addCallingParameters(originCu, originMethod, blockStmt);
        // add dependency stubbing
        this.testMethodDepsConfigurer.handle(originCu, originMethod, blockStmt, utCu);
        // adds method call
        this.addMethodCall(originCu, originMethod, blockStmt, callingParameters, utCu);
        // adds blockStmt to method
        return testMethodDeclaration.setBody(blockStmt);
    }

    public MethodDeclaration initEmptyMethod(final MethodDeclaration originMethodDeclaration, final CompilationUnit utCu) {
        final String testMethodName = originMethodDeclaration.getName().asString();
        final NodeList<ReferenceType> thrownExceptions = originMethodDeclaration.getThrownExceptions();
        final Type voidType = new VoidType();
        final NodeList<Modifier> modifiers = new NodeList<>();
        final MethodDeclaration testMethodDeclaration = new MethodDeclaration(modifiers, voidType, testMethodName);
        testMethodDeclaration.addAnnotation("Test");
        this.generatorUtils.addImportIfNotExists(utCu, "org.junit.jupiter.api.Test", false, false);
        if (thrownExceptions.isNonEmpty()) {
            testMethodDeclaration.setThrownExceptions(thrownExceptions);
        }
        return testMethodDeclaration;
    }


    private NodeList<Expression> addCallingParameters(final CompilationUnit originCu,
                                                      final MethodDeclaration originMethod,
                                                      final BlockStmt blockStmt) {
        final NodeList<Expression> callArguments = new NodeList<>();
        for (final Parameter parameter : originMethod.getParameters()) {
            final Expression variable = this.testMethodInputParamsGenerator.handle(originCu, originMethod, blockStmt, parameter);
            callArguments.add(variable);
        }
        return callArguments;
    }

    public void addMethodCall(final CompilationUnit originCu, final MethodDeclaration originMethod,
                              final BlockStmt blockStmt, final NodeList<Expression> callingParameters, final CompilationUnit utCu) {
        final ClassOrInterfaceDeclaration originClass = this.generatorUtils.getMainType(originCu);
        final String originClassName = originClass.getNameAsString();
        final String underTestName = this.generatorUtils.getInstanceName(originClassName);
        final MethodCallExpr methodCallExpr = new MethodCallExpr(
                new NameExpr(underTestName),
                originMethod.getName(),
                callingParameters
        );
        final Type type = originMethod.getType();
        if (type.isVoidType()) {
            blockStmt.addStatement(methodCallExpr);
        } else {
            final NodeList<Expression> callArguments = new NodeList<>();
            callArguments.add(methodCallExpr);
            this.generatorUtils.addImportIfNotExists(utCu, "org.junit.jupiter.api.Assertions.assertNotNull", true, false);
            final MethodCallExpr nonNullAssertion = new MethodCallExpr(null, "assertNotNull", callArguments);
            blockStmt.addStatement(nonNullAssertion);
        }
    }
}
