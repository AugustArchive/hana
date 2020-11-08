FROM openjdk:11

ARG version
MAINTAINER Chris "August" Hernandez <august@augu.dev>

WORKDIR /opt/API
COPY ./build .
COPY config.yml .

#COPY ./build/libs/API-${version}.jar API.jar
#COPY ./config.yml config.yml

RUN ["java", "-jar", "API.jar"]
