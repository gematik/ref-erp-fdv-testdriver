#!/usr/bin/env sh
set -e

ARGS="-Djdk.tls.namedGroups=secp256r1,secp384r1,ffdhe2048,ffdhe3072"

[ -n "$HTTPS_PROXY_HOST" ] && ARGS="$ARGS -Dhttps.proxyHost=$HTTPS_PROXY_HOST"
[ -n "$HTTPS_PROXY_PORT" ] && ARGS="$ARGS -Dhttps.proxyPort=$HTTPS_PROXY_PORT"

exec java $ARGS -jar /app/app.jar
