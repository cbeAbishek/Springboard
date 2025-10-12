#!/bin/bash
# IntelliJ IDEA Setup Script for Springboard Project with JDK 21

echo "Setting up Springboard project for IntelliJ IDEA with JDK 21 LTS..."

# Set JDK 21 environment
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

echo "Using JDK: $(java -version 2>&1 | head -n1)"

# Create IntelliJ IDEA project configuration
mkdir -p .idea

# Create compiler.xml for IntelliJ
cat > .idea/compiler.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="CompilerConfiguration">
    <bytecodeTargetLevel target="21" />
  </component>
  <component name="JavacSettings">
    <option name="ADDITIONAL_OPTIONS_OVERRIDE">
      <module name="testinffame" options="-parameters" />
    </option>
  </component>
</project>
EOF

# Create misc.xml for project settings
cat > .idea/misc.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="ExternalStorageConfigurationManager" enabled="true" />
  <component name="MavenProjectsManager">
    <option name="originalFiles">
      <list>
        <option value="$PROJECT_DIR$/pom.xml" />
      </list>
    </option>
  </component>
  <component name="ProjectRootManager" version="2" languageLevel="JDK_21" default="true" project-jdk-name="21" project-jdk-type="JavaSDK">
    <output url="file://$PROJECT_DIR$/out" />
  </component>
</project>
EOF

# Create run configuration for Spring Boot
mkdir -p .idea/runConfigurations
cat > .idea/runConfigurations/SpringboardApplication.xml << 'EOF'
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="SpringboardApplication" type="SpringBootApplicationConfigurationType" factoryName="Spring Boot">
    <module name="testinffame" />
    <option name="SPRING_BOOT_MAIN_CLASS" value="org.example.Main" />
    <option name="ALTERNATIVE_JRE_PATH" value="21" />
    <option name="ALTERNATIVE_JRE_PATH_ENABLED" value="true" />
    <method v="2">
      <option name="Make" enabled="true" />
    </method>
  </configuration>
</component>
EOF

echo "✅ IntelliJ IDEA configuration created successfully!"
echo ""
echo "📋 Next Steps for IntelliJ IDEA:"
echo "1. Open IntelliJ IDEA"
echo "2. File → Open → Select the Springboard project folder"
echo "3. When prompted, select 'Import as Maven project'"
echo "4. Go to File → Project Structure → Project → Set SDK to JDK 21"
echo "5. Go to File → Settings → Build → Compiler → Java Compiler → Set Project bytecode version to 21"
echo "6. Click 'Reload Maven Project' in the Maven tool window"
echo ""
echo "🚀 Your Springboard project is now ready to run with JDK 21 LTS!"
