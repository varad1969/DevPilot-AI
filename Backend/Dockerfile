# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy Maven wrapper & pom first (layer-cache friendly)
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies without building source (cached unless pom.xml changes)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

# Copy source and build the fat JAR, skipping tests
COPY src src
RUN ./mvnw package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy fat JAR from builder
COPY --from=builder /workspace/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=65.0", "-XX:MetaspaceSize=128m", "-XX:MaxMetaspaceSize=384m", "-jar", "app.jar"]
