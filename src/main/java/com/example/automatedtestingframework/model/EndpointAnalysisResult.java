package com.example.automatedtestingframework.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "endpoint_analysis_results")
public class EndpointAnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Project project;

    @Column(nullable = false, length = 512)
    private String domain;

    @Column(nullable = false)
    private Instant executedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EndpointAnalysisStatus status;

    @Lob
    @Column(name = "payload_json", columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Column(name = "file_discovery_count")
    private int fileDiscoveryCount;

    @Column(name = "html_discovery_count")
    private int htmlDiscoveryCount;

    @Column(name = "network_discovery_count")
    private int networkDiscoveryCount;

    @Lob
    @Column(name = "error_details", columnDefinition = "LONGTEXT")
    private String errorDetails;

    public EndpointAnalysisResult() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }

    public EndpointAnalysisStatus getStatus() {
        return status;
    }

    public void setStatus(EndpointAnalysisStatus status) {
        this.status = status;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public int getFileDiscoveryCount() {
        return fileDiscoveryCount;
    }

    public void setFileDiscoveryCount(int fileDiscoveryCount) {
        this.fileDiscoveryCount = fileDiscoveryCount;
    }

    public int getHtmlDiscoveryCount() {
        return htmlDiscoveryCount;
    }

    public void setHtmlDiscoveryCount(int htmlDiscoveryCount) {
        this.htmlDiscoveryCount = htmlDiscoveryCount;
    }

    public int getNetworkDiscoveryCount() {
        return networkDiscoveryCount;
    }

    public void setNetworkDiscoveryCount(int networkDiscoveryCount) {
        this.networkDiscoveryCount = networkDiscoveryCount;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }
}
