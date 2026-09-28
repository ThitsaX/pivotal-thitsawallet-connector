#!/bin/sh
set -eu

# How the JVM is started. Unchanged unless this deployment signs through a device: `java -jar`
# ignores -cp, and the vendor's provider arrives as a separate jar in the image rather than inside
# app.jar -- so reaching it means launching Spring Boot's loader by name instead. The plain form is
# kept for every other deployment, so a profile that needs none of this cannot be broken by it.
LAUNCH="-jar app.jar"

if [ -n "${CLOUDHSM_IP:-}" ]; then

    # The provider reads its cluster address from its own configuration file, which ships with a
    # placeholder. Writing it here rather than baking it in because the address differs per
    # environment, and generating it with the vendor's tool because the file's format is theirs.
    /opt/cloudhsm/bin/configure-jce -a "$CLOUDHSM_IP"

    LAUNCH="-cp /opt/app/app.jar:/opt/cloudhsm/java/* org.springframework.boot.loader.launch.JarLauncher"

    echo "CloudHSM client configured for $CLOUDHSM_IP."
fi


# FSPIOP JWS and mutual TLS (hub-facing leg). Both are off unless explicitly enabled, and every
# variable carries a default, so a deployment that sets none of them behaves as before.
# Key material is never passed here: the signing key comes from Vault, and the client certificate
# is read from a mounted Secret so a renewal reaches the connector without a new image.
exec java \
    "-DconnectorId=${CONNECTOR_ID}" \
    "-DsupportedCurrencies=${SUPPORTED_CURRENCIES}" \
    "-DconnectorIlpSecret=${CONNECTOR_ILP_SECRET}" \
    "-DnatsUrl=${NATS_URL}" \
    "-DfspiopStreamName=${FSPIOP_STREAM_NAME}" \
    "-DpivotalAuditStreamName=${PIVOTAL_AUDIT_STREAM_NAME}" \
    "-DconnectorForcePatchError=${CONNECTOR_FORCE_PATCH_ERROR}" \
    "-DoutboundEndpoint=${OUTBOUND_ENDPOINT}" \
    "-DfspiopPartiesUrl=${FSPIOP_PARTIES_URL}" \
    "-DfspiopQuotesUrl=${FSPIOP_QUOTES_URL}" \
    "-DfspiopTransfersUrl=${FSPIOP_TRANSFERS_URL}" \
    "-DfspiopSwitchId=${FSPIOP_SWITCH_ID}" \
    "-DbackendEndpoint=${BACKEND_ENDPOINT}" \
    "-DbackendApiTimeoutMs=${BACKEND_API_TIMEOUT_MS}" \
    "-DisPrefix=${IS_PREFIX}" \
    "-DfeeEngineEndpoint=${FEE_ENGINE_ENDPOINT}" \
    "-DredisUrl=${REDIS_URL}" \
    "-DredisTtlSeconds=${REDIS_TTL_SECONDS}" \
    "-DsdkConnectorPortNo=${SDK_CONNECTOR_PORT_NO}" \
    "-DtransactionAmountLimit=${TRANSACTION_AMOUNT_LIMIT}" \
    "-DisCalculateFee=${IS_CALCULATE_FEE}" \
    "-DfspiopUseJws=${FSPIOP_USE_JWS:-false}" \
    "-DvaultUrl=${VAULT_URL:-}" \
    "-DvaultRole=${VAULT_ROLE:-}" \
    "-DvaultKubernetesAuthPath=${VAULT_KUBERNETES_AUTH_PATH:-kubernetes}" \
    "-DvaultKvMount=${VAULT_KV_MOUNT:-secret}" \
    "-DvaultJwsKeyPathPrefix=${VAULT_JWS_KEY_PATH_PREFIX:-pivotal/jwskey}" \
    "-DvaultServiceAccountTokenPath=${VAULT_SERVICE_ACCOUNT_TOKEN_PATH:-/var/run/secrets/kubernetes.io/serviceaccount/token}" \
    "-DfspiopUseMutualTls=${FSPIOP_USE_MUTUAL_TLS:-false}" \
    "-DfspiopMtlsCaPath=${FSPIOP_MTLS_CA_PATH:-}" \
    "-DfspiopMtlsClientCertPath=${FSPIOP_MTLS_CLIENT_CERT_PATH:-}" \
    "-DfspiopMtlsClientKeyPath=${FSPIOP_MTLS_CLIENT_KEY_PATH:-}" \
    "-DfspiopMtlsReloadIntervalMs=${FSPIOP_MTLS_RELOAD_INTERVAL_MS:-60000}" \
    "-DconnectorToTazamaKafkaEnabled=${CONNECTOR_TO_TAZAMA_KAFKA_ENABLED}" \
    "-DconnectorToTazamaKafkaBootstrapServers=${CONNECTOR_TO_TAZAMA_KAFKA_BOOTSTRAP_SERVERS}" \
    "-DconnectorToTazamaKafkaTopic=${CONNECTOR_TO_TAZAMA_KAFKA_TOPIC}" \
    "-DconnectorToTazamaKafkaClientId=${CONNECTOR_TO_TAZAMA_KAFKA_CLIENT_ID}" \
    "-DkeyProvider=${KEY_PROVIDER:-vault-kv}" \
    "-DhsmCredPath=${HSM_CRED_PATH:-}" \
    "-DkeyRefPathPrefix=${KEY_REF_PATH:-pivotal/keyref}" \
    $LAUNCH