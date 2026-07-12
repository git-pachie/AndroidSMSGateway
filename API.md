# Android SMS Gateway API

## Base URL

The embedded HTTP server binds to `0.0.0.0` on the configured port.

Example:

```text
http://DEVICE_IP:8080
```

Use this API only on a trusted LAN or private VPN. Traffic is cleartext HTTP.

## Authentication

- `GET /api/health` is public.
- All other routes require `Authorization: Bearer <api-key>`.
- Request IDs use `X-Request-ID`. The server returns a sanitized or generated value on every response.

## Response Envelope

Success:

```json
{
  "success": true,
  "data": {}
}
```

Error:

```json
{
  "success": false,
  "error": {
    "code": "INVALID_REQUEST",
    "message": "The request is invalid",
    "details": null
  },
  "requestId": "request-id",
  "timestamp": "2026-07-12T00:00:00Z"
}
```

## Endpoints

### `GET /api/health`

Public health summary for the gateway service, permissions, and webhook queue state.

### `GET /api/settings`

Returns safe settings only. API keys and secrets are never returned.

### `PUT /api/settings`

Updates device ID, webhook settings, limits, retry settings, and gateway options.

### `POST /api/settings/webhook`

Updates webhook URL, enablement, retry count, and secret handling.

Rules:

- Blank `webhookSecret` keeps the existing secret.
- Set `clearWebhookSecret=true` to delete the stored secret.
- Secrets are never returned by the API.

### `POST /api/webhook/test`

Sends a test webhook event using the fixed configured webhook URL.

- Does not create a fake inbox SMS record.
- Uses event type `sms.gateway.test`.
- Returns safe response details only.

### `POST /api/webhook/retry/{smsId}`

Enqueues a manual retry for one received SMS and returns `202 Accepted`.

- Rejects already forwarded records with `409 Conflict`.
- Schedules WorkManager unique work named `webhook-retry-{smsId}`.

### `GET /api/webhook/attempts`

Returns paged webhook attempt history.

Query parameters:

- `limit` default `50`, maximum `100`
- `offset` minimum `0`
- `smsId`
- `success`
- `responseCode`
- `dateFrom` ISO-8601 UTC timestamp
- `dateTo` ISO-8601 UTC timestamp
- `sortDirection` values: `ASC`, `DESC`

### `PUT /api/settings/server`

Updates the configured server port and indicates that a restart is required.

### `GET /api/logs/system`

Returns paged system logs.

Query parameters:

- `limit` default `50`, maximum `100`
- `offset` minimum `0`
- `level`
- `category`
- `sortDirection` values: `ASC`, `DESC`

### `GET /api/logs/audit`

Returns paged request audit logs.

Query parameters:

- `limit` default `50`, maximum `100`
- `offset` minimum `0`
- `sortDirection` values: `ASC`, `DESC`

### `POST /api/sms/send`

Accepts an outgoing SMS request and returns `202 Accepted` when queued.

Request:

```json
{
  "to": "+639171234567",
  "message": "Your OTP code is 123456",
  "clientReference": "OTP-000001",
  "subscriptionId": null
}
```

Notes:

- The API returns `PENDING` when accepted. It does not claim `SENT` immediately.
- Rate limiting uses HTTP `429` and may include `Retry-After`.
- Daily limits also return HTTP `429`.
- Allowed-prefix violations return `PREFIX_NOT_ALLOWED`.
- Delivery reports depend on carrier support.

### `GET /api/sms/status/{messageId}`

Returns the full authenticated status for one outgoing SMS, including:

- `messageId`
- `to`
- `message`
- `status`
- `createdAt`
- `sendingAt`
- `sentAt`
- `deliveredAt`
- `failedAt`
- `clientReference`
- `segmentCount`
- `subscriptionId`
- `simSlot`
- `errorCode`
- `errorMessage`

Returns `404` with `RECORD_NOT_FOUND` when absent.

### `GET /api/sms/sent`

Returns a paged list of sent SMS records.

Query parameters:

