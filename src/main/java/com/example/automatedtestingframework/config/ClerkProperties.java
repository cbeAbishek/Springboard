package com.example.automatedtestingframework.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component("clerkProperties")
@ConfigurationProperties(prefix = "clerk")
public class ClerkProperties {

    private String publishableKey;
    private String frontendApi;
    private String jwksUrl;
    private String issuer;
    private String audience;
    private String secretKey;

    public String getPublishableKey() {
        return publishableKey;
    }

    public void setPublishableKey(String publishableKey) {
        this.publishableKey = publishableKey;
    }

    public String getFrontendApi() {
        return frontendApi;
    }

    public void setFrontendApi(String frontendApi) {
        this.frontendApi = frontendApi;
    }

    public String getJwksUrl() {
        return jwksUrl;
    }

    public void setJwksUrl(String jwksUrl) {
        this.jwksUrl = jwksUrl;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public boolean isEnabled() {
        return StringUtils.hasText(jwksUrl) && StringUtils.hasText(publishableKey);
    }
}
