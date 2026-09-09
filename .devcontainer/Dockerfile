FROM alpine:3.23

LABEL org.opencontainers.image.source=https://github.com/Flashbibi/450-tictactest-mvk

# openjdk25 liegt in der community-Repo von Alpine 3.23
RUN apk add --no-cache openjdk25 bash git

RUN addgroup -g 1000 dev \
	&& adduser -D -u 1000 -G dev dev

USER dev
