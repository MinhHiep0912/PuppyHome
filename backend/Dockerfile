# Stage 1: Build the application JAR
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

# Copy Maven wrapper and POM first to leverage layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' ./mvnw && chmod +x ./mvnw

# Copy source code and build production artifact
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Runtime environment
FROM eclipse-temurin:25-jre
WORKDIR /app

# Install curl for container health check
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
