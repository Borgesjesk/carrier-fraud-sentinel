# ============ BUILD STAGE ============
# Compiles the Java application
FROM maven:3.9-eclipse-temurin-21 as builder

WORKDIR /build

# Copy pom.xml first (caches dependency layer)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# ============ RUNTIME STAGE ============
# Runs the compiled application (smaller image)
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy jar from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Health check endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD java -version

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

# Default Spring Boot port
EXPOSE 8081
