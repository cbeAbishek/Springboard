package org.automation.api;

import io.restassured.RestAssured;
import org.testng.annotations.BeforeClass;
import org.automation.config.ConfigManager;

import static io.restassured.config.DecoderConfig.decoderConfig;

public class ApiBaseTest {

    @BeforeClass
    public void setup() {
        String base = System.getProperty("base.api", ConfigManager.getApiBaseUrl());
        RestAssured.baseURI = base;
        RestAssured.config = RestAssured.config()
                .decoderConfig(decoderConfig().defaultContentCharset("UTF-8"));
    }
}
