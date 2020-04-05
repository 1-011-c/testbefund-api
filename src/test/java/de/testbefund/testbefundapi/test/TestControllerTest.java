package de.testbefund.testbefundapi.test;

import de.testbefund.testbefundapi.test.data.TestCase;
import de.testbefund.testbefundapi.test.data.TestContainer;
import de.testbefund.testbefundapi.test.data.TestResult;
import de.testbefund.testbefundapi.test.requests.CreateTestContainerRequest;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TestControllerTest {

    private RestTemplate restTemplate = new RestTemplate();

    @LocalServerPort
    private int port;

    private String baseUri() {
        return "http://localhost:" + port;
    }

    private ResponseEntity<TestContainer> createSampleContainer() {
        CreateTestContainerRequest createTestContainerRequest = new CreateTestContainerRequest();
        createTestContainerRequest.titles = List.of("Test");
        return restTemplate.postForEntity(baseUri() + "/container", createTestContainerRequest, TestContainer.class);
    }

    private TestContainer getContainerByReadId(String readId) {
        return restTemplate.getForEntity(baseUri() + "/container/" + readId, TestContainer.class).getBody();
    }

    @Test
    void shouldCreateTestContainer() {
        ResponseEntity<TestContainer> response = createSampleContainer();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void shouldGetTheTestContainer_byReadId() {
        TestContainer container = createSampleContainer().getBody();
        assertThat(container).isNotNull();
        TestContainer readContainer = getContainerByReadId(container.getReadId());
        assertThat(container).isEqualToIgnoringGivenFields(readContainer, "date");
    }

    @Test
    void shouldUpdateContainer() {
        TestContainer container = createSampleContainer().getBody();
        assertThat(container).isNotNull();
        String uri = String.format("/testcase/%s/%s", container.getTestCases().iterator().next().getWriteId(), TestResult.NEGATIVE);
        ResponseEntity<String> response = restTemplate.postForEntity(baseUri() + uri, null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        TestContainer readContainer = getContainerByReadId(container.getReadId());
        assertThat(readContainer.getTestCases())
                .extracting(TestCase::getTitle, TestCase::getResult)
                .containsExactly(Tuple.tuple("Test", TestResult.NEGATIVE));
    }

    @Test
    void shouldReturnNotFound_whenTryingToUpdateNonExistingContainer() {
        TestContainer container = createSampleContainer().getBody();
        assertThat(container).isNotNull();
        String uri = String.format("/testcase/%s/%s", "FOOBAR", TestResult.NEGATIVE);
        assertThatThrownBy(() -> restTemplate.postForEntity(baseUri() + uri, null, String.class))
                .isInstanceOf(HttpClientErrorException.class)
                .hasMessageStartingWith("404");
    }
}
