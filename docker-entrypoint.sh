#!/bin/sh
set -eu


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
    -jar app.jar