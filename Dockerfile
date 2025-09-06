# -----------------------------
# Stage 1: Build with Maven
# -----------------------------
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy Maven wrapper and pom.xml first (to leverage Docker caching)
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the JAR
RUN ./mvnw clean package -DskipTests

# -----------------------------
# Stage 2: Runtime
# -----------------------------
FROM amazoncorretto:21-alpine

WORKDIR /app

# Copy the JAR file from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the service port (Spring Boot default)
EXPOSE 8080

# Run the service
ENTRYPOINT ["java", "-jar", "app.jar"]
