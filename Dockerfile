# syntax=docker/dockerfile:1

# ---- Build stage ----
# A JDK + Maven image compiles the application into an executable jar.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Resolve dependencies in their own layer so it is cached unless pom.xml changes.
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Then compile and package. Tests run in CI (they need Docker for Testcontainers), so they
# are skipped here to keep the image build self-contained and fast.
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Runtime stage ----
# A slim JRE (no compiler/Maven) keeps the final image small.
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as an unprivileged user rather than root.
RUN groupadd --system spring && useradd --system --no-create-home --gid spring spring

COPY --from=build /app/target/*.jar app.jar
USER spring:spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
