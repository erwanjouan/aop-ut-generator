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

class MethodExecutionCacheTest {

    private MethodExecutionCache cache;

    @BeforeEach
    void setUp() {
        cache = new MethodExecutionCache(new GeneratorUtils());
    }

    private MethodExecution exec(String className, String simpleName, String method, long start, long end) {
        MethodExecution e = new MethodExecution();
        e.setClassName(className);
        e.setSimpleClassName(simpleName);
        e.setName(method);
        e.setInputParams(List.of());
        e.setStartTime(start);
        e.setEndTime(end);
        e.setHashCode(method.hashCode());
        return e;
    }

    // ── findTimeCompatibleExecutions ─────────────────────────────────────────

    @Test
    void findTimeCompatibleExecutions_returnsCallsWithinTimeWindow() {
        MethodExecution parent = exec("com.example.ServiceA", "ServiceA", "doWork",  100L, 500L);
        MethodExecution child  = exec("com.example.RepoB",    "RepoB",    "findAll", 200L, 300L);
        MethodExecution before = exec("com.example.RepoC",    "RepoC",    "save",    10L,  80L);

        cache.add(parent);
        cache.add(child);
        cache.add(before);

        assertThat(cache.findTimeCompatibleExecutions(parent)).containsExactly(child);
    }

    @Test
    void findTimeCompatibleExecutions_excludesSameClass() {
        MethodExecution parent    = exec("com.example.ServiceA", "ServiceA", "doWork",  100L, 500L);
        MethodExecution sameClass = exec("com.example.ServiceA", "ServiceA", "helper",  200L, 300L);

        cache.add(parent);
        cache.add(sameClass);

        assertThat(cache.findTimeCompatibleExecutions(parent)).isEmpty();
    }

    @Test
    void findTimeCompatibleExecutions_excludesSameMethodName() {
        // Mirrors the real-world case: SampleService.doSomething() calls
        // SampleRepository.doSomething() — same name, different class.
        // The same-name filter prevents generating a stub for it.
        MethodExecution parent   = exec("com.example.ServiceA",    "ServiceA",    "doWork", 100L, 500L);
        MethodExecution sameName = exec("com.example.RepositoryB", "RepositoryB", "doWork", 200L, 300L);

        cache.add(parent);
        cache.add(sameName);

        assertThat(cache.findTimeCompatibleExecutions(parent)).isEmpty();
    }

    @Test
    void findTimeCompatibleExecutions_excludesCallsOutsideWindow() {
        MethodExecution parent = exec("com.example.ServiceA", "ServiceA", "doWork",  100L, 500L);
        MethodExecution after  = exec("com.example.RepoB",    "RepoB",    "findAll", 600L, 700L);

        cache.add(parent);
        cache.add(after);

        assertThat(cache.findTimeCompatibleExecutions(parent)).isEmpty();
    }

    // ── findNameMatchingExecution ─────────────────────────────────────────────

    @Test
    void findNameMatchingExecution_matchesByNameAndFullClass() {
        CompilationUnit cu = StaticJavaParser.parse(
                "package com.example; class MyService { public void myMethod() {} }");
        MethodDeclaration method = cu.getType(0).getMethods().get(0);

        MethodExecution exec = exec("com.example.MyService", "MyService", "myMethod", 100L, 200L);
        cache.add(exec);

        Optional<MethodExecution> result = cache.findNameMatchingExecution(cu, method);

        assertThat(result).contains(exec);
    }

    @Test
    void findNameMatchingExecution_returnsEmptyWhenNoMatch() {
        CompilationUnit cu = StaticJavaParser.parse(
                "package com.example; class MyService { public void myMethod() {} }");
        MethodDeclaration method = cu.getType(0).getMethods().get(0);

        assertThat(cache.findNameMatchingExecution(cu, method)).isEmpty();
    }

    @Test
    void findNameMatchingExecution_doesNotMatchWrongClass() {
        CompilationUnit cu = StaticJavaParser.parse(
                "package com.example; class MyService { public void myMethod() {} }");
        MethodDeclaration method = cu.getType(0).getMethods().get(0);

        MethodExecution wrongClass = exec("com.example.OtherService", "OtherService", "myMethod", 100L, 200L);
        cache.add(wrongClass);

        assertThat(cache.findNameMatchingExecution(cu, method)).isEmpty();
    }

    // ── findInputParamValue ───────────────────────────────────────────────────

    @Test
    void findInputParamValue_returnsValueForMatchingParam() {
        CompilationUnit cu = StaticJavaParser.parse(
                "package com.example; class MyService { public void myMethod(Long id) {} }");
        MethodDeclaration method = cu.getType(0).getMethods().get(0);
        Parameter parameter = method.getParameter(0);

        MethodExecution exec = exec("com.example.MyService", "MyService", "myMethod", 100L, 200L);
        exec.setInputParams(List.of(new InterceptedParam("id", Long.class, 42L)));
        cache.add(exec);

        assertThat(cache.findInputParamValue(cu, method, parameter)).isEqualTo(42L);
    }

    @Test
    void findInputParamValue_returnsNullWhenParamNotFound() {
        CompilationUnit cu = StaticJavaParser.parse(
                "package com.example; class MyService { public void myMethod(Long id) {} }");
        MethodDeclaration method = cu.getType(0).getMethods().get(0);
        Parameter parameter = method.getParameter(0);

        assertThat(cache.findInputParamValue(cu, method, parameter)).isNull();
    }
}
