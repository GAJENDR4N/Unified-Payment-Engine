# Payment Engine

GoMobi's Payment Engine - scoped to two jobs only: **create a payment** and
**enquire its status**. Everything else (callbacks/IPN, audit trail,
settlement) is owned by the gateway layer or other platform services.

## What changed in this pass

- **Removed metrics.** No Micrometer/Prometheus. `PaymentMetrics` and the
  `management.metrics.*` config are gone. Actuator is kept but only exposes
  `health` and `info`.
- **Removed callback handling.** `CallbackController`, `CallbackHandlingService`,
  `CallbackPayload`/`CallbackResult`, `AuditService`, `PaymentAudit`, and
  `SignatureValidator` are all gone. Inbound provider callbacks/IPNs and the
  payment audit trail now live in the gateway layer, upstream of this service.
- **Scope narrowed to create + status enquiry.** See `PaymentController`
  (`POST /v1/payments`) and `PaymentStatusController`
  (`GET /v1/payments/{transactionId}/status`).
- **Single exception type.** `PaymentEngineException` + `ErrorCode` enum
  (code + `HttpStatus` + default message, grouped into numeric ranges -
  1xxx validation, 2xxx merchant/routing, 3xxx transaction state, 5xxx
  external provider, 8xxx auth, 9xxx system). `BusinessException`,
  `ExternalProviderException` and `ApiError` are gone; `ErrorResponse`
  replaces `ApiError`.
- **Entities for the pre-call persistence pattern.** `PaymentTransaction`
  and the new `QrTransactionDetails` map 1:1 to `PAYMENT_TRANSACTION` /
  `QR_TRANSACTION_DETAILS` from the v5 schema (`src/main/resources/db/unified_payments_schema_v5.sql`).
  Both are inserted with status `INITIATED` **before** the provider is
  called, then the transaction row is updated with the outcome - see
  `PaymentCreationService`.
- **Structured, readable logging** (`io.gomobi.payment.logging.HttpLoggingFilter`,
  `io.gomobi.payment.util.ProviderCallLog`, `io.gomobi.payment.util.DbPersistLog`) -
  all emit consistent `key=value` fields so external log tooling can
  filter/aggregate without a custom parser:
  - `event=INCOMING_REQUEST` / `event=INCOMING_REQUEST_BODY` / `event=OUTGOING_RESPONSE` (+ `durationMs`) for every HTTP call.
  - `event=CLIENT_REQUEST` / `event=CLIENT_RESPONSE` (+ `durationMs`) for every outbound provider call - **raw, unmasked**, since there's no separate audit trail anymore (see the tradeoff noted in `ProviderCallLog`'s javadoc - restrict log access accordingly).
  - `event=DB_PERSIST` (+ `durationMs`) around every transaction/QR-details save.
  - The previously-commented `LoggingAspect` is deleted; logging is explicit at each of these three points rather than generic AOP, since the ask was specifically about these three signals.

## Routing: how "host" is resolved

The Create Payment request doesn't name a host/provider explicitly - it
only carries `payment_method.channel_code`. `HostResolutionService`
resolves, in order:

1. `PAYMENT_METHOD` by `channel_code` (must be `ACTIVE`).
2. The `ACTIVE` provider(s) supporting it, via `PROVIDER_SUPPORTED_METHOD`.
   Today every seeded channel resolves to exactly one provider, so this is
   unambiguous. If a channel is ever configured with more than one active
   provider, the request fails fast with `AMBIGUOUS_PROVIDER_ROUTING`
   rather than guessing - that's the point to add an explicit routing rule.
3. The technical gateway adapter for that provider/host, via
   `payment.host-gateway-mapping` in `application.yml` (e.g. `MAYBANK: FIUU`,
   `PAYOK: PAYOK`). One gateway can serve multiple hosts.

**Flag for confirmation:** this assumes routing is fully config-driven off
`channel_code`, with no per-request host override. If the platform needs
callers to name a specific host explicitly (e.g. for A/B testing two
providers on the same channel), that's an additive change to
`CreatePaymentRequest` + `HostResolutionService`.

## Known open item: PAYOK adapter is a skeleton

`PayokClient`/`PayokPaymentAdapter` compile and run end-to-end against the
sample VietQR request below, but the actual PAYOK API contract (auth
scheme, field names, status codes) hasn't been confirmed - `PayokClient`
returns a deterministic `PENDING` response and is clearly marked with
`TODO`s. Wire it to a real HTTP call (mirroring `FiuuClient`'s
retry/circuit-breaker/raw-logging pattern) once the contract is confirmed.

## Also flagging: the idempotency key field name

The agreed contract spells it `idempontent_key`. `CreatePaymentRequest`
accepts that spelling plus the more conventional `idempotency_key` via
`@JsonAlias`, so a future fix on either side won't break the other -
worth confirming which one is actually correct.

## Running locally

```bash
mvn spring-boot:run
```

Defaults to the `dev` profile (`application-dev.yml`): local MySQL on
`localhost:3306/payment_engine`, `ddl-auto: update`. Load
`src/main/resources/db/unified_payments_schema_v5.sql` first (or let
Hibernate create the tables and run just the `INSERT` statements at the
bottom) - it includes seed data for a demo merchant/sub-merchant matching
the request below.

### Create a payment (VietQR / PAYOK)

`amount` is sent in minor units (integer, no decimal point). Example: `10000` means `100.00`.

```bash
curl -X POST http://localhost:8080/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "idempontent_key": "IDEM-100001",
    "reference_id": "ORDER-100001",
    "global_account_id": "GA000001",
    "master_mid": "MID000001",
    "sub_merchant_mid": "SUB000001",
    "amount": 150000,
    "currency": "VND",
    "description": "Order #100001",
    "payment_method": { "type": "QR", "channel_code": "VN_VIETQR" },
    "customer": { "id": "CUS0001", "name": "John Doe", "email": "john@example.com", "phone": "+84123456789" },
    "metadata": { "store": "HN01", "terminal": "POS01" }
  }'
```

### Check status

```bash
curl http://localhost:8080/v1/payments/{transaction_id}/status
```

(`transaction_id` comes back in the create response, e.g. `TXN-1751...`.)

## API docs

Swagger UI: `http://localhost:8080/swagger-ui.html` (disabled in prod).
