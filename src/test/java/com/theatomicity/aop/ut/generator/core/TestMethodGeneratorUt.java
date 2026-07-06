package com.theatomicity.aop.ut.generator.core;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.VoidType;
import com.theatomicity.aop.ut.generator.core.method.TestMethodDepsConfigurer;
import com.theatomicity.aop.ut.generator.core.method.TestMethodInputParamsGenerator;
import com.theatomicity.aop.ut.generator.core.method.UtMethodGenerator;
import com.theatomicity.aop.ut.generator.utils.GeneratorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class TestMethodGeneratorUt {

    // package-private methods initEmptyMethod() and addMethodCall() are
    // accessible here because this test lives in the same package.

    @Spy
    private GeneratorUtils generatorUtils;

    @Mock
    private TestMethodInputParamsGenerator inputParamsGenerator;

    @Mock
    private TestMethodDepsConfigurer depsConfigurer;

    private UtMethodGenerator utMethodGenerator;

    @BeforeEach
    void setUp() {
        this.utMethodGenerator = new UtMethodGenerator(this.generatorUtils, this.inputParamsGenerator, this.depsConfigurer);
    }

    // ── initEmptyMethod ───────────────────────────────────────────────────────

    @Test
    void initEmptyMethod_createsVoidMethodWithTestAnnotation() {
        final MethodDeclaration origin = StaticJavaParser.parse(
                        "class Foo { public String bar() {} }")
                .getType(0).getMethods().get(0);
        final CompilationUnit utCu = new CompilationUnit();

        final MethodDeclaration result = this.utMethodGenerator.initEmptyMethod(origin, utCu);

        assertThat(result.getNameAsString()).isEqualTo("bar");
        assertThat(result.getType()).isInstanceOf(VoidType.class);
        assertThat(result.isAnnotationPresent("Test")).isTrue();
    }

    @Test
    void initEmptyMethod_hasNoModifiers() {
        final MethodDeclaration origin = StaticJavaParser.parse(
                        "class Foo { public void doWork() {} }")
                .getType(0).getMethods().get(0);
        final CompilationUnit utCu = new CompilationUnit();

        final MethodDeclaration result = this.utMethodGenerator.initEmptyMethod(origin, utCu);

        assertThat(result.getModifiers()).isEmpty();
    }

    @Test
    void initEmptyMethod_propagatesThrownExceptions() {
        final MethodDeclaration origin = StaticJavaParser.parse(
                        "class Foo { public void doWork() throws Exception {} }")
                .getType(0).getMethods().get(0);
        final CompilationUnit utCu = new CompilationUnit();

        final MethodDeclaration result = this.utMethodGenerator.initEmptyMethod(origin, utCu);

        assertThat(result.getThrownExceptions()).hasSize(1);
        assertThat(result.getThrownExceptions().get(0).asString()).isEqualTo("Exception");
    }

    @Test
    void initEmptyMethod_noExceptionsWhenOriginHasNone() {
        final MethodDeclaration origin = StaticJavaParser.parse(
                        "class Foo { public void doWork() {} }")
                .getType(0).getMethods().get(0);
        final CompilationUnit utCu = new CompilationUnit();

        final MethodDeclaration result = this.utMethodGenerator.initEmptyMethod(origin, utCu);

        assertThat(result.getThrownExceptions()).isEmpty();
    }

    // ── addMethodCall ─────────────────────────────────────────────────────────

    @Test
    void addMethodCall_wrapsNonVoidResultInAssertNotNull() {
        final CompilationUnit cu = StaticJavaParser.parse(
                "package com.example; class MyService { public String doWork() {} }");
        final MethodDeclaration origin = cu.getType(0).getMethods().get(0);
        final BlockStmt block = new BlockStmt();
        final CompilationUnit utCu = new CompilationUnit();

        this.utMethodGenerator.addMethodCall(cu, origin, block, new NodeList<>(), utCu);

        final String body = block.toString();
        assertThat(body).contains("assertNotNull");
        assertThat(body).contains("myService.doWork()");
    }

    @Test
    void addMethodCall_doesNotWrapVoidReturn() {
        final CompilationUnit cu = StaticJavaParser.parse(
                "package com.example; class MyService { public void doWork() {} }");
        final MethodDeclaration origin = cu.getType(0).getMethods().get(0);
        final BlockStmt block = new BlockStmt();
        final CompilationUnit utCu = new CompilationUnit();

        this.utMethodGenerator.addMethodCall(cu, origin, block, new NodeList<>(), utCu);

        final String body = block.toString();
        assertThat(body).doesNotContain("assertNotNull");
        assertThat(body).contains("myService.doWork()");
    }

    @Test
    void addMethodCall_usesLowercaseInstanceName() {
        final CompilationUnit cu = StaticJavaParser.parse(
                "package com.example; class OrderService { public String place() {} }");
        final MethodDeclaration origin = cu.getType(0).getMethods().get(0);
        final BlockStmt block = new BlockStmt();
        final CompilationUnit utCu = new CompilationUnit();

        this.utMethodGenerator.addMethodCall(cu, origin, block, new NodeList<>(), utCu);

        assertThat(block.toString()).contains("orderService.place()");
    }
}
