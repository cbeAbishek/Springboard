package com.example.automatedtestingframework.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 512)
    private String description;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User owner;

    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TestCase> testCases = new HashSet<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SchedulerJob> schedulerJobs = new HashSet<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Report> reports = new HashSet<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<GeneratedActionFile> actionFiles = new HashSet<>();

    @Column(length = 512)
    private String automationEndpoint;

    @Column(length = 120)
    private String automationToken;

    @Column(length = 120)
    private String automationTargetProjectId;

    @Column(length = 120)
    private String automationBranches;

    @Column(length = 512)
    private String githubLink;

    @Column(length = 512)
    private String websiteLink;

    @Column(length = 512)
    private String appDomain;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<TestCase> getTestCases() {
        return testCases;
    }

    public void setTestCases(Set<TestCase> testCases) {
        this.testCases = testCases;
    }

    public Set<SchedulerJob> getSchedulerJobs() {
        return schedulerJobs;
    }

    public void setSchedulerJobs(Set<SchedulerJob> schedulerJobs) {
        this.schedulerJobs = schedulerJobs;
    }

    public Set<Report> getReports() {
        return reports;
    }

    public void setReports(Set<Report> reports) {
        this.reports = reports;
    }

    public Set<GeneratedActionFile> getActionFiles() {
        return actionFiles;
    }

    public void setActionFiles(Set<GeneratedActionFile> actionFiles) {
        this.actionFiles = actionFiles;
    }

    public String getAutomationEndpoint() {
        return automationEndpoint;
    }

    public void setAutomationEndpoint(String automationEndpoint) {
        this.automationEndpoint = automationEndpoint;
    }

    public String getAutomationToken() {
        return automationToken;
    }

    public void setAutomationToken(String automationToken) {
        this.automationToken = automationToken;
    }

    public String getAutomationTargetProjectId() {
        return automationTargetProjectId;
    }

    public void setAutomationTargetProjectId(String automationTargetProjectId) {
        this.automationTargetProjectId = automationTargetProjectId;
    }

    public String getAutomationBranches() {
        return automationBranches;
    }

    public void setAutomationBranches(String automationBranches) {
        this.automationBranches = automationBranches;
    }

    public String getGithubLink() {
        return githubLink;
    }

    public void setGithubLink(String githubLink) {
        this.githubLink = githubLink;
    }

    public String getWebsiteLink() {
        return websiteLink;
    }

    public void setWebsiteLink(String websiteLink) {
        this.websiteLink = websiteLink;
    }

    public String getAppDomain() {
        return appDomain;
    }

    public void setAppDomain(String appDomain) {
        this.appDomain = appDomain;
    }
}