- `limit` default `50`, maximum `100`
- `offset` minimum `0`
- `status`
- `to`
- `clientReference`
- `dateFrom` ISO-8601 UTC timestamp
- `dateTo` ISO-8601 UTC timestamp
- `sortDirection` values: `ASC`, `DESC`

### `GET /api/sms/inbox`

Returns a paged list of received SMS records.

Query parameters:

- `limit` default `50`, maximum `100`
- `offset` minimum `0`
- `from`
- `forwardStatus`
- `dateFrom` ISO-8601 UTC timestamp
- `dateTo` ISO-8601 UTC timestamp
- `sortDirection` values: `ASC`, `DESC`

### `GET /api/sms/inbox/{smsId}`

Returns one received SMS detail record including webhook-related state fields such as retry counters, last/next attempt timestamps, and recent attempt summaries.

## Webhook Delivery

Incoming SMS content never selects a URL. The app only POSTs to the administrator-configured fixed webhook URL.

Headers:

- `Content-Type: application/json`
- `Authorization: Bearer <webhook-secret>` when configured
- `X-SMS-Gateway-Device: <device-id>`
- `X-SMS-Gateway-Event: sms.received`
- `X-Request-ID: <generated-id>`

Payload:

```json
{
  "eventType": "sms.received",
  "deviceId": "android-gateway-01",
  "from": "+639171234567",
  "message": "PAYMENT 12345",
  "receivedAt": "2026-07-11T21:30:00Z",
  "subscriptionId": 1,
  "simSlot": 1,
  "smsId": 9001
}
```

Success condition:

- Only HTTP `200` through `299` marks a message as `FORWARDED`.

Retry policy:

- Retryable failures: timeout, connection/DNS failure, HTTP `408`, HTTP `429`, HTTP `5xx`
- Permanent failures: invalid URL, TLS failure, and most HTTP `4xx`
- WorkManager enforces connected-network execution and exponential backoff
- Configured retry base delay is clamped to WorkManager minimum when necessary

Stable webhook error codes:

- `WEBHOOK_NOT_CONFIGURED`
- `WEBHOOK_DISABLED`
- `INVALID_WEBHOOK_URL`
- `WEBHOOK_UNREACHABLE`
- `WEBHOOK_TIMEOUT`
- `WEBHOOK_TLS_ERROR`
- `WEBHOOK_HTTP_ERROR`
- `WEBHOOK_RETRIES_EXHAUSTED`

### Incoming SMS Notes

- Multipart SMS received in one Android broadcast are reconstructed into one logical inbox record.
- Duplicate prevention uses a SHA-256 fingerprint over normalized sender, message body, subscription ID, and a 5-minute receive bucket.
- `from` preserves the original display sender, including alphanumeric business senders.
- SIM metadata may be unavailable on some devices or carriers; `subscriptionId` and `simSlot` may be `null`.

### SMS Error Codes

Common Phase 4 SMS errors:

- `SMS_PERMISSION_DENIED`
- `NO_SIM_AVAILABLE`
- `INVALID_SUBSCRIPTION`
- `SMS_NO_SERVICE`
- `PREFIX_NOT_ALLOWED`
- `RATE_LIMIT_EXCEEDED`
- `DAILY_SMS_LIMIT_EXCEEDED`
- `SMS_GENERIC_FAILURE`
- `SMS_RADIO_OFF`
- `SMS_NULL_PDU`
- `SMS_LIMIT_EXCEEDED`
- `SMS_FDN_CHECK_FAILURE`
- `SMS_SHORT_CODE_NOT_ALLOWED`
- `SMS_SHORT_CODE_NEVER_ALLOWED`
- `UNKNOWN_SMS_ERROR`

### Example Curl

```bash
curl -X POST "http://DEVICE_IP:8080/api/sms/send" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "to": "+639171234567",
    "message": "Test message from Android SMS Gateway",
    "clientReference": "manual-test-001"
  }'
```

## Security Warning

- Do not expose the LAN HTTP API directly to the public internet.
- Webhook secrets are stored locally and never returned.
- The Authorization header used for webhook forwarding is never logged or persisted.
