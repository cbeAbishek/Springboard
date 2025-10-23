package com.example.automatedtestingframework.analysis;

import java.util.ArrayList;
import java.util.List;

public record EndpointAnalysisPayload(
    List<FileResourceFinding> fileResources,
    List<HtmlEndpointFinding> htmlEndpoints,
    List<NetworkRequestFinding> networkRequests,
    List<String> issues
) {
    public EndpointAnalysisPayload {
        fileResources = fileResources != null ? fileResources : new ArrayList<>();
        htmlEndpoints = htmlEndpoints != null ? htmlEndpoints : new ArrayList<>();
        networkRequests = networkRequests != null ? networkRequests : new ArrayList<>();
        issues = issues != null ? issues : new ArrayList<>();
    }

    public record FileResourceFinding(
        String path,
        int statusCode,
        boolean accessible,
        long contentLength,
        String contentType,
        String preview
    ) { }

    public record HtmlEndpointFinding(
        String tag,
        String url,
        String text
    ) { }

    public record NetworkRequestFinding(
        String method,
        String url,
        int statusCode,
        String contentType
    ) { }
}
