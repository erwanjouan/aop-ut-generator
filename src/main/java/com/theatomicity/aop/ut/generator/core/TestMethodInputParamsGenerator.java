package com.theatomicity.aop.ut.generator.core;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TestMethodInputParamsGenerator {

    private final MethodExecutionCache cache;

    public Expression handle(final CompilationUnit originCompilationUnit,
                             final MethodDeclaration originMethod,
                             final BlockStmt blockStmt,
                             final Parameter parameter) {
        final Object inputParamValue = this.cache.findInputParamValue(originCompilationUnit, originMethod, parameter);
        final Type parameterType = parameter.getType();
        if (parameterType.isPrimitiveType()) {
            return this.primitiveFill(parameter, blockStmt, inputParamValue);
        } else {
            return this.referenceFill(parameter, blockStmt, inputParamValue);
        }
    }

    private Expression primitiveFill(final Parameter parameter, final BlockStmt blockStmt, final Object inputParamValue) {
        final Type parameterType = parameter.getType();
        final PrimitiveType primitiveType = parameterType.asPrimitiveType();
        final Expression initializer;
        switch (primitiveType.getType()) {
            case BOOLEAN:
                initializer = new BooleanLiteralExpr((Boolean) inputParamValue);
                break;
            case CHAR:
                initializer = new CharLiteralExpr((String) inputParamValue);
                break;
            default:
                initializer = new IntegerLiteralExpr((String) inputParamValue); // Covers byte, short, int, long, float, double
                break;
        }
        final String argName = parameter.getNameAsString();
        final VariableDeclarator argDeclarator = new VariableDeclarator(parameterType, argName, initializer);
        final VariableDeclarationExpr argDeclarationExpr = new VariableDeclarationExpr(argDeclarator);
        blockStmt.addStatement(new ExpressionStmt(argDeclarationExpr));
        return new NameExpr(argName);
    }

    private Expression referenceFill(final Parameter parameter, final BlockStmt blockStmt,
                                     final Object inputParamValue) {
        final Type parameterType = parameter.getType();
        final Expression initializer;
        final String argName = parameter.getNameAsString();
        if (StaticJavaParser.parseClassOrInterfaceType("Long").equals(parameterType)) {
            this.longBoxed(blockStmt, inputParamValue, parameterType, argName);
        } else {
            // Mock
            initializer = new MethodCallExpr(null, "mock", new NodeList<>(new ClassExpr(parameterType)));
            final VariableDeclarator argDeclarator = new VariableDeclarator(parameterType, argName, initializer);
            final VariableDeclarationExpr argDeclarationExpr = new VariableDeclarationExpr(argDeclarator);
            blockStmt.addStatement(new ExpressionStmt(argDeclarationExpr));
        }
        return new NameExpr(argName);
    }


    private void longBoxed(final BlockStmt blockStmt, final Object interceptedParamValue,
                           final Type parameterType, final String argName) {
        final String longExpr = String.format("%dL", (Long) interceptedParamValue);
        final Expression initializer = new IntegerLiteralExpr(longExpr);
        final VariableDeclarator argDeclarator = new VariableDeclarator(parameterType, argName, initializer);
        final VariableDeclarationExpr argDeclarationExpr = new VariableDeclarationExpr(argDeclarator);
        blockStmt.addStatement(new ExpressionStmt(argDeclarationExpr));
    }

}
