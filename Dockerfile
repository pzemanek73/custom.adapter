# ---- Build stage ----
FROM gradle:8.10.2-jdk21 AS build
WORKDIR /workspace

# copy wrapper + build files first for caching
COPY gradlew gradle/ settings.gradle build.gradle ./
RUN chmod +x gradlew

# then sources
COPY src ./src

# build the jar
RUN ./gradlew --no-daemon clean bootJar -x test

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
