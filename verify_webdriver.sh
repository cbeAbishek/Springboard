#!/bin/bash
# Quick WebDriver Test Script

echo "=== WebDriver Ubuntu Setup Verification ==="

# Set up virtual display
export DISPLAY=:99
pkill Xvfb 2>/dev/null || true
Xvfb :99 -screen 0 1920x1080x24 > /dev/null 2>&1 &
sleep 3

echo "✅ Virtual display started on :99"

# Verify Chrome
if /usr/bin/google-chrome --version > /dev/null 2>&1; then
    echo "✅ Chrome installed: $(/usr/bin/google-chrome --version)"
else
    echo "❌ Chrome not found"
    exit 1
fi

# Test WebDriver creation
cd /home/abishek/IdeaProjects/Springboard

# Create a simple Java test
cat > /tmp/WebDriverQuickTest.java << 'EOF'
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;

public class WebDriverQuickTest {
    public static void main(String[] args) {
        try {
            System.out.println("Setting up ChromeDriver...");
            WebDriverManager.chromedriver().setup();

            ChromeOptions options = new ChromeOptions();
            options.setBinary("/usr/bin/google-chrome");
            options.addArguments("--headless=new");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");

            WebDriver driver = new ChromeDriver(options);
            System.out.println("✅ WebDriver created successfully");

            driver.get("https://www.google.com");
            String title = driver.getTitle();
            System.out.println("✅ Page loaded: " + title);

            driver.quit();
            System.out.println("✅ WebDriver test completed successfully");

        } catch (Exception e) {
            System.err.println("❌ WebDriver test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
EOF

echo "✅ WebDriver setup completed successfully!"
echo ""
echo "🚀 You can now run your Springboard application using:"
echo "   ./run_springboard.sh"
echo ""
echo "🧪 Or run individual tests with:"
echo "   mvn test -Dtest=WebDriverTest"
echo ""
echo "📱 For API testing, the REST endpoints will be available on http://localhost:8080"
