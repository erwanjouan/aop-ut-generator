package com.theatomicity.aop.ut.generator.core;

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
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TestMethodGenerator {
    
    private final GeneratorUtils generatorUtils;

    private final TestMethodInputParamsGenerator testMethodInputParamsGenerator;

    private final TestMethodDepsConfigurer testMethodDepsConfigurer;

    public MethodDeclaration processOriginMethod(final CompilationUnit originCompilationUnit,
                                                 final MethodDeclaration originMethod) {
        // creates empty @Test method
        final MethodDeclaration testMethodDeclaration = this.initEmptyMethod(originMethod);
        // Create new blockStmt
        final BlockStmt blockStmt = new BlockStmt();
        // adds calling parameters
        final NodeList<Expression> callingParameters = this.addCallingParameters(originCompilationUnit, originMethod, blockStmt);
        // add dependency stubbing
        this.testMethodDepsConfigurer.handle(originCompilationUnit, originMethod, blockStmt);
        // adds method call
        this.addMethodCall(originCompilationUnit, originMethod, blockStmt, callingParameters);
        // adds blockStmt to method
        return testMethodDeclaration.setBody(blockStmt);
    }

    MethodDeclaration initEmptyMethod(final MethodDeclaration originMethodDeclaration) {
        final String testMethodName = originMethodDeclaration.getName().asString();
        final Type voidType = new VoidType();
        final NodeList<Modifier> modifiers = new NodeList<>();
        final MethodDeclaration testMethodDeclaration = new MethodDeclaration(modifiers, voidType, testMethodName);
        testMethodDeclaration.addMarkerAnnotation("Test");
        return testMethodDeclaration;
    }


    private NodeList<Expression> addCallingParameters(final CompilationUnit originCompilationUnit,
                                                      final MethodDeclaration originMethod,
                                                      final BlockStmt blockStmt) {
        final NodeList<Expression> callArguments = new NodeList<>();
        for (final Parameter parameter : originMethod.getParameters()) {
            final Expression variable = this.testMethodInputParamsGenerator.handle(originCompilationUnit, originMethod, blockStmt, parameter);
            callArguments.add(variable);
        }
        return callArguments;
    }

    void addMethodCall(final CompilationUnit originCompilationUnit, final MethodDeclaration originMethod,
                       final BlockStmt blockStmt, final NodeList<Expression> callingParameters) {
        final ClassOrInterfaceDeclaration originClass = this.generatorUtils.getMainType(originCompilationUnit);
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
            final MethodCallExpr nonNullAssertion = new MethodCallExpr(null, "assertNotNull", callArguments);
            blockStmt.addStatement(nonNullAssertion);
        }
    }
}
