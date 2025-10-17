# ---- Build stage ----
FROM gradle:8.10.2-jdk21 AS build
WORKDIR /workspace

# copy build files first for better cache
COPY settings.gradle build.gradle ./
COPY gradle gradle
# If you don't want wrapper at all, you can skip copying gradle/ and settings,
# but then ensure build.gradle doesn't reference the wrapper task.

# then sources
COPY src ./src

# build the jar using the gradle binary in the image (no wrapper jar needed)
RUN gradle --no-daemon clean bootJar -x test

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
