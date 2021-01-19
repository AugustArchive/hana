FROM alpine:latest

LABEL MAINTAINER="August <august@augu.dev>"

# Install JDK 11
RUN apk add git
RUN apk add openjdk11 --repository=http://dl-cdn.alpinelinux.org/alpine/edge/community
RUN java --version

# Now we run the API
WORKDIR /opt/api.floofy.dev
COPY . .

# Expose port 3621 because networking owo
EXPOSE 3621

# Run the created JAR file
ENTRYPOINT [ "java", "-jar", "./build/libs/API.jar" ]
