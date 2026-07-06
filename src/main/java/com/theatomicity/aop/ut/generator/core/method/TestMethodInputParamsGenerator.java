package com.theatomicity.aop.ut.generator.core.method;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.theatomicity.aop.ut.generator.cache.MethodExecutionCache;
import org.springframework.stereotype.Component;

@Component
public class TestMethodInputParamsGenerator {

    private final MethodExecutionCache cache;

    public TestMethodInputParamsGenerator(final MethodExecutionCache cache) {
        this.cache = cache;
    }

    public Expression handle(final CompilationUnit originCu,
                             final MethodDeclaration originMethod,
                             final BlockStmt blockStmt,
                             final Parameter parameter) {
        final Object inputParamValue = this.cache.findInputParamValue(originCu, originMethod, parameter);
        final Type parameterType = parameter.getType();
        if (parameterType.isPrimitiveType()) {
            return this.processPrimitive(parameter, blockStmt, inputParamValue);
        } else {
            return this.processReference(parameter, blockStmt, inputParamValue);
        }
    }

    private Expression processPrimitive(final Parameter parameter, final BlockStmt blockStmt, final Object inputParamValue) {
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
            case INT:
                initializer = new IntegerLiteralExpr((int) inputParamValue);
                break;
            case LONG:
                initializer = new LongLiteralExpr((Long) inputParamValue);
                break;
            case DOUBLE:
                initializer = new DoubleLiteralExpr((Double) inputParamValue);
                break;
            default:
                initializer = new CharLiteralExpr((String) inputParamValue); // Covers byte, short, int, long, float, double
                break;
        }
        final String argName = parameter.getNameAsString();
        final VariableDeclarator argDeclarator = new VariableDeclarator(parameterType, argName, initializer);
        final VariableDeclarationExpr argDeclarationExpr = new VariableDeclarationExpr(argDeclarator);
        blockStmt.addStatement(new ExpressionStmt(argDeclarationExpr));
        return new NameExpr(argName);
    }

    private Expression processReference(final Parameter parameter, final BlockStmt blockStmt,
                                        final Object inputParamValue) {
        final Type parameterType = parameter.getType();
        final Expression initializer;
        final String argName = parameter.getNameAsString();
        if (StaticJavaParser.parseClassOrInterfaceType("Long").equals(parameterType)) {
            this.longBoxed(blockStmt, inputParamValue, parameterType, argName);
        } else if (StaticJavaParser.parseClassOrInterfaceType("String").equals(parameterType)) {
            this.stringInput(blockStmt, (String) inputParamValue, parameterType, argName);
        } else if (StaticJavaParser.parseClassOrInterfaceType("Integer").equals(parameterType)) {
            this.integerBoxed(blockStmt, inputParamValue, parameterType, argName);
        } else {
            // Mock
            initializer = new MethodCallExpr(null, "mock", new NodeList<>(new ClassExpr(parameterType)));
            final VariableDeclarator argDeclarator = new VariableDeclarator(parameterType, argName, initializer);
            final VariableDeclarationExpr argDeclarationExpr = new VariableDeclarationExpr(argDeclarator);
            blockStmt.addStatement(new ExpressionStmt(argDeclarationExpr));
        }
        return new NameExpr(argName);
    }

    private void stringInput(final BlockStmt blockStmt, final String inputParamValue, final Type parameterType, final String argName) {
        final Expression initializer = new StringLiteralExpr(inputParamValue);
        final VariableDeclarator argDeclarator = new VariableDeclarator(parameterType, argName, initializer);
        final VariableDeclarationExpr argDeclarationExpr = new VariableDeclarationExpr(argDeclarator);
        blockStmt.addStatement(new ExpressionStmt(argDeclarationExpr));
    }

    private void integerBoxed(final BlockStmt blockStmt, final Object interceptedParamValue, final Type parameterType, final String argName) {
        final String intExpr = String.format("%d", (Integer) interceptedParamValue);
        this.addBoxed(blockStmt, parameterType, argName, intExpr);
    }

    private void longBoxed(final BlockStmt blockStmt, final Object interceptedParamValue,
                           final Type parameterType, final String argName) {
        final String longExpr = String.format("%dL", (Long) interceptedParamValue);
        this.addBoxed(blockStmt, parameterType, argName, longExpr);
    }

    private void addBoxed(final BlockStmt blockStmt, final Type parameterType, final String argName, final String intExpr) {
        final Expression initializer = new IntegerLiteralExpr(intExpr);
        final VariableDeclarator argDeclarator = new VariableDeclarator(parameterType, argName, initializer);
        final VariableDeclarationExpr argDeclarationExpr = new VariableDeclarationExpr(argDeclarator);
        blockStmt.addStatement(new ExpressionStmt(argDeclarationExpr));
    }

}
