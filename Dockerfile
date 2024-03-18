FROM openjdk:17.0-slim AS base
ENV LC_ALL=C.UTF-8
ENV LANG=C.UTF-8
RUN groupadd -r evident \
    && useradd --no-log-init -r -g evident -G adm -s /bin/bash evident \
    && install -d -o evident -g evident -m 0750 /opt/eid

FROM base as artifact
# build arguments
ARG BUILD_NUMBER
ARG BUILD_TAG
ARG BUILD_HASH
ARG APP_VERSION
ARG JDBC_URL=''
# environment
ENV BUILD_NUMBER=${BUILD_NUMBER:-"0"}
ENV BUILD_TAG=${BUILD_TAG:-"0"}
ENV BUILD_HASH=${BUILD_HASH:-"0"}
ENV APP_VERSION=${APP_VERSION:-"0"}
ENV JDBC_URL=${JDBC_URL:-"jdbc:postgresql://localhost/evident?user=USER&password=PASSWORD&ssl=true&sslmode=require"}
ENV SENTRY_DSN=""
ENV SENTRY_ENVIRONMENT=""
ENV LC_ALL=C.UTF-8
ENV LANG=C.UTF-8
WORKDIR /opt/eid/eid-application
COPY ./target/scala-2.13/eid-application-assembly-*.jar ./eid-application-app.jar
EXPOSE 8080/tcp
CMD ["java", "-jar", "eid-scala-app.jar"]
