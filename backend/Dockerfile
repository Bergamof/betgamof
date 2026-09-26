FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle.kts detekt.yml ./
COPY gradle gradle
COPY buildSrc buildSrc
COPY domain/build.gradle.kts domain/
COPY application/build.gradle.kts application/
COPY adapters/http/build.gradle.kts adapters/http/
COPY adapters/persistence/build.gradle.kts adapters/persistence/
COPY adapters/odds/build.gradle.kts adapters/odds/
COPY adapters/demo/build.gradle.kts adapters/demo/
COPY app/build.gradle.kts app/
# Resolve dependencies in their own layer so code changes don't re-download them.
RUN ./gradlew --no-daemon --quiet :app:dependencies > /dev/null
COPY . .
RUN ./gradlew --no-daemon --quiet :app:installDist -x test

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S betgamof && adduser -S betgamof -G betgamof \
    && mkdir /data && chown betgamof:betgamof /data
WORKDIR /app
COPY --from=build /app/app/build/install/betgamof-backend ./
USER betgamof
ENV PORT=8080 \
    BETGAMOF_DB_PATH=/data/betgamof.db
VOLUME /data
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s \
    CMD wget -qO- http://localhost:8080/health || exit 1
CMD ["bin/betgamof-backend"]
