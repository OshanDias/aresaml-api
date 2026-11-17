# ---- Build stage: compile the Spring Boot app into a runnable jar ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Run stage: a small JRE image that just runs the jar ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# JVM flags tuned for a small (256 MB) free-tier container:
# cap the heap to a share of container RAM and use the lightweight serial GC.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65.0", "-XX:+UseSerialGC", "-Xss512k", "-jar", "app.jar"]
