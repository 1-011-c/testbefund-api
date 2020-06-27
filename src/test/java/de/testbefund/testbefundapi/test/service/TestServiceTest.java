package de.testbefund.testbefundapi.test.service;

import de.testbefund.testbefundapi.client.data.Client;
import de.testbefund.testbefundapi.client.data.ClientRepository;
import de.testbefund.testbefundapi.test.data.*;
import de.testbefund.testbefundapi.test.dto.TestResultT;
import de.testbefund.testbefundapi.test.dto.TestToCreate;
import de.testbefund.testbefundapi.test.dto.UpdateTestRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static de.testbefund.testbefundapi.test.dto.TestToCreate.builder;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.MockitoAnnotations.initMocks;

class TestServiceTest {

    private TestService testService;

    @Mock
    private TestContainerRepository testContainerRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private Supplier<LocalDateTime> currentDateSupplier;

    @Mock
    private Supplier<String> idProvider;

    @BeforeEach
    void setUp() {
        initMocks(this);
        testService = new TestService(testContainerRepository, clientRepository, currentDateSupplier, idProvider);
        Mockito.when(testContainerRepository.save(any())).thenAnswer(returnsFirstArg());
    }


    @Test
    void shouldCreateTestContainer_withAllProvidedTests() {
        TestContainer testContainer = testService.createTestContainer(of(
                builder().title("TitleA").build(),
                builder().title("TitleB").build()
        ), null);
        assertThat(testContainer.getTestCases())
                .extracting(TestCase::getTitle)
                .containsExactlyInAnyOrder("TitleA", "TitleB");
    }

    @Test
    void shouldInitializeTests_withCorrectData() {
        TestContainer testContainer = testService.createTestContainer(of(builder().title("TitleA").build()), null);
        assertThat(testContainer.getTestCases()).hasSize(1);
        TestCase testCase = testContainer.getTestCases().iterator().next();
        assertThat(testCase.getId()).isNull(); // Filled in by auto generation
        assertThat(testCase.getCurrentStatus()).isEqualTo(TestStageStatus.ISSUED);
    }


    @Test
    void shouldCreateTest_withICDCode() {
        TestToCreate testToCreate = builder().title("Title").icdCode("icd1234").build();
        TestContainer testContainer = testService.createTestContainer(of(testToCreate), null);
        assertThat(testContainer.getTestCases())
                .extracting(TestCase::getIcdCode)
                .containsExactly("icd1234");
    }

    @Test
    void shouldPersistTestContainer() {
        TestContainer testContainer = testService.createTestContainer(of(builder().title("TitleA").build()), null);
        Mockito.verify(testContainerRepository, times(1)).save(testContainer);
    }

    @Test
    void shouldCreateTests_withCurrentTimeStamp() {
        LocalDateTime currentDate = LocalDateTime.now();
        Mockito.when(currentDateSupplier.get()).thenReturn(currentDate);
        TestContainer testContainer = testService.createTestContainer(of(builder().title("TitleA").build()), null);
        assertThat(testContainer.getTestCases())
                .extracting(TestCase::getLastChangeDate)
                .containsExactly(currentDate);
    }

    private TestCase withPersistentTestCase(String writeId) {
        TestCase testCase = TestCase.builder()
                .title("SARS-CoV2")
                .id("1234")
                .icdCode("1234")
                .lastChangeDate(LocalDateTime.now())
                .currentStatus(TestStageStatus.ISSUED)
                .build();
        TestContainer testContainer = TestContainer.builder()
                .testCases(List.of(testCase))
                .writeId(writeId)
                .build();
        Mockito.when(testContainerRepository.findByWriteId(testContainer.getWriteId())).thenReturn(Optional.of(testContainer));
        return testCase;
    }

    @Test
    void whenUpdatingTest_shouldUpdateTimeStamp() {
        LocalDateTime currentDate = LocalDateTime.now();
        Mockito.when(currentDateSupplier.get()).thenReturn(currentDate);
        TestCase testCase = withPersistentTestCase("ABCD");
        testService.updateTestByWriteId("ABCD", testCase.getId(), TestResultT.NEGATIVE);
        assertThat(testCase.getLastChangeDate()).isEqualTo(currentDate);
    }

    @Test
    void whenUpdatingTest_shouldSetCorrectStatusForNegative() {
        TestCase testCase = withPersistentTestCase("ABCDE");
        testService.updateTestByWriteId("ABCDE", testCase.getId(), TestResultT.NEGATIVE);
        assertThat(testCase.getCurrentStatus()).isEqualTo(TestStageStatus.CONFIRM_NEGATIVE);
    }

    @Test
    void whenUpdatingTest_shouldSetCorrectStatusForPositive() {
        TestCase testCase = withPersistentTestCase("ABCDE");
        testService.updateTestByWriteId("ABCDE", testCase.getId(), TestResultT.POSITIVE);
        assertThat(testCase.getCurrentStatus()).isEqualTo(TestStageStatus.CONFIRM_POSITIVE);
    }

    @Test
    void shouldUseIdProvider() {
        Mockito.when(idProvider.get()).thenReturn("ABCDE");
        TestContainer testContainer = testService.createTestContainer(of(builder().title("TitleA").build()), null);
        assertThat(testContainer.getReadId()).isEqualTo("ABCDE");
    }

    @Test
    void shouldAttachWriteId_toContainer() {
        Mockito.when(idProvider.get()).thenReturn("ABCDE");
        TestContainer testContainer = testService.createTestContainer(of(builder().title("TitleA").build()), null);
        assertThat(testContainer.getWriteId()).isEqualTo("ABCDE");
    }

    @Test
    void shouldBatchUpdate() {
        // Title is SARS-CoV2
        TestCase testCase = withPersistentTestCase("ABCDE");
        UpdateTestRequest request = new UpdateTestRequest();
        request.writeId = "ABCDE";
        request.tests = List.of(UpdateTestRequest.SingleTest.builder().title(testCase.getTitle()).testResult(TestResultT.NEGATIVE).build());
        TestContainer result = testService.updateTestContainer(request);
        assertThat(result.getTestCases())
                .extracting(TestCase::getCurrentStatus)
                .containsExactly(TestStageStatus.CONFIRM_NEGATIVE);
    }

    @Test
    void shouldCreateContainerWithClient() {
        Client client = Client.builder().id("12345-abcdef").name("Testorganization").build();
        Mockito.when(clientRepository.findById(client.getId())).thenReturn(Optional.of(client));
        TestContainer testContainer = testService.createTestContainer(of(builder().title("TitleA").build()), client.getId());
        assertThat(testContainer.getClient()).isEqualTo(client);
    }
}