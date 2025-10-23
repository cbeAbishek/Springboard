package com.example.automatedtestingframework.service;

import com.example.automatedtestingframework.analysis.EndpointAnalysisPayload;
import com.example.automatedtestingframework.model.EndpointAnalysisResult;
import com.example.automatedtestingframework.model.EndpointAnalysisStatus;
import com.example.automatedtestingframework.model.Project;
import com.example.automatedtestingframework.repository.EndpointAnalysisResultRepository;
import com.example.automatedtestingframework.util.JsonParserUtil;
import io.github.bonigarcia.wdm.WebDriverManager;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.proxy.CaptureType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class EndpointAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(EndpointAnalysisService.class);
    private static final String USER_AGENT = "AutomationPlatformBot/1.0";
    private static final String[] COMMON_RESOURCES = {"/sitemap.xml", "/robots.txt", "/swagger.json", "/openapi.json"};

    private final EndpointAnalysisResultRepository repository;
    private final JsonParserUtil jsonParserUtil;
    private final HttpClient httpClient;

    public EndpointAnalysisService(EndpointAnalysisResultRepository repository, JsonParserUtil jsonParserUtil) {
        this.repository = repository;
        this.jsonParserUtil = jsonParserUtil;
        this.httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public EndpointAnalysisResult performAnalysis(Project project, String inputDomain) {
        Objects.requireNonNull(project, "project must not be null");
        if (inputDomain == null || inputDomain.isBlank()) {
            throw new IllegalArgumentException("Domain URL is required");
        }

        DomainContext domain = normaliseDomain(inputDomain.trim());
        logger.info("Starting endpoint analysis for project {} on {}", project.getId(), domain.displayUrl());

        List<String> issues = new ArrayList<>();
        List<EndpointAnalysisPayload.FileResourceFinding> fileFindings = new ArrayList<>();
        List<EndpointAnalysisPayload.HtmlEndpointFinding> htmlFindings = new ArrayList<>();
        List<EndpointAnalysisPayload.NetworkRequestFinding> networkFindings = new ArrayList<>();

        // File-based analysis
        try {
            fileFindings = analyseCommonResources(domain.rootUrl());
        } catch (Exception ex) {
            if (ex instanceof InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Analysis interrupted", interrupted);
            }
            logger.warn("File-based analysis failed for {}", domain.rootUrl(), ex);
            issues.add("File analysis failed: " + ex.getMessage());
        }

        // HTML parsing analysis
        try {
            htmlFindings = analyseHtml(domain.targetUrl());
        } catch (Exception ex) {
            if (ex instanceof InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Analysis interrupted", interrupted);
            }
            logger.warn("HTML parsing analysis failed for {}", domain.targetUrl(), ex);
            issues.add("HTML parsing failed: " + ex.getMessage());
        }

        // Headless browser network capture
        try {
            networkFindings = analyseNetworkTraffic(domain.targetUrl());
        } catch (Exception ex) {
            logger.warn("Network capture failed for {}", domain.targetUrl(), ex);
            issues.add("Network capture failed: " + ex.getMessage());
        }

        EndpointAnalysisPayload payload = new EndpointAnalysisPayload(fileFindings, htmlFindings, networkFindings, issues);

        EndpointAnalysisResult result = new EndpointAnalysisResult();
        result.setProject(project);
        result.setDomain(domain.displayUrl());
        result.setExecutedAt(Instant.now());
        result.setFileDiscoveryCount(fileFindings.size());
        result.setHtmlDiscoveryCount(htmlFindings.size());
        result.setNetworkDiscoveryCount(networkFindings.size());
        if (!issues.isEmpty()) {
            result.setStatus(fileFindings.isEmpty() && htmlFindings.isEmpty() && networkFindings.isEmpty()
                ? EndpointAnalysisStatus.FAILED
                : EndpointAnalysisStatus.PARTIAL);
            result.setErrorDetails(String.join("\n", issues));
        } else {
            result.setStatus(EndpointAnalysisStatus.SUCCESS);
        }
        result.setPayloadJson(jsonParserUtil.toJson(payload));

        return repository.save(result);
    }

    private List<EndpointAnalysisPayload.FileResourceFinding> analyseCommonResources(String baseUrl) throws IOException, InterruptedException {
        List<EndpointAnalysisPayload.FileResourceFinding> findings = new ArrayList<>();
        for (String path : COMMON_RESOURCES) {
            String target = baseUrl + path;
            HttpRequest request = HttpRequest.newBuilder(URI.create(target))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();

            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            } catch (IOException | InterruptedException ex) {
                findings.add(new EndpointAnalysisPayload.FileResourceFinding(
                    path,
                    -1,
                    false,
                    0L,
                    "",
                    ex.getMessage()
                ));
                if (ex instanceof InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw interruptedException;
                }
                continue;
            }

            boolean accessible = response.statusCode() < 400;
            String contentType = response.headers().firstValue("content-type").orElse("");
            long length = response.body() != null ? response.body().length() : 0L;
            String preview = accessible ? preview(response.body()) : response.body();

            findings.add(new EndpointAnalysisPayload.FileResourceFinding(
                path,
                response.statusCode(),
                accessible,
                length,
                contentType,
                preview
            ));
        }
        return findings;
    }

    private List<EndpointAnalysisPayload.HtmlEndpointFinding> analyseHtml(String targetUrl) throws IOException {
        Document document = Jsoup.connect(targetUrl)
            .userAgent(USER_AGENT)
            .timeout(10_000)
            .get();

        Set<EndpointAnalysisPayload.HtmlEndpointFinding> results = new LinkedHashSet<>();

        for (Element link : document.select("a[href]")) {
            String href = link.attr("abs:href");
            if (!href.isBlank()) {
                results.add(new EndpointAnalysisPayload.HtmlEndpointFinding("a", trimAbsoluteUrl(href), link.text()));
            }
        }

        for (Element script : document.select("script[src]")) {
            String src = script.attr("abs:src");
            if (!src.isBlank()) {
                results.add(new EndpointAnalysisPayload.HtmlEndpointFinding("script", trimAbsoluteUrl(src), ""));
            }
        }

        for (Element resource : document.select("link[href]")) {
            String href = resource.attr("abs:href");
            if (!href.isBlank()) {
                results.add(new EndpointAnalysisPayload.HtmlEndpointFinding("link", trimAbsoluteUrl(href), resource.attr("rel")));
            }
        }

        for (Element form : document.select("form[action]")) {
            String action = form.attr("abs:action");
            if (!action.isBlank()) {
                results.add(new EndpointAnalysisPayload.HtmlEndpointFinding("form", trimAbsoluteUrl(action), form.attr("method")));
            }
        }

        return new ArrayList<>(results).subList(0, Math.min(results.size(), 100));
    }

    private List<EndpointAnalysisPayload.NetworkRequestFinding> analyseNetworkTraffic(String targetUrl) {
        List<EndpointAnalysisPayload.NetworkRequestFinding> networkFindings = new ArrayList<>();
        BrowserMobProxy proxy = null;
        WebDriver driver = null;
        try {
            proxy = new BrowserMobProxyServer();
            proxy.setTrustAllServers(true); // Trust all servers
            proxy.start(0);
            proxy.enableHarCaptureTypes(EnumSet.of(CaptureType.REQUEST_HEADERS, CaptureType.RESPONSE_HEADERS));
            proxy.newHar("endpoint-analysis");

            Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage", "--window-size=1920,1080");
            options.setProxy(seleniumProxy);
            options.setAcceptInsecureCerts(true);

            WebDriverManager.chromedriver().setup();
            driver = new ChromeDriver(options);

            driver.get(targetUrl);
            waitForPageReady(driver);
            sleep(2000);

            Har har = proxy.getHar();
            if (har != null && har.getLog() != null && har.getLog().getEntries() != null) {
                for (HarEntry entry : har.getLog().getEntries()) {
                    if (entry.getRequest() == null) {
                        continue;
                    }
                    String url = Optional.ofNullable(entry.getRequest().getUrl()).orElse("");
                    if (url.isBlank()) {
                        continue;
                    }
                    String method = Optional.ofNullable(entry.getRequest().getMethod()).orElse("GET");
                    int status = entry.getResponse() != null ? entry.getResponse().getStatus() : -1;
                    String contentType = entry.getResponse() != null && entry.getResponse().getHeaders() != null
                        ? entry.getResponse().getHeaders().stream()
                            .filter(h -> "content-type".equalsIgnoreCase(h.getName()))
                            .findFirst()
                            .map(net.lightbody.bmp.core.har.HarNameValuePair::getValue)
                            .orElse("")
                        : "";

                    networkFindings.add(new EndpointAnalysisPayload.NetworkRequestFinding(
                        method,
                        trimAbsoluteUrl(url),
                        status,
                        contentType
                    ));

                    if (networkFindings.size() >= 150) {
                        break;
                    }
                }
            }
        } catch (WebDriverException ex) {
            throw new IllegalStateException("Selenium execution failed: " + ex.getMessage(), ex);
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception ex) {
                    logger.debug("Failed to close WebDriver cleanly", ex);
                }
            }
            if (proxy != null) {
                try {
                    proxy.stop();
                } catch (Exception ex) {
                    logger.debug("Failed to stop BrowserMob proxy cleanly", ex);
                }
            }
        }
        return networkFindings;
    }

    private void waitForPageReady(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.until((ExpectedCondition<Boolean>) d -> {
            if (d instanceof JavascriptExecutor executor) {
                Object state = executor.executeScript("return document.readyState");
                return "complete".equals(state);
            }
            return true;
        });
    }

    private DomainContext normaliseDomain(String raw) {
        String prepared = ensureScheme(raw);
        try {
            URI uri = new URI(prepared);
            if (uri.getHost() == null) {
                throw new IllegalArgumentException("Invalid domain URL");
            }
            String scheme = Optional.ofNullable(uri.getScheme()).orElse("https");
            int port = uri.getPort();
            String host = uri.getHost();
            String root = port > 0 ? "%s://%s:%d".formatted(scheme, host, port) : "%s://%s".formatted(scheme, host);
            String target = uri.getPath() != null && !uri.getPath().isBlank()
                ? root + uri.getPath()
                : root;
            if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                target = target + "?" + uri.getQuery();
            }
            return new DomainContext(root, target, prepared);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid domain URL", ex);
        }
    }

    private String ensureScheme(String raw) {
        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            return raw;
        }
        return "https://" + raw;
    }

    private String trimAbsoluteUrl(String url) {
        if (url == null) {
            return "";
        }
        return url.strip();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private String preview(String body) {
        if (body == null) {
            return "";
        }
        int length = Math.min(body.length(), 500);
        String snippet = body.substring(0, length);
        if (body.length() > length) {
            snippet += "...";
        }
        return snippet;
    }

    private record DomainContext(String rootUrl, String targetUrl, String displayUrl) { }
}
