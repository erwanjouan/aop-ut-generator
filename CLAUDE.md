# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./mvnw clean install

# Run tests
./mvnw test

# Run a single test
./mvnw test -Dtest=SampleApplicationTests

# Run the application
./mvnw spring-boot:run
```

## Architecture

This is a Spring Boot library that **generates Mockito unit tests at runtime** by intercepting method executions via AOP and writing `*AutoTest.java` files into `src/test/java/`.

### How it works

1. **Interception** — `EntryPointAroundPointcut` (an `@Aspect`) intercepts two kinds of method executions:
   - Methods annotated with `@GenerateMockitoUt`
   - All calls to Spring `CrudRepository` subtypes (via a pointcut)

   `UtGenerateInterceptor` is a second aspect that intercepts all methods in the `sample` package and delegates to `EntryPointAroundPointcut`.

2. **Caching** — Every intercepted execution is stored in `MethodExecutionCache` (an in-memory `List<MethodExecution>`). The cache records input param types/values, return value, and timing (`startTime`/`endTime` in millis).

3. **Test generation** — For each new (non-repository) method execution, `TestClassGenerator` is called:
   - Parses the source file from `src/main/java/` using JavaParser
   - Creates or updates a `*AutoTest.java` file in `src/test/java/` (same package)
   - Delegates to `TestMethodGenerator` to build the test method body

4. **Test method body construction** (`TestMethodGenerator`):
   - `TestMethodInputParamsGenerator` — declares local variables for each method parameter, using actual intercepted values for primitives/boxed types, `mock(Type.class)` for reference types
   - `TestMethodDepsConfigurer` — stubs dependencies by finding "time-compatible" executions (dependency calls that happened *within* the time window of the parent call), then emits `doReturn(...).when(dep).method(any())` or `doNothing().when(dep).method(any())`
   - The method call itself is added, wrapped in `assertNotNull(...)` for non-void return types

### Key data flow

```
AOP intercept → MethodExecution.from(joinPoint) → MethodExecutionCache.add()
                                                          ↓
                                               TestClassGenerator.generateUnitTest()
                                                          ↓
                                    JavaParser: parse source → build test CompilationUnit
                                                          ↓
                                        write src/test/java/.../*AutoTest.java
```

### Auto-configuration

`AopUtGeneratorConfig` is registered as a Spring Boot auto-configuration via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, so the library activates automatically when on the classpath. It `@ComponentScan`s the `core` package.

### Dependency correlation

`MethodExecutionCache.findTimeCompatibleExecutions()` identifies which dependency calls belong to a given parent call by comparing timestamps: a dependency call is "compatible" if its `startTime >= parent.startTime` and `endTime <= parent.endTime` and it's from a different class.

### Generated test naming

Test methods are named `{methodName}_{joinPointHashCode}` to avoid collisions when the same method is called multiple times. The generated class is `{OriginClass}AutoTest`.

### Known limitations / TODOs in the code

- `normalizeArg()` always emits `any()` regardless of argument type (primitive case is a TODO)
- Not all boxed types are handled in `TestMethodInputParamsGenerator` (only `Long` and `Integer`)
- Repository calls are excluded from test generation (`isRepository` check in `EntryPointAroundPointcut`)
