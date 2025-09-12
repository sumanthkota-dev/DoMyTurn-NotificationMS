# -----------------------------
# Stage 1: Build with Maven + JDK 21
# -----------------------------
FROM maven:3.9.9-eclipse-temurin-21 AS build

# -----------------------------
# Build CommonDTO first
# -----------------------------
WORKDIR /common-dto
COPY CommonDTO/ /common-dto
RUN mvn clean install -DskipTests

# -----------------------------
# Build CommonSecurity (depends on CommonDTO)
# -----------------------------
WORKDIR /common-security
COPY CommonSecurity/ /common-security
RUN mvn clean install -DskipTests

# -----------------------------
# Build AuthenticationMS (depends on CommonSecurity + CommonDTO)
# -----------------------------
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline -B || true
COPY src ./src
RUN mvn clean package -DskipTests

# -----------------------------
# Stage 2: Runtime (lightweight JDK)
# -----------------------------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose Spring Boot default port
EXPOSE 8080

# Run the service
ENTRYPOINT ["java", "-jar", "app.jar"]
