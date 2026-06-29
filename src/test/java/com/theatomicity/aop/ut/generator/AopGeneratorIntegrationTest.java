package com.theatomicity.aop.ut.generator;

import com.theatomicity.aop.ut.generator.cache.MethodExecutionCache;
import com.theatomicity.aop.ut.generator.sample.service.SampleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class AopGeneratorIntegrationTest {

    @Autowired
    private SampleService sampleService;

    @Autowired
    private MethodExecutionCache methodExecutionCache;

    private static final Path GENERATED_FILE = Path.of(
            "src/test/java/com/theatomicity/aop/ut/generator/sample/service/SampleServiceAutoTest.java");

    @BeforeEach
    void setUp() throws IOException {
        this.methodExecutionCache.getCache().clear();
        Files.deleteIfExists(GENERATED_FILE);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(GENERATED_FILE);
    }

    @Test
    void doSomething_generatesTestFileWithClassStructure() throws Exception {
        this.sampleService.doSomething();

        assertThat(GENERATED_FILE).exists();
        final String content = Files.readString(GENERATED_FILE);

        assertThat(content).contains("@ExtendWith(MockitoExtension.class)");
        assertThat(content).contains("@Mock");
        assertThat(content).contains("SampleRepository sampleRepository");
        assertThat(content).contains("@Spy");
        assertThat(content).contains("@InjectMocks");
        assertThat(content).contains("SampleService sampleService");
    }

    @Test
    void doSomething_generatesTestMethodThatCallsService() throws Exception {
        this.sampleService.doSomething();

        final String content = Files.readString(GENERATED_FILE);
        assertThat(content).contains("@Test");
        assertThat(content).contains("doSomething_");
        assertThat(content).contains("sampleService.doSomething()");
    }

    @Test
    void doSomethingElse_generatesStubForRepositoryReturnValue() throws Exception {
        this.sampleService.doSomethingElse();

        final String content = Files.readString(GENERATED_FILE);
        assertThat(content).contains("doReturn(1L).when(sampleRepository).returnSomeThing()");
        assertThat(content).contains("assertNotNull(sampleService.doSomethingElse())");
    }

    @Test
    void doSomethingElse_generatesTestMethodThatThrowsException() throws Exception {
        this.sampleService.doSomethingElse();

        final String content = Files.readString(GENERATED_FILE);
        assertThat(content).contains("throws Exception");
        assertThat(content).contains("doSomethingElse_");
    }

    @Test
    void callingBothMethods_generatesTwoDistinctTestMethods() throws Exception {
        this.sampleService.doSomething();
        this.sampleService.doSomethingElse();

        final String content = Files.readString(GENERATED_FILE);
        final long testAnnotationCount = content.lines()
                .filter(line -> line.strip().startsWith("@Test"))
                .count();
        assertThat(testAnnotationCount).isGreaterThanOrEqualTo(2);
        assertThat(content).contains("doSomething_");
        assertThat(content).contains("doSomethingElse_");
    }

    @Test
    void doSomethingWithList_generatesStubForRepositoryReturnValue() throws Exception {
        this.sampleService.doSomethingWithList();

        final String content = Files.readString(GENERATED_FILE);
        assertThat(content).contains("doReturn(List.of(mock(BigDecimal.class))).when(sampleRepository).returnSomeThingWithList()");
        assertThat(content).contains("assertNotNull(sampleService.doSomethingWithList())");
    }

}
