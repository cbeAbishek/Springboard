package com.example.automatedtestingframework.service;

import com.example.automatedtestingframework.model.Report;
import com.example.automatedtestingframework.model.TestCase;
import com.example.automatedtestingframework.repository.ReportRepository;
import com.example.automatedtestingframework.repository.TestCaseRepository;
import com.example.automatedtestingframework.util.JsonParserUtil;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.Map;

@Service
public class ApiTestExecutor {

    private static final Logger log = LoggerFactory.getLogger(ApiTestExecutor.class);

    private final JsonParserUtil jsonParserUtil;
    private final ReportRepository reportRepository;
    private final TestCaseRepository testCaseRepository;

    public ApiTestExecutor(JsonParserUtil jsonParserUtil,
                           ReportRepository reportRepository,
                           TestCaseRepository testCaseRepository) {
        this.jsonParserUtil = jsonParserUtil;
        this.reportRepository = reportRepository;
        this.testCaseRepository = testCaseRepository;
    }

    public Report execute(TestCase testCase) {
        JsonNode definition = jsonParserUtil.parse(testCase.getDefinitionJson());
        HttpClient client = HttpClient.newBuilder().build();
        OffsetDateTime start = OffsetDateTime.now();
        StringBuilder detailsBuilder = new StringBuilder();
        String status = "PASSED";
        Integer lastResponseCode = null;
        String lastErrorMessage = null;
        try {
            Iterator<JsonNode> iterator = definition.withArray("requests").elements();
            while (iterator.hasNext()) {
                JsonNode requestNode = iterator.next();
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(requestNode.path("url").asText()))
                    .method(requestNode.path("method").asText("GET"), buildBodyPublisher(requestNode));

                JsonNode headers = requestNode.path("headers");
                if (headers.isObject()) {
                    for (Iterator<Map.Entry<String, JsonNode>> it = headers.fields(); it.hasNext();) {
                        Map.Entry<String, JsonNode> entry = it.next();
                        requestBuilder.header(entry.getKey(), entry.getValue().asText());
                    }
                }

                HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
                lastResponseCode = response.statusCode();
                detailsBuilder.append("Request: ").append(requestNode.path("name").asText("Unnamed"))
                    .append(" -> ").append(response.statusCode()).append('\n');

                int expectedStatus = requestNode.path("expectedStatus").asInt(200);
                if (response.statusCode() != expectedStatus) {
                    status = "FAILED";
                    lastErrorMessage = "Expected status %d but got %d".formatted(expectedStatus, response.statusCode());
                    break;
                }

                JsonNode contains = requestNode.path("expectContains");
                if (contains.isArray()) {
                    for (JsonNode node : contains) {
                        if (!response.body().contains(node.asText())) {
                            status = "FAILED";
                            lastErrorMessage = "Response missing text: " + node.asText();
                            break;
                        }
                    }
                    if (!"PASSED".equals(status)) {
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            status = "FAILED";
            lastErrorMessage = ex.getMessage();
            log.error("API test execution failed", ex);
        }

        Report report = new Report();
        report.setProject(testCase.getProject());
        report.setTestCase(testCase);
        report.setStartedAt(start);
        report.setCompletedAt(OffsetDateTime.now());
        report.setStatus(status);
        report.setDetails(detailsBuilder.toString());
        report.setSummary("API test execution for %s".formatted(testCase.getName()));
        report.setErrorMessage(lastErrorMessage);
        report.setResponseCode(lastResponseCode);

        Report saved = reportRepository.save(report);
        updateTestCaseLastRun(testCase, status, lastErrorMessage, lastResponseCode);
        return saved;
    }

    private HttpRequest.BodyPublisher buildBodyPublisher(JsonNode requestNode) {
        JsonNode body = requestNode.path("body");
        if (body.isMissingNode() || body.isNull()) {
            return HttpRequest.BodyPublishers.noBody();
        }
        return HttpRequest.BodyPublishers.ofString(body.asText(), StandardCharsets.UTF_8);
    }

    private void updateTestCaseLastRun(TestCase testCase, String status, String error, Integer responseCode) {
        testCase.setLastRunAt(OffsetDateTime.now());
        testCase.setLastRunStatus(status);
        testCase.setLastErrorMessage(error);
        testCase.setLastResponseCode(responseCode);
        testCaseRepository.save(testCase);
    }
}
