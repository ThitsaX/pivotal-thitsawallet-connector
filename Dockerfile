# syntax=docker/dockerfile:1.7

FROM maven:3.9.6-eclipse-temurin-21 AS build
COPY .mvn /opt/app/.mvn/
WORKDIR /opt/app

ARG GITHUB_ACTOR

COPY pom.xml /opt/app/pom.xml
COPY license-header /opt/app/license-header
COPY implementation /opt/app/implementation/

RUN --mount=type=secret,id=github_token \
    set -eu; \
    mkdir -p /opt/app/.mvn; \
    SETTINGS="/opt/app/.mvn/settings.xml"; \
    TOKEN="$(cat /run/secrets/github_token)"; \
    trap 'rm -f "$SETTINGS"' EXIT; \
    printf '%s\n' \
      '<settings>' \
      '  <servers>' \
      '    <server>' \
      '      <id>github</id>' \
      "      <username>${GITHUB_ACTOR}</username>" \
      "      <password>${TOKEN}</password>" \
      '    </server>' \
      '  </servers>' \
      '</settings>' > "$SETTINGS"; \
    mvn -f /opt/app/pom.xml \
      -pl implementation/mod_thitsaworks_connector \
      -am clean package -DskipTests

FROM eclipse-temurin:21-jdk-alpine
WORKDIR /opt/app

COPY --from=build /opt/app/implementation/mod_thitsaworks_connector/target/app.jar /opt/app/app.jar
COPY docker-entrypoint.sh /opt/app/
RUN chmod +x /opt/app/docker-entrypoint.sh

EXPOSE 3003


ENV CONNECTOR_ID="thitsawallet"
ENV SUPPORTED_CURRENCIES="MMK"
ENV CONNECTOR_ILP_SECRET="1234"
ENV NATS_URL="nats://example.com:4222"
ENV FSPIOP_STREAM_NAME="PIVOTAL_FSPIOP"
ENV PIVOTAL_AUDIT_STREAM_NAME="PIVOTAL_AUDIT"
ENV CONNECTOR_FORCE_PATCH_ERROR="false"
ENV FSPIOP_PARTIES_URL="http://example.com:4001"
ENV FSPIOP_QUOTES_URL="http://example.com:4001"
ENV FSPIOP_TRANSFERS_URL="http://example.com:4001"
ENV FSPIOP_SWITCH_ID="hub"
ENV BACKEND_ENDPOINT="http://example.com:8081"
ENV BACKEND_API_TIMEOUT_MS=30000
ENV IS_PREFIX="false"
ENV REDIS_URL="redis://example.com:7379"
ENV REDIS_TTL_SECONDS=1200
ENV FEE_ENGINE_ENDPOINT="http://example.com:8082/"
ENV SDK_CONNECTOR_PORT_NO=8080
ENV OUTBOUND_ENDPOINT="http://example.com:4001"
ENV TRANSACTION_AMOUNT_LIMIT=0
ENV IS_CALCULATE_FEE=true


# FSPIOP JWS (hub-facing leg). Signing is off by default and is enabled per
# deployment; key material never appears here — it is read from Vault.
ENV FSPIOP_USE_JWS="false"
ENV VAULT_URL=""
ENV VAULT_ROLE=""
ENV VAULT_KUBERNETES_AUTH_PATH="kubernetes"
ENV VAULT_KV_MOUNT="secret"
ENV VAULT_JWS_KEY_PATH_PREFIX="pivotal/jwskey"
ENV VAULT_SERVICE_ACCOUNT_TOKEN_PATH="/var/run/secrets/kubernetes.io/serviceaccount/token"

ENV FSPIOP_USE_MUTUAL_TLS="false"
ENV FSPIOP_MTLS_CA_PATH=""
ENV FSPIOP_MTLS_CLIENT_CERT_PATH=""
ENV FSPIOP_MTLS_CLIENT_KEY_PATH=""
ENV FSPIOP_MTLS_RELOAD_INTERVAL_MS="60000"
ENV CONNECTOR_TO_TAZAMA_KAFKA_ENABLED=false
ENV CONNECTOR_TO_TAZAMA_KAFKA_BOOTSTRAP_SERVERS=""
ENV CONNECTOR_TO_TAZAMA_KAFKA_TOPIC=test-topic
ENV CONNECTOR_TO_TAZAMA_KAFKA_CLIENT_ID=connector-to-tazama
ENTRYPOINT ["/opt/app/docker-entrypoint.sh"]
