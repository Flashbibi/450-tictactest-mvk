FROM azul/zulu-openjdk-alpine:25

LABEL org.opencontainers.image.source=https://github.com/Flashbibi/450-tictactest-mvk

# Zulu, weil gradle/gradle-daemon-jvm.properties toolchainVendor=AZUL verlangt
RUN apk add --no-cache bash git

RUN addgroup -g 1000 dev \
	&& adduser -D -u 1000 -G dev dev

USER dev
