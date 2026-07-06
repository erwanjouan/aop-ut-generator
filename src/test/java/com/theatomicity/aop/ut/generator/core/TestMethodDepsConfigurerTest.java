package com.theatomicity.aop.ut.generator.core;

import com.github.javaparser.ast.CompilationUnit;
import com.theatomicity.aop.ut.generator.cache.MethodExecutionCache;
import com.theatomicity.aop.ut.generator.core.method.TestMethodDepsConfigurer;
import com.theatomicity.aop.ut.generator.model.InterceptedParam;
import com.theatomicity.aop.ut.generator.model.MethodExecution;
import com.theatomicity.aop.ut.generator.utils.GeneratorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;

@ExtendWith(MockitoExtension.class)
class TestMethodDepsConfigurerTest {

    @Mock
    private MethodExecutionCache cache;

    @Mock
    private GeneratorUtils generatorUtils;

    private TestMethodDepsConfigurer configurer;

    @BeforeEach
    void setUp() {
        this.configurer = new TestMethodDepsConfigurer(this.cache, this.generatorUtils);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private MethodExecution withResult(final Class<?> type, final Object value) {
        final MethodExecution exec = new MethodExecution();
        exec.setMethodName("repoMethod");
        exec.setClassName("com.example.Repo");
        exec.setSimpleClassName("Repo");
        exec.setInputParams(List.of());
        exec.setStartTime(100L);
        exec.setEndTime(200L);
        exec.setHashCode(1);
        exec.setResult(new InterceptedParam("result", type, value));
        return exec;
    }

    private MethodExecution withNoResult() {
        final MethodExecution exec = new MethodExecution();
        exec.setMethodName("repoMethod");
        exec.setClassName("com.example.Repo");
        exec.setSimpleClassName("Repo");
        exec.setInputParams(List.of());
        exec.setStartTime(100L);
        exec.setEndTime(200L);
        exec.setHashCode(1);
        return exec;
    }

    // ── null / void ───────────────────────────────────────────────────────────

    @Test
    void nullResult_returnsNull() {
        final String result = this.configurer.getDependencyResult(this.withNoResult(), new CompilationUnit());
        assertThat(result).isNull();
    }

    @Test
    void nullResult_doesNotAddImport() {
        final CompilationUnit cu = new CompilationUnit();
        this.configurer.getDependencyResult(this.withNoResult(), cu);
        assertThat(cu.getImports()).isEmpty();
    }

    // ── String ────────────────────────────────────────────────────────────────

    @Test
    void stringResult_returnsDoubleQuotedValue() {
        final String result = this.configurer.getDependencyResult(this.withResult(String.class, "hello"), new CompilationUnit());
        assertThat(result).isEqualTo("\"hello\"");
    }

    // ── Long ──────────────────────────────────────────────────────────────────

    @Test
    void longResult_returnsValueWithLSuffix() {
        final String result = this.configurer.getDependencyResult(this.withResult(Long.class, 42L), new CompilationUnit());
        assertThat(result).isEqualTo("42L");
    }

    // ── Optional ──────────────────────────────────────────────────────────────

    @Test
    void optionalEmpty_returnsOptionalEmptyExpression() {
        final String result = this.configurer.getDependencyResult(
                this.withResult(Optional.class, Optional.empty()), new CompilationUnit());
        assertThat(result).isEqualTo("Optional.empty()");
    }

    @Test
    void optionalWithValue_returnsMockWrappedInOptionalOf() {
        final String result = this.configurer.getDependencyResult(
                this.withResult(Optional.class, Optional.of("someEntity")), new CompilationUnit());
        assertThat(result).isEqualTo("Optional.of(mock(String.class))");
    }

    @Test
    void optionalResult_addsImport() {
        final CompilationUnit cu = new CompilationUnit();
        doCallRealMethod().when(this.generatorUtils).addImportIfNotExists(eq(cu), anyString(), anyBoolean(), anyBoolean());
        this.configurer.getDependencyResult(this.withResult(Optional.class, Optional.empty()), cu);
        assertThat(cu.getImports().toString()).contains("java.util.Optional");
    }

    @Test
    void optionalResult_addsOptionalBigDecimal() {
        final CompilationUnit cu = new CompilationUnit();
        doCallRealMethod().when(this.generatorUtils).addImportIfNotExists(eq(cu), anyString(), anyBoolean(), anyBoolean());
        final String dependencyResult = this.configurer.getDependencyResult(this.withResult(Optional.class, Optional.of(BigDecimal.ONE)), cu);
        assertThat(cu.getImports().toString()).contains("java.util.Optional");
        assertThat(dependencyResult).isEqualTo("Optional.of(mock(BigDecimal.class))");
    }

    // ── Custom / reference type ───────────────────────────────────────────────

    @Test
    void customType_returnsMockExpressionWithSimpleName() {
        final CompilationUnit cu = new CompilationUnit();
        doCallRealMethod().when(this.generatorUtils).addImportIfNotExists(eq(cu), anyString(), anyBoolean(), anyBoolean());
        doCallRealMethod().when(this.generatorUtils).addInlinedMock(eq(cu), anyString());
        final String result = this.configurer.getDependencyResult(
                this.withResult(ArrayList.class, new ArrayList<>()), cu);
        assertThat(result).isEqualTo("mock(ArrayList.class)");
    }

    @Test
    void customType_addsFullyQualifiedImport() {
        final CompilationUnit cu = new CompilationUnit();
        doCallRealMethod().when(this.generatorUtils).addImportIfNotExists(eq(cu), anyString(), anyBoolean(), anyBoolean());
        doCallRealMethod().when(this.generatorUtils).addInlinedMock(eq(cu), anyString());
        this.configurer.getDependencyResult(this.withResult(ArrayList.class, new ArrayList<>()), cu);
        assertThat(cu.getImports().toString()).contains("java.util.ArrayList");
    }

    @Test
    void customType_processStringList() {
        final CompilationUnit cu = new CompilationUnit();
        final String result = this.configurer.getDependencyResult(this.withResult(List.class, List.of("")), cu);
        assertThat(result).isEqualTo("List.of(\"\")");
    }

    @Test
    void customType_nonPrimitive() {
        final CompilationUnit cu = new CompilationUnit();
        doCallRealMethod().when(this.generatorUtils).addImportIfNotExists(eq(cu), anyString(), anyBoolean(), anyBoolean());
        doCallRealMethod().when(this.generatorUtils).addInlinedMock(eq(cu), anyString());
        final String dependencyResult = this.configurer.getDependencyResult(this.withResult(BigDecimal.class, BigDecimal.valueOf(1)), cu);
        assertThat(cu.getImports().toString()).contains("java.math.BigDecimal");
        assertThat(dependencyResult).isEqualTo("mock(BigDecimal.class)");
    }

    @Test
    void customType_processBigDecimalList() {
        final CompilationUnit cu = new CompilationUnit();
        final BigDecimal elt = BigDecimal.valueOf(1L);
        doCallRealMethod().when(this.generatorUtils).addImportIfNotExists(eq(cu), anyString(), anyBoolean(), anyBoolean());
        doCallRealMethod().when(this.generatorUtils).addInlinedMock(eq(cu), anyString());
        final String result = this.configurer.getDependencyResult(this.withResult(List.class, List.of(elt)), cu);
        assertThat(result).isEqualTo("List.of(mock(BigDecimal.class))");
    }
}
