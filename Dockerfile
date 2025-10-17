FROM openjdk:21-jdk-bullseye

LABEL authors="petr.zemanek"

WORKDIR /app

COPY build/libs/custom.adapter-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]