package com.theatomicity.aop.ut.generator.cache;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.theatomicity.aop.ut.generator.model.InterceptedParam;
import com.theatomicity.aop.ut.generator.model.MethodExecution;
import com.theatomicity.aop.ut.generator.utils.GeneratorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MethodExecutionCacheTest {

    private MethodExecutionCache cache;

    @BeforeEach
    void setUp() {
        this.cache = new MethodExecutionCache(new GeneratorUtils());
    }

    private MethodExecution exec(final String className, final String simpleName, final String method, final long start, final long end) {
        final MethodExecution e = new MethodExecution();
        e.setClassName(className);
        e.setSimpleClassName(simpleName);
        e.setMethodName(method);
        e.setInputParams(List.of());
        e.setStartTime(start);
        e.setEndTime(end);
        e.setHashCode(method.hashCode());
        e.setCaller(Optional.empty());
        return e;
    }

    // ── findTimeCompatibleExecutions ─────────────────────────────────────────

    @Test
    void findTimeCompatibleExecutions_returnsCallsWithinTimeWindow() {
        final MethodExecution parent = this.exec("com.example.ServiceA", "ServiceA", "doWork", 100L, 500L);
        final MethodExecution child = this.exec("com.example.RepoB", "RepoB", "findAll", 200L, 300L);
        // caller must point into the body of doWork (line 3 in the multi-line source below)
        child.setCaller(Optional.of(new StackTraceElement("com.example.ServiceA", "doWork", "ServiceA.java", 3)));
        final MethodExecution before = this.exec("com.example.RepoC", "RepoC", "save", 10L, 80L);
        final CompilationUnit cu = StaticJavaParser.parse(
                "class ServiceA {\n  void doWork() {\n    repoB.findAll();\n  }\n}");
        final MethodDeclaration originMethod = cu.getType(0).getMethods().get(0);

        this.cache.add(parent);
        this.cache.add(child);
        this.cache.add(before);

        assertThat(this.cache.findChildExecutions(parent, originMethod)).containsExactly(child);
    }

    @Test
    void findChildExecutions_excludesSameClass() {
        final MethodExecution parent = this.exec("com.example.ServiceA", "ServiceA", "doWork", 100L, 500L);
        final MethodExecution sameClass = this.exec("com.example.ServiceA", "ServiceA", "helper", 200L, 300L);
        final MethodDeclaration originMethod = mock(MethodDeclaration.class);

        this.cache.add(parent);
        this.cache.add(sameClass);

        assertThat(this.cache.findChildExecutions(parent, originMethod)).isEmpty();
    }

    @Test
    void findChildExecutions_excludesSameMethodName() {
        // Mirrors the real-world case: SampleService.doSomething() calls
        // SampleRepository.doSomething() — same name, different class.
        // The same-name filter prevents generating a stub for it.
        final MethodExecution parent = this.exec("com.example.ServiceA", "ServiceA", "doWork", 100L, 500L);
        final MethodExecution sameName = this.exec("com.example.RepositoryB", "RepositoryB", "doWork", 200L, 300L);
        final MethodDeclaration originMethod = mock(MethodDeclaration.class);

        this.cache.add(parent);
        this.cache.add(sameName);

        assertThat(this.cache.findChildExecutions(parent, originMethod)).isEmpty();
    }

    @Test
    void findChildExecutions_excludesCallsOutsideWindow() {
        final MethodExecution parent = this.exec("com.example.ServiceA", "ServiceA", "doWork", 100L, 500L);
        final MethodExecution after = this.exec("com.example.RepoB", "RepoB", "findAll", 600L, 700L);
        final MethodDeclaration originMethod = mock(MethodDeclaration.class);

        this.cache.add(parent);
        this.cache.add(after);

        assertThat(this.cache.findChildExecutions(parent, originMethod)).isEmpty();
    }

    // ── findNameMatchingExecution ─────────────────────────────────────────────

    @Test
    void findNameMatchingExecution_matchesByNameAndFullClass() {
        final CompilationUnit cu = StaticJavaParser.parse(
                "package com.example; class MyService { public void myMethod() {} }");
        final MethodDeclaration method = cu.getType(0).getMethods().get(0);

        final MethodExecution exec = this.exec("com.example.MyService", "MyService", "myMethod", 100L, 200L);
        this.cache.add(exec);

        final Optional<MethodExecution> result = this.cache.findOriginMethodInCache(cu, method);

        assertThat(result).contains(exec);
    }

    @Test
    void findOriginMethodInCache_returnsEmptyWhenNoMatch() {
        final CompilationUnit cu = StaticJavaParser.parse(
                "package com.example; class MyService { public void myMethod() {} }");
        final MethodDeclaration method = cu.getType(0).getMethods().get(0);

        assertThat(this.cache.findOriginMethodInCache(cu, method)).isEmpty();
    }

    @Test
    void findOriginMethodInCache_doesNotMatchWrongClass() {
        final CompilationUnit cu = StaticJavaParser.parse(
                "package com.example; class MyService { public void myMethod() {} }");
        final MethodDeclaration method = cu.getType(0).getMethods().get(0);

        final MethodExecution wrongClass = this.exec("com.example.OtherService", "OtherService", "myMethod", 100L, 200L);
        this.cache.add(wrongClass);

        assertThat(this.cache.findOriginMethodInCache(cu, method)).isEmpty();
    }

    // ── findInputParamValue ───────────────────────────────────────────────────

    @Test
    void findInputParamValue_returnsValueForMatchingParam() {
        final CompilationUnit cu = StaticJavaParser.parse(
                "package com.example; class MyService { public void myMethod(Long id) {} }");
        final MethodDeclaration method = cu.getType(0).getMethods().get(0);
        final Parameter parameter = method.getParameter(0);

        final MethodExecution exec = this.exec("com.example.MyService", "MyService", "myMethod", 100L, 200L);
        exec.setInputParams(List.of(new InterceptedParam("id", Long.class, 42L)));
        this.cache.add(exec);

        assertThat(this.cache.findInputParamValue(cu, method, parameter)).isEqualTo(42L);
    }

    @Test
    void findInputParamValue_returnsNullWhenParamNotFound() {
        final CompilationUnit cu = StaticJavaParser.parse(
                "package com.example; class MyService { public void myMethod(Long id) {} }");
        final MethodDeclaration method = cu.getType(0).getMethods().get(0);
        final Parameter parameter = method.getParameter(0);

        assertThat(this.cache.findInputParamValue(cu, method, parameter)).isNull();
    }
}
