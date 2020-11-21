# API · [![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/) ![Workflow Staatus](https://github.com/auguwu/API/workflows/ktlint/badge.svg)
> :black_heart: **| API service to do certain things, mainly for webhooks and proxying.**

## Installation Guide
### Requirements
- Sentry (optional)
- JDK 11 (required)
- Git (required)

### Process
- [Fork](https://github.com/auguwu/API/fork) the repository under your username.
- Clone the repository (``git clone https://github.com/USERNAME/API``); omit `USERNAME` with your actual username.
- Change the directory to the newly cloned repository (``cd API``)
- Run `./gradlew build` if you're using Unix; run `gradlew build` if you're using Windows
- Copy the configuration under `config.example.yml` and copy it to make a new file called `config.yml`
  - This has to be placed where you build the JAR file, so `build/libs` is where it'll be at!
- Run the JAR file with `java -jar API-version.jar`; omit `version` with the version listed under [build.gradle.kts](/build.gradle.kts)

## License
**API** is released under the MIT License, read [here](/LICENSE) for more information.
