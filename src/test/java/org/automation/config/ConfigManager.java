package org.automation.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ConfigManager {
    private static final Properties PROPS = new Properties();
    private static final String DB_PROPS_PATH = "config/db.properties";

    static {
        loadDefaults();
        loadFromFile();
        loadOverrides();
    }

    private static void loadDefaults() {
        PROPS.setProperty("base.url.ui", "https://blazedemo.com");
        PROPS.setProperty("base.url.api", "https://jsonplaceholder.typicode.com");
        PROPS.setProperty("db.url", "jdbc:mysql://localhost:3306/automation_tests");
        PROPS.setProperty("db.username", "root");
        PROPS.setProperty("db.password", "root");
        PROPS.setProperty("db.enabled", "true");
        PROPS.setProperty("artifacts.enabled", "true");
    }

    private static void loadFromFile() {
        Path p = Path.of(DB_PROPS_PATH);
        if (Files.exists(p)) {
            try (FileInputStream fis = new FileInputStream(p.toFile())) {
                Properties fileProps = new Properties();
                fileProps.load(fis);
                fileProps.forEach((k, v) -> PROPS.setProperty(k.toString(), v.toString()));
            } catch (IOException ignored) {}
        }
    }

    private static void loadOverrides() {
        // System properties override
        System.getProperties().forEach((k, v) -> {
            String key = k.toString();
            if (key.startsWith("db.") || key.startsWith("base.url") || key.endsWith("enabled")) {
                PROPS.setProperty(key, v.toString());
            }
        });
        // Environment variables override (uppercase with underscores)
        overrideFromEnv("DB_URL", "db.url");
        overrideFromEnv("DB_USERNAME", "db.username");
        overrideFromEnv("DB_USER", "db.username");
        overrideFromEnv("DB_PASSWORD", "db.password");
        overrideFromEnv("DB_PASS", "db.password");
        overrideFromEnv("DB_ENABLED", "db.enabled");
        overrideFromEnv("ARTIFACTS_ENABLED", "artifacts.enabled");
    }

    private static void overrideFromEnv(String envKey, String propKey) {
        String val = System.getenv(envKey);
        if (val != null && !val.isBlank()) {
            PROPS.setProperty(propKey, val.trim());
        }
    }

    public static String get(String key) {
        return PROPS.getProperty(key);
    }

    public static String getUiBaseUrl() { return get("base.url.ui"); }
    public static String getApiBaseUrl() { return get("base.url.api"); }
    public static String getDbUrl() { return get("db.url"); }
    public static String getDbUser() { return get("db.username"); }
    public static String getDbPassword() { return get("db.password"); }

    public static boolean isDbEnabled() { return parseBoolean(get("db.enabled"), true); }
    public static boolean isArtifactsEnabled() { return parseBoolean(get("artifacts.enabled"), true); }

    private static boolean parseBoolean(String v, boolean def) {
        if (v == null) return def;
        v = v.trim().toLowerCase();
        return v.equals("true") || v.equals("1") || v.equals("yes") || v.equals("y");
    }
}
