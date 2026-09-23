FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts detekt.yml ./
COPY gradle gradle
# Resolve dependencies in their own layer so code changes don't re-download them.
RUN ./gradlew --no-daemon --quiet dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon --quiet installDist -x test

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S betgamof && adduser -S betgamof -G betgamof \
    && mkdir /data && chown betgamof:betgamof /data
WORKDIR /app
COPY --from=build /app/build/install/betgamof-backend ./
USER betgamof
ENV PORT=8080 \
    BETGAMOF_DB_PATH=/data/betgamof.db
VOLUME /data
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s \
    CMD wget -qO- http://localhost:8080/health || exit 1
CMD ["bin/betgamof-backend"]
