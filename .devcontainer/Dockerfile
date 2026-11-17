FROM azul/zulu-openjdk-alpine:25

LABEL org.opencontainers.image.source=https://github.com/Flashbibi/450-tictactest-mvk
LABEL org.opencontainers.image.description="DevContainer for TicTacTest: Alpine, Azul Zulu JDK 25, bash, git, ttyd, nginx, curl"

# Zulu, weil gradle/gradle-daemon-jvm.properties toolchainVendor=AZUL verlangt
# ttyd, nginx und curl für die Web-Konsole (webkonsole/start.sh, siehe WEBKONSOLE.md)
RUN apk add --no-cache bash git ttyd nginx curl

RUN addgroup -g 1000 dev \
	&& adduser -D -u 1000 -G dev dev

USER dev
