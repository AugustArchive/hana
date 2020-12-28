FROM alpine:latest

LABEL MAINTAINER="August <august@augu.dev>"

# Install JDK 11
RUN apk add git
RUN apk add openjdk11 --repository=http://dl-cdn.alpinelinux.org/alpine/edge/community
RUN java --version

# Install Gradle
#RUN mkdir /tmp/gradle /tmp/gradle/gradle-6.7.1
#RUN wget https://services.gradle.org/distributions/gradle-6.7.1-all.zip -O /tmp/gradle/gradle-6.7.1.zip
#RUN unzip /tmp/gradle/gradle-6.7.1.zip -d /tmp/gradle/gradle-6.7.1 -p

# Now we run the API
WORKDIR /opt/api.floofy.dev
COPY . .

# Run Gradle
RUN chmod +x gradlew
RUN /bin/ash ./gradlew build spotlessApply

# Expose port 3621 because networking owo
EXPOSE 3621

# Run the created JAR file
CMD [ "java", "-jar", "./build/libs/API.jar" ]
