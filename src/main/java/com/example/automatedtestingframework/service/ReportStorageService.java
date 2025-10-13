package com.example.automatedtestingframework.service;

import io.imagekit.sdk.ImageKit;
import io.imagekit.sdk.config.Configuration;
import io.imagekit.sdk.models.FileCreateRequest;
import io.imagekit.sdk.models.results.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Base64;

@Service
public class ReportStorageService {

    private static final Logger log = LoggerFactory.getLogger(ReportStorageService.class);

    private final ImageKit imageKit;
    private final boolean imageKitConfigured;
    private final String folder;

    public ReportStorageService(ImageKit imageKit,
                                @Value("${imagekit.report-folder:/reports}") String folder) {
        this.imageKit = imageKit;
        this.folder = folder;
        this.imageKitConfigured = isConfigured(imageKit);
    }

    public String upload(byte[] payload, String fileName, String mimeType) {
        if (!imageKitConfigured) {
            log.warn("ImageKit not configured; returning inline data URI for generated report {}", fileName);
            return buildDataUri(payload, mimeType);
        }

        try {
            FileCreateRequest request = new FileCreateRequest(Base64.getEncoder().encodeToString(payload), fileName);
            request.setUseUniqueFileName(true);
            if (StringUtils.hasText(folder)) {
                request.setFolder(folder.trim());
            }

            Result result = imageKit.upload(request);
            if (result != null && StringUtils.hasText(result.getUrl())) {
                String url = result.getUrl();
                log.info("Generated report uploaded to ImageKit: {}", url);
                return url;
            }
            log.error("ImageKit upload succeeded without URL for file {}", fileName);
        } catch (Exception ex) {
            log.error("Failed to upload generated report {} to ImageKit", fileName, ex);
        }

        log.info("Falling back to inline data URI for report {}", fileName);
        return buildDataUri(payload, mimeType);
    }

    private boolean isConfigured(ImageKit imageKit) {
        if (imageKit == null) {
            return false;
        }
        Configuration configuration = imageKit.getConfig();
        return configuration != null
            && StringUtils.hasText(configuration.getPrivateKey())
            && StringUtils.hasText(configuration.getPublicKey())
            && StringUtils.hasText(configuration.getUrlEndpoint());
    }

    private String buildDataUri(byte[] payload, String mimeType) {
        String type = StringUtils.hasText(mimeType) ? mimeType : "text/plain";
        return "data:" + type + ";base64," + Base64.getEncoder().encodeToString(payload);
    }
}
