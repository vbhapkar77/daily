# Daily backend — multi-stage Dockerfile (per ADR-0008)
# Build stage: full JDK + Maven, downloads dependencies and builds the jar.
# Runtime stage: JRE only, runs the jar. Smaller final image.

# ---- build stage ----
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# Copy Maven wrapper + pom first so dependency download is cached when source-only changes
COPY pom.xml mvnw ./
COPY .mvn ./.mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src ./src
RUN ./mvnw -B -q package -DskipTests \
    && cp target/*.jar app.jar

# ---- runtime stage ----
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Run as non-root (security best practice)
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /app/app.jar app.jar

EXPOSE 8080

# Container-aware JVM flags. UseContainerSupport is default in modern JREs but explicit is clearer.
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --quiet --spider http://localhost:8080/actuator/health || exit 1
