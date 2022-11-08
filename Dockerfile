# 🥀 hana: API to proxy different APIs like GitHub Sponsors, source code for api.floofy.dev
# Copyright (c) 2020-2022 Noel <cutie@floofy.dev>
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

# Build stage!
FROM eclipse-temurin:17.0.5_8-jdk-alpine AS builder

# Install common libraries we will need
RUN apk update && apk add --no-cache git

# Change the directory to `/build`
WORKDIR /build

# Bring all of our code into this worker
COPY . .

# Build a copy of hana (we only need the class libraries)
RUN ./gradlew installDist --stacktrace --no-daemon

# Now we're at the container stage.
FROM eclipse-temurin:17.0.5_8-jdk

# Install common libraries we need
RUN apt update && apt install bash tini
# RUN apk update && apk add --no-cache bash musl-dev libc-dev gcompat

# Change the working directory to `/app/noel/hana` to add
# our Docker scripts!
WORKDIR /app/noel/hana

# Copy the `docker/` folder to the directory we just changed to
COPY docker /app/noel/hana/scripts

# Copy the `assets/` folder to the image
COPY assets /app/noel/hana/assets

# Bring in the classpath!
COPY --from=builder /build/build/install/hana .

# Remove the `hana.bat` file since we are NOT on windows.
RUN rm /app/noel/hana/bin/hana.bat

# Give our scripts executable permissions
RUN chmod +x /app/noel/hana/scripts/docker-entrypoint.sh && chmod +x /app/noel/hana/scripts/runner.sh

# Now, we need to not be root for security reasons.
USER 1001

# Add in the entrypoint (which will be /docker-entrypoint.sh)
ENTRYPOINT ["/app/noel/hana/scripts/docker-entrypoint.sh"]
CMD ["/app/noel/hana/scripts/runner.sh"]
