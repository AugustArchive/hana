# api.augu.dev
[![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/)

> :black_heart: **| API service to do certain things, mainly for webhooks and proxying.**

## Installation Guide
### Requirements
- Docker (optional)
- Sentry (optional)
- JDK 11 (required)
- Git (required)

### Process (locally)
- [Fork](https://github.com/auguwu/API/fork) the repository under your username.
- Clone the repository (``git clone https://github.com/USERNAME/API``); omit `USERNAME` with your actual username.
- Change the directory to the newly cloned repository (``cd API`)
- Run `./gradlew build` if you're using Unix; run `gradlew build` if you're using Windows
- Copy the configuration under `config.example.yml` and copy it to make a new file called `config.yml`
  - This has to be placed where you build the JAR file, so `build/libs` is where it'll be at!
- Run the JAR file with `java -jar API-version.jar`; omit `version` with the version listed under [build.gradle.kts](/build.gradle.kts)

### Process (Docker)
> This process is the most recommended, so you don't have to install any dependencies under your machine.
>
> If you're on Windows or macOS, you must have Docker Desktop installed, else install Docker under Linux.
>
> You can skip this process by using the official image under the Docker registry, just use `docker.floofy.dev`
> when retrieving the image and run `docker pull auguwu/api:latest` then run `docker run -d -p 3621:3621 auguwu/api:latest`

- Run `docker build . -t api:latest`, which will create the `api` image using the `latest` tag
- Run `docker run -d -p 3621:3621 api:latest` to run the image under detached mode, which will run under the background.

## License
**API** is released under the MIT License, read [here](/LICENSE) for more information.
