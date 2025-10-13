package com.example.automatedtestingframework.config;

/**
 * Left for backward compatibility. Use {@link ImageKitConfig} instead.
 */
@Deprecated(forRemoval = true, since = "1.0.0")
public final class ImageKitConfiguration {

    private ImageKitConfiguration() {
        throw new UnsupportedOperationException("Use ImageKitConfig for ImageKit bean configuration");
    }
}
