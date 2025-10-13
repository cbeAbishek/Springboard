package com.example.automatedtestingframework.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "generated_reports")
public class GeneratedReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "filter_type", length = 10)
    private TestCaseType filterType;

    @Column(name = "filter_status", length = 32)
    private String filterStatus;

    @Column(name = "filter_from")
    private OffsetDateTime filterFrom;

    @Column(name = "filter_to")
    private OffsetDateTime filterTo;

    @Column(name = "total_records")
    private Integer totalRecords;

    @Column(name = "file_url", length = 512, nullable = false)
    private String fileUrl;

    @Column(name = "file_name", length = 160, nullable = false)
    private String fileName;

    @Column(name = "mime_type", length = 64, nullable = false)
    private String mimeType;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public TestCaseType getFilterType() {
        return filterType;
    }

    public void setFilterType(TestCaseType filterType) {
        this.filterType = filterType;
    }

    public String getFilterStatus() {
        return filterStatus;
    }

    public void setFilterStatus(String filterStatus) {
        this.filterStatus = filterStatus;
    }

    public OffsetDateTime getFilterFrom() {
        return filterFrom;
    }

    public void setFilterFrom(OffsetDateTime filterFrom) {
        this.filterFrom = filterFrom;
    }

    public OffsetDateTime getFilterTo() {
        return filterTo;
    }

    public void setFilterTo(OffsetDateTime filterTo) {
        this.filterTo = filterTo;
    }

    public Integer getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
