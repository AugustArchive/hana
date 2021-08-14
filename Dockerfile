FROM node:16-alpine

LABEL MAINTAINER="Noel <cutie@floofy.dev>"
RUN apk update && apk add git ca-certificates

WORKDIR /opt/hana
COPY . .
RUN apk add --no-cache git
RUN npm i -g typescript eslint typeorm
RUN npm ci
RUN yarn build:no-lint
RUN yarn cache clean
RUN rm -rf src

ENTRYPOINT [ "yarn", "start" ]
