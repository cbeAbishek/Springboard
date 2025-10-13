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
public class ScreenshotService {

    private static final Logger log = LoggerFactory.getLogger(ScreenshotService.class);

    private final ImageKit imageKit;
    private final boolean imageKitConfigured;
    private final String folder;

    public ScreenshotService(ImageKit imageKit,
                             @Value("${imagekit.folder:/UI_Report}") String folder) {
        this.imageKit = imageKit;
        this.folder = folder;
        this.imageKitConfigured = isConfigured(imageKit);
    }

    public String uploadScreenshot(byte[] data, String filename) {
        if (!imageKitConfigured) {
            log.warn("ImageKit credentials not configured; returning inline Base64 image");
            return base64DataUri(data);
        }

        try {
            FileCreateRequest request = new FileCreateRequest(Base64.getEncoder().encodeToString(data), filename);
            request.setUseUniqueFileName(true);
            if (StringUtils.hasText(folder)) {
                request.setFolder(folder.trim());
            }

            Result result = imageKit.upload(request);
            if (result != null && StringUtils.hasText(result.getUrl())) {
                String url = result.getUrl();
                log.info("Screenshot uploaded successfully: {}", url);
                return url;
            }

            log.error("ImageKit SDK did not return a URL for file {}", filename);
        } catch (Exception ex) {
            log.error("Unexpected failure uploading screenshot via ImageKit SDK for file {}", filename, ex);
        }

        log.info("Returning base64 encoded image as fallback");
        return base64DataUri(data);
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

    private String base64DataUri(byte[] data) {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(data);
    }
}
