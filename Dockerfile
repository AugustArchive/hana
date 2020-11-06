FROM openjdk:11-alpine

ARG version

WORKDIR /opt/API
COPY /build/libs/API-${version}.jar API.jar
COPY config.yml config.yml

RUN ["java", "-jar", "API.jar"]
