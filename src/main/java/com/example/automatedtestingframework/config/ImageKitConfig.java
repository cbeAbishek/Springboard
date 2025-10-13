package com.example.automatedtestingframework.config;

import io.imagekit.sdk.ImageKit;
import io.imagekit.sdk.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Configuration
public class ImageKitConfig {

    private static final Logger log = LoggerFactory.getLogger(ImageKitConfig.class);

    @Bean
    public ImageKit imageKit(@Value("${imagekit.public-key:}") String publicKey,
                             @Value("${imagekit.private-key:}") String privateKey,
                             @Value("${imagekit.url-endpoint:${imagekit.upload-url:}}") String urlEndpoint) {
        ImageKit imageKit = ImageKit.getInstance();

        Optional<io.imagekit.sdk.config.Configuration> configurationOptional = loadFromConfigFile();
        if (configurationOptional.isPresent()) {
            imageKit.setConfig(configurationOptional.get());
            log.info("Initialized ImageKit using config.properties from classpath");
            return imageKit;
        }

        if (StringUtils.hasText(publicKey) && StringUtils.hasText(privateKey) && StringUtils.hasText(urlEndpoint)) {
            io.imagekit.sdk.config.Configuration configuration =
                new io.imagekit.sdk.config.Configuration(publicKey.trim(), privateKey.trim(), urlEndpoint.trim());
            imageKit.setConfig(configuration);
            log.info("Initialized ImageKit using Spring properties");
        } else {
            log.warn("ImageKit credentials missing; screenshot uploads will fall back to Base64 data URI");
        }

        return imageKit;
    }

    private Optional<io.imagekit.sdk.config.Configuration> loadFromConfigFile() {
        try {
            io.imagekit.sdk.config.Configuration configuration = Utils.getSystemConfig(ImageKitConfig.class);
            if (configuration != null
                && StringUtils.hasText(configuration.getPrivateKey())
                && StringUtils.hasText(configuration.getPublicKey())
                && StringUtils.hasText(configuration.getUrlEndpoint())) {
                return Optional.of(configuration);
            }
        } catch (Exception ex) {
            log.debug("Unable to load ImageKit configuration from config.properties", ex);
        }
        return Optional.empty();
    }
}
