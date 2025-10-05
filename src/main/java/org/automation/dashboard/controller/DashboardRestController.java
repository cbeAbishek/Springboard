package org.automation.dashboard.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DashboardRestController {

    @GetMapping("/generate-allure-report")
    public Map<String, Object> generateAllureReport() {
        Map<String, Object> response = new HashMap<>();

        try {
            File allureResults = new File("allure-results");
            if (!allureResults.exists() || allureResults.listFiles() == null || allureResults.listFiles().length == 0) {
                response.put("success", false);
                response.put("message", "No allure-results found. Run tests first to generate results.");
                return response;
            }

            // Check if allure command exists
            ProcessBuilder checkAllure = new ProcessBuilder("which", "allure");
            Process checkProcess = checkAllure.start();
            int checkResult = checkProcess.waitFor();

            if (checkResult != 0) {
                response.put("success", false);
                response.put("message", "Allure CLI not installed. Generating report using Maven plugin...");

                // Try using Maven allure plugin
                ProcessBuilder mavenAllure = new ProcessBuilder("mvn", "allure:report");
                mavenAllure.redirectErrorStream(true);
                Process mvnProcess = mavenAllure.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(mvnProcess.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }

                int mvnResult = mvnProcess.waitFor();
                if (mvnResult == 0) {
                    response.put("success", true);
                    response.put("message", "Allure report generated successfully using Maven");
                } else {
                    response.put("success", false);
                    response.put("message", "Failed to generate report. Please install Allure CLI.");
                    response.put("output", output.toString());
                }
                return response;
            }

            // Generate report using Allure CLI
            ProcessBuilder pb = new ProcessBuilder("allure", "generate", "allure-results", "-o", "allure-report", "--clean");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                response.put("success", true);
                response.put("message", "Allure report generated successfully");
                response.put("reportUrl", "/allure-report/index.html");
            } else {
                response.put("success", false);
                response.put("message", "Failed to generate Allure report");
                response.put("output", output.toString());
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error generating report: " + e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return response;
    }
}

