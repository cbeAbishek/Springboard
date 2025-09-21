package org.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import jakarta.annotation.PostConstruct;
import java.util.logging.Level;

/**
 * Configuration class to suppress all CDP and Selenium warnings
 * This ensures that Chrome DevTools Protocol version mismatch warnings are completely suppressed
 */
@Configuration
public class SeleniumLoggingConfig {

    private static final Logger log = LoggerFactory.getLogger(SeleniumLoggingConfig.class);

    @PostConstruct
    public void suppressSeleniumLogging() {
        log.info("Configuring Selenium logging suppression for Chrome 140 CDP compatibility");

        // Comprehensive system properties to suppress all CDP warnings
        System.setProperty("webdriver.chrome.silentOutput", "true");
        System.setProperty("webdriver.chrome.logfile", "/dev/null");
        System.setProperty("webdriver.chrome.verboseLogging", "false");
        System.setProperty("webdriver.chrome.args", "--silent --log-level=3 --disable-logging");
        System.setProperty("org.openqa.selenium.logging.ignore", "org.openqa.selenium.devtools");
        System.setProperty("selenium.LOGGER", "OFF");
        System.setProperty("webdriver.chrome.driver.log", "OFF");

        // Disable all Java logging for Selenium packages
        java.util.logging.Logger seleniumLogger = java.util.logging.Logger.getLogger("org.openqa.selenium");
        seleniumLogger.setLevel(Level.OFF);
        seleniumLogger.setUseParentHandlers(false);

        java.util.logging.Logger devtoolsLogger = java.util.logging.Logger.getLogger("org.openqa.selenium.devtools");
        devtoolsLogger.setLevel(Level.OFF);
        devtoolsLogger.setUseParentHandlers(false);

        java.util.logging.Logger remoteLogger = java.util.logging.Logger.getLogger("org.openqa.selenium.remote");
        remoteLogger.setLevel(Level.OFF);
        remoteLogger.setUseParentHandlers(false);

        java.util.logging.Logger chromeLogger = java.util.logging.Logger.getLogger("org.openqa.selenium.chrome");
        chromeLogger.setLevel(Level.OFF);
        chromeLogger.setUseParentHandlers(false);

        // Specific CDP warning suppression
        java.util.logging.Logger cdpLogger = java.util.logging.Logger.getLogger("org.openqa.selenium.devtools.CdpVersionFinder");
        cdpLogger.setLevel(Level.OFF);
        cdpLogger.setUseParentHandlers(false);

        // WebDriverManager logging
        java.util.logging.Logger wdmLogger = java.util.logging.Logger.getLogger("io.github.bonigarcia.wdm");
        wdmLogger.setLevel(Level.WARNING);

        // Disable root logger for selenium packages
        java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
        rootLogger.setLevel(Level.WARNING);

        log.info("Selenium CDP warning suppression configured successfully");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application ready - Selenium logging suppression is active");
        log.info("Chrome DevTools Protocol (CDP) warnings for version 140 are now suppressed");
    }
}
