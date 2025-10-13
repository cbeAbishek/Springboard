# syntax=docker/dockerfile:1.6

FROM maven:3.9.9-eclipse-temurin-23 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY .mvn/ .mvn/

RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp dependency:go-offline

COPY src ./src

RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

FROM eclipse-temurin:23-jre AS runtime

ENV APP_HOME=/app \
	SPRING_PROFILES_ACTIVE=prod \
	JAVA_TOOL_OPTIONS="" \
	WDM_CACHE_PATH=/app/.cache/webdriver \
	CHROME_BIN=/usr/bin/chromium

WORKDIR ${APP_HOME}

RUN apt-get update \
	&& apt-get install -y --no-install-recommends \
		chromium \
		chromium-driver \
		fonts-liberation \
		libasound2t64 \
		libatk-bridge2.0-0 \
		libatk1.0-0 \
		libdrm2 \
		libgbm1 \
		libgtk-3-0 \
		libnss3 \
		libxcomposite1 \
		libxdamage1 \
		libxrandr2 \
		libxss1 \
		libxtst6 \
	&& rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/target/automated-testing-framework-1.0.0.jar app.jar

RUN addgroup --system spring \
	&& adduser --system --ingroup spring spring \
	&& mkdir -p ${WDM_CACHE_PATH} \
	&& chown -R spring:spring ${WDM_CACHE_PATH} \
	&& chown spring:spring app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
