# -----------------------------------------------------------------------------
# STAGE 1: Build the application
# -----------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

# Copy the Maven wrapper and pom.xml first to leverage Docker cache
# (This step speeds up re-builds if dependencies haven't changed)
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Download dependencies (fail-safe for offline capability)
RUN ./mvnw dependency:go-offline

# Copy the actual source code
COPY src ./src

# Build the application
# skipping tests to speed up the container build (run tests in CI/CD pipeline instead)
RUN ./mvnw package -DskipTests

# Extract the jar file name (optional automation, or just hardcode the path below)
# Usually target/*.jar

# -----------------------------------------------------------------------------
# STAGE 2: Create the final runtime image
# -----------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built JAR from the builder stage
# Adjust 'app-0.0.1-SNAPSHOT.jar' to match your actual artifact name or use a wildcard
COPY --from=builder /app/target/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Configure the startup command
# Added "-XX:+UseContainerSupport" which is default in newer Java versions but good practice to know
ENTRYPOINT ["java", "-jar", "app.jar"]