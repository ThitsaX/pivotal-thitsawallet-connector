#!/bin/sh
set -eu


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
    -jar app.jar