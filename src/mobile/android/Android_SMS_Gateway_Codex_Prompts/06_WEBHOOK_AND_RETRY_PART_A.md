Continue the existing Android SMS Gateway project.
Implement Phase 6: webhook forwarding, secure payload, attempt history, WorkManager retry queue, network recovery, manual retry API, and webhook test API.
Preserve all previous phases.
Create actual files, run the build, and fix errors.
## 1. Webhook safety rule
This rule is mandatory:
Incoming SMS content must never choose a URL.
Never:
- Parse a URL from the SMS and call it.
- Execute a command from SMS text.
- Create a generic URL execution endpoint.
- Allow sender-controlled request targets.
Only send incoming SMS data to the fixed webhook URL configured by the administrator.
## 2. Webhook forwarder
Implement `WebhookForwarder`.
Use Ktor client or OkHttp.
Requirements:
- Load current webhook settings.
- Return disabled result when webhook is disabled.
- Validate configured URL.
- Enforce HTTPS when required.
- Use connection timeout.
- Use request timeout.
- Use read timeout.
- Send JSON.
- Handle DNS failure.
- Handle TLS failure.
- Handle timeout.
- Handle connection failure.
- Treat only HTTP 200 through 299 as success.
- Truncate stored response summary.
- Never log the secret.
- Never store Authorization header.
## 3. Webhook request
Send:
- Method: POST
- URL: configured fixed URL
- Content-Type: application/json
- `Authorization: Bearer WEBHOOK_SECRET` when configured
- `X-SMS-Gateway-Device: DEVICE_ID`
- `X-SMS-Gateway-Event: sms.received`
- `X-Request-ID: UNIQUE_ID`
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
Use ISO 8601 UTC timestamps.
## 4. Optional HMAC
Implement HMAC only if it is clear and secure.
If implemented:
- Use HMAC-SHA256.
- Sign the exact UTF-8 request body.
- Use webhook secret.
- Send:
  `X-SMS-Gateway-Signature: sha256=HEX_VALUE`
- Use deterministic serialization for the signed body.
- Document verification.
Do not make HMAC mandatory unless settings include enable/disable.
If not implemented, document Bearer authentication only.
## 5. Forwarding lifecycle
For every received SMS:
1. Message already exists in database.
2. If webhook disabled, leave clear status and do not fake success.
3. If enabled, set RETRYING or PENDING as appropriate.
4. Create attempt record.
5. Send request.
6. Record duration.
7. Record response code.
8. Record truncated response summary.
9. On 2xx:
   - Mark attempt success.
   - Mark received SMS FORWARDED.
   - Clear next retry.
   - Clear latest error.
10. On failure:
   - Mark attempt failure.
   - Increment retry count.
   - Set safe error.
   - Schedule retry if allowed.
   - Mark FAILED when retries exhausted.
Use transactions where status and attempt history must remain consistent.
## 6. Retry worker
Create `WebhookRetryWorker`.
Use WorkManager.
Requirements:
- Network connectivity constraint
- Exponential backoff
- Unique work per received SMS ID
- No duplicate concurrent retries
- Persist state
- Load latest settings at execution time
- Stop after configured maximum
- Handle app restart
- Handle device reboot
- Return retry only when appropriate
- Return failure after permanent exhaustion
- Return success after 2xx
Use unique name:
`webhook-retry-{smsId}`
## 7. Retry timing
Default:
- Maximum retries: 5
- Base delay: 30 seconds or nearest safe WorkManager value
Respect WorkManager minimum backoff rules.
If the configured value is below the platform minimum:
- Clamp it.
- Show effective value in logs or settings response.
- Document the limitation.
Calculate and persist `nextRetryAt` as an estimate.
## 8. Immediate scheduling
Update `SmsReceiver` flow.
After local insert:
- If webhook enabled, enqueue unique work.
- Do not perform long network work inside BroadcastReceiver.
- Use scheduler abstraction.
- Ensure message is saved before enqueue.
- Do not enqueue duplicate work.
If webhook disabled:
- Keep message stored.
- Use a clear status.
- Do not mark FORWARDED.
Choose and document whether disabled messages remain PENDING or use a new `DISABLED` state.
If adding `DISABLED`, create migration and update contracts consistently.
## 9. Permanent and retryable errors
Retryable:
- Timeout
- DNS temporary failure
- Connection failure
- HTTP 408
- HTTP 429
- HTTP 5xx
Potentially permanent:
- Invalid URL
- Unsupported scheme
- TLS certificate failure
- HTTP 400
- HTTP 401
- HTTP 403
- HTTP 404
Implement a reasonable policy.
Do not retry permanent errors endlessly.
Store stable codes:
- `WEBHOOK_NOT_CONFIGURED`
- `WEBHOOK_DISABLED`
- `INVALID_WEBHOOK_URL`
- `WEBHOOK_UNREACHABLE`
- `WEBHOOK_TIMEOUT`
- `WEBHOOK_TLS_ERROR`
- `WEBHOOK_HTTP_ERROR`
- `WEBHOOK_RETRIES_EXHAUSTED`
## 10. POST /api/webhook/test
Authentication required.
Behavior:
- Load current webhook config.
- Validate enabled/configured state.
- Send a test payload.
- Do not create a fake incoming SMS.
- Return:
  - success
  - response code
  - duration
  - safe response summary
  - error code
- Do not return secret.
- Do not log Authorization header.
Test payload may use:
- eventType `sms.gateway.test`
- current device ID
- current timestamp
- generated test ID
## 11. POST /api/webhook/retry/{smsId}
Authentication required.
Rules:
- Record must exist.
- Allow retry for FAILED or pending states.
- Reject already FORWARDED unless explicit force is supported.
- Prefer no force in MVP.
- Enqueue unique work.
- Return HTTP 202.
- Return current retry state.
- Do not execute synchronously inside route.
## 12. GET /api/webhook/attempts
Authentication required.
Filters:
- smsId
- success
- responseCode
- dateFrom
- dateTo
- limit
- offset
- sortDirection
Maximum limit:
100
Use database-backed paging.
## 13. GET /api/sms/inbox/{smsId}
Extend response.
Include attempt history summary:
- attempt ID
- attempt number
- response code
- success
- duration
- error code
- attemptedAt
Do not return secret or Authorization data.
## 14. Dashboard
Update:
- Webhook enabled
- Webhook URL preview
- Pending count
- Failed count
- Last successful webhook time
- Last failed webhook time
- Retry queue status
Add Test Webhook action.
## 15. Received detail UI
Implement:
- Attempt history list
- Manual retry button
- Retry progress
- Error summary
- Response code
- Last attempt
- Next retry
Disable retry button while unique work is active.
Show clear state when webhook is disabled.
## 16. Settings behavior
Webhook secret:
- Existing secret is never displayed.
- Blank input keeps existing secret.
- Separate Clear Secret action.
- Confirmation before clear.
- Saving URL does not erase secret.
- Disabling webhook does not automatically erase secret.
Webhook URL:
- Validate.
- Warn on non-HTTPS.
- Reject non-HTTPS when required.
## 17. Logging
Log:
- Attempt start
- Attempt success
- Attempt failure
- Retry scheduled
- Retries exhausted
- Webhook disabled
- Invalid URL
Do not log:
- Secret
- Authorization header
- Complete payload in release
- Complete response body
Truncate response summary.
