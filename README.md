# Pivotal ThitsaWallet Connector

This connector is a DFSP-specific integration layer that connects to Pivotal, the multi-tenant payment adapter for Mojaloop. For ThitsaWallet, it connects the Hub side of the Mojaloop/FSPIOP flow to ThitsaWallet's core banking system and APIs. If a developer needs to integrate with Bank A, Bank B, Wallet A, or MFI C, they should create a similar repo and customize the connector to match that DFSP's APIs and core banking flows. In other words, this repository is tailored for one DFSP implementation at a time.

The FSPIOP follow APIs are maintained in the separate repository:

- [ThitsaX/pivotal-connector](https://github.com/ThitsaX/pivotal-connector)

This repo consumes the published follow API jar from GitHub Packages and focuses on the connector runtime and integration logic.

## What This Connector Does

- consumes Hub requests from NATS JetStream
- performs party lookup against ThitsaWallet
- calculates quotes with ThitsaWallet and the fee engine
- reserves transfers and stores pending state in Redis
- confirms committed transfers
- publishes FSPIOP callback responses back to the Hub
- publishes audit events for patch-transfer errors

## Repository Layout

This repo has one Maven module:

- `implementation/mod_thitsaworks_connector` - the Spring Boot connector application

Supporting project files:

- `Dockerfile` - container build for the connector
- `docker-entrypoint.sh` - maps environment variables to JVM system properties
- `license-header` - shared license header used by the formatter plugin
- `implementation/openapitools.json` - OpenAPI generator settings

## Requirements

- Java 21
- Maven 3.9+
- NATS with JetStream enabled
- Redis
- ThitsaWallet backend access
- Fee engine access
- Hub/FSPIOP callback endpoint
- Access to GitHub Packages for the `mod-pivotal-connector-api` dependency

## Build

From the repository root:

```bash
mvn clean install
```

To build only the runnable connector module:

```bash
mvn -pl implementation/mod_thitsaworks_connector -am clean package
```

The main application jar is produced at:

```text
implementation/mod_thitsaworks_connector/target/app.jar
```

## Run

### Run With Java

```bash
java \
  -DconnectorId=thitsawallet \
  -DsupportedCurrencies=MMK \
  -DconnectorIlpSecret=1234 \
  -DnatsUrl=nats://example.com:4222 \
  -DfspiopPartiesUrl=http://example.com:4002 \
  -DfspiopQuotesUrl=http://example.com:3002 \
  -DfspiopTransfersUrl=http://example.com:3000 \
  -DbackendEndpoint=http://example.com:8081 \
  -DfeeEngineEndpoint=http://example.com:8082 \
  -DredisUrl=redis://example.com:6379 \
  -DsdkConnectorPortNo=8080 \
  -cp "implementation/mod_thitsaworks_connector/target/app.jar:implementation/mod_thitsaworks_connector/target/lib/*" \
  com.thitsaworks.mojaloop.coreconnector.ThitsaWalletConnectorApplication
```

### Run With Docker

The Docker image maps environment variables to JVM system properties through `docker-entrypoint.sh`.

The build needs GitHub Packages access because the connector resolves `mod-pivotal-connector-api` from:

```text
https://maven.pkg.github.com/thitsax/pivotal-java-connector
```

Docker build example:

```bash
docker build \
  --secret id=github_token,src=/path/to/github_token.txt \
  --build-arg GITHUB_ACTOR=your-github-username \
  -t pivotal-thitsawallet-connector .
```

Common defaults:

- `CONNECTOR_ID=thitsawallet`
- `SUPPORTED_CURRENCIES=MMK`
- `NATS_URL=nats://example.com:4222`
- `REDIS_URL=redis://example.com:7379`
- `SDK_CONNECTOR_PORT_NO=8080`

## Configuration

| System property | Docker env var | Default | Description |
| --- | --- | --- | --- |
| `connectorId` | `CONNECTOR_ID` | `thitsawallet` | Connector / payee FSP ID |
| `supportedCurrencies` | `SUPPORTED_CURRENCIES` | `MMK` | Comma-separated supported currencies |
| `connectorIlpSecret` | `CONNECTOR_ILP_SECRET` | `1234` | ILP secret for packet generation |
| `natsUrl` | `NATS_URL` | `nats://example.com:4222` | NATS connection URL |
| `fspiopStreamName` | `FSPIOP_STREAM_NAME` | `PIVOTAL_FSPIOP` | JetStream stream for FSPIOP messages |
| `pivotalAuditStreamName` | `PIVOTAL_AUDIT_STREAM_NAME` | `PIVOTAL_AUDIT` | JetStream stream for audit events |
| `connectorForcePatchError` | `CONNECTOR_FORCE_PATCH_ERROR` | `false` | Forces patch error handling for testing |
| `fspiopPartiesUrl` | `FSPIOP_PARTIES_URL` | `http://example.com:4002` | Hub callback base URL for parties |
| `fspiopQuotesUrl` | `FSPIOP_QUOTES_URL` | `http://example.com:3002` | Hub callback base URL for quotes |
| `fspiopTransfersUrl` | `FSPIOP_TRANSFERS_URL` | `http://example.com:3000` | Hub callback base URL for transfers |
| `fspiopSwitchId` | `FSPIOP_SWITCH_ID` | `hub` | FSPIOP switch identifier |
| `backendEndpoint` | `BACKEND_ENDPOINT` | `http://example.com:8081` | ThitsaWallet backend base URL |
| `backendApiTimeoutMs` | `BACKEND_API_TIMEOUT_MS` | `30000` | Backend timeout in milliseconds |
| `isPrefix` | `IS_PREFIX` | `false` | Remove mobile/account prefixes before backend calls |
| `redisUrl` | `REDIS_URL` | `redis://example.com:6379` | Redis connection URL |
| `redisTtlSeconds` | `REDIS_TTL_SECONDS` | `1200` | Pending transfer TTL in seconds |
| `feeEngineEndpoint` | `FEE_ENGINE_ENDPOINT` | `http://example.com:8082` | Fee engine base URL |
| `sdkConnectorPortNo` | `SDK_CONNECTOR_PORT_NO` | `8080` | HTTP port used by the Spring Boot app |
| `transactionAmountLimit` | `TRANSACTION_AMOUNT_LIMIT` | `0` | Threshold for transaction scenario logic |
| `outboundEndpoint` | `OUTBOUND_ENDPOINT` | `http://example.com:4001` | FSPIOP callback base URL used by the connector |

## Message Flow

The connector listens on NATS subjects built from the connector ID:

- `fspiop.<connectorId>.get.parties`
- `fspiop.<connectorId>.post.quotes`
- `fspiop.<connectorId>.post.transfers`
- `fspiop.<connectorId>.patch.transfers`

Flow summary:

1. Hub publishes a request to NATS.
2. The connector consumes the request from JetStream.
3. The connector calls ThitsaWallet and, when needed, the fee engine.
4. The connector sends the FSPIOP callback response back to the Hub.

For transfers:

- the reservation step stores pending data in Redis
- the patch step confirms the transfer when the state is `COMMITTED`
- failed patch handling can publish an audit event to NATS

## Logging

Logging is handled by the connector's runtime configuration.

## Notes

- Main entrypoint: `com.thitsaworks.mojaloop.coreconnector.ThitsaWalletConnectorApplication`
- Default connector ID: `thitsawallet`
- Default switch ID: `hub`
- If you need the FSPIOP follow API jar, use the separate `ThitsaX/pivotal-connector` repository
- The shared license header file lives at the repo root and is copied into modules for the formatter plugin

## License

See [`LICENSE`](LICENSE) and [`NOTICE`](NOTICE).
