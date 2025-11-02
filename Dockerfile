# Build stage - Maven + JDK to compile
FROM maven:3.9-amazoncorretto-21-alpine AS build

WORKDIR /app

# Cache dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build application
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage - Minimal JRE only
FROM amazoncorretto:21-alpine-jre

WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/code-cache-0.1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

# Memory limit for Render free tier
CMD ["java", "-Xmx450m", "-jar", "app.jar"]