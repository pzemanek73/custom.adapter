# ---- Build stage ----
FROM gradle:8.10.2-jdk21 AS build
WORKDIR /workspace

# copy build files first (better cache)
COPY gradlew gradle/ settings.gradle build.gradle ./
# if you have more *.gradle or buildSrc, copy them too
# COPY buildSrc buildSrc

# then sources
COPY src ./src

# build the jar
RUN ./gradlew --no-daemon clean bootJar -x test

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# copy the built jar (wildcard avoids hard-coding the name)
COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
