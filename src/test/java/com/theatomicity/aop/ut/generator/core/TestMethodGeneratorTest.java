package com.theatomicity.aop.ut.generator.core;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.VoidType;
import com.theatomicity.aop.ut.generator.utils.GeneratorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TestMethodGeneratorTest {

    // package-private methods initEmptyMethod() and addMethodCall() are
    // accessible here because this test lives in the same package.

    @Spy
    private GeneratorUtils generatorUtils;

    @Mock
    private TestMethodInputParamsGenerator inputParamsGenerator;

    @Mock
    private TestMethodDepsConfigurer depsConfigurer;

    private TestMethodGenerator testMethodGenerator;

    @BeforeEach
    void setUp() {
        testMethodGenerator = new TestMethodGenerator(generatorUtils, inputParamsGenerator, depsConfigurer);
    }

    // ── initEmptyMethod ───────────────────────────────────────────────────────

    @Test
    void initEmptyMethod_createsVoidMethodWithTestAnnotation() {
        MethodDeclaration origin = StaticJavaParser.parse(
                "class Foo { public String bar() {} }")
                .getType(0).getMethods().get(0);

        MethodDeclaration result = testMethodGenerator.initEmptyMethod(origin);

        assertThat(result.getNameAsString()).isEqualTo("bar");
        assertThat(result.getType()).isInstanceOf(VoidType.class);
        assertThat(result.isAnnotationPresent("Test")).isTrue();
    }

    @Test
    void initEmptyMethod_hasNoModifiers() {
        MethodDeclaration origin = StaticJavaParser.parse(
                "class Foo { public void doWork() {} }")
                .getType(0).getMethods().get(0);

        MethodDeclaration result = testMethodGenerator.initEmptyMethod(origin);

        assertThat(result.getModifiers()).isEmpty();
    }

    @Test
    void initEmptyMethod_propagatesThrownExceptions() {
        MethodDeclaration origin = StaticJavaParser.parse(
                "class Foo { public void doWork() throws Exception {} }")
                .getType(0).getMethods().get(0);

        MethodDeclaration result = testMethodGenerator.initEmptyMethod(origin);

        assertThat(result.getThrownExceptions()).hasSize(1);
        assertThat(result.getThrownExceptions().get(0).asString()).isEqualTo("Exception");
    }

    @Test
    void initEmptyMethod_noExceptionsWhenOriginHasNone() {
        MethodDeclaration origin = StaticJavaParser.parse(
                "class Foo { public void doWork() {} }")
                .getType(0).getMethods().get(0);

        MethodDeclaration result = testMethodGenerator.initEmptyMethod(origin);

        assertThat(result.getThrownExceptions()).isEmpty();
    }

    // ── addMethodCall ─────────────────────────────────────────────────────────

    @Test
    void addMethodCall_wrapsNonVoidResultInAssertNotNull() {
        CompilationUnit cu = StaticJavaParser.parse(
                "package com.example; class MyService { public String doWork() {} }");
        MethodDeclaration origin = cu.getType(0).getMethods().get(0);
        BlockStmt block = new BlockStmt();

        testMethodGenerator.addMethodCall(cu, origin, block, new NodeList<>());

        String body = block.toString();
        assertThat(body).contains("assertNotNull");
        assertThat(body).contains("myService.doWork()");
    }

    @Test
    void addMethodCall_doesNotWrapVoidReturn() {
        CompilationUnit cu = StaticJavaParser.parse(
                "package com.example; class MyService { public void doWork() {} }");
        MethodDeclaration origin = cu.getType(0).getMethods().get(0);
        BlockStmt block = new BlockStmt();

        testMethodGenerator.addMethodCall(cu, origin, block, new NodeList<>());

        String body = block.toString();
        assertThat(body).doesNotContain("assertNotNull");
        assertThat(body).contains("myService.doWork()");
    }

    @Test
    void addMethodCall_usesLowercaseInstanceName() {
        CompilationUnit cu = StaticJavaParser.parse(
                "package com.example; class OrderService { public String place() {} }");
        MethodDeclaration origin = cu.getType(0).getMethods().get(0);
        BlockStmt block = new BlockStmt();

        testMethodGenerator.addMethodCall(cu, origin, block, new NodeList<>());

        assertThat(block.toString()).contains("orderService.place()");
    }
}
