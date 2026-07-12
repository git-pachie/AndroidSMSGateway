Continue the existing Android SMS Gateway project.
Implement Phase 3: embedded Ktor HTTP server, authentication, middleware, health endpoint, settings API, logs API, paging, and audit logging.
Preserve all previous phases.
Create actual files, run the build, and fix errors.
## 1. HTTP server
Implement `GatewayHttpServer`.
Requirements:
- Embedded Ktor server
- Bind to `0.0.0.0`
- Configurable port from settings
- Default port 8080
- Thread-safe start
- Thread-safe stop
- Prevent duplicate instances
- Detect port-in-use
- Controlled restart support
- JSON responses only
- Kotlin Serialization
- Timeouts where relevant
- Safe exception handling
- No stack traces in API responses
Integrate server lifecycle with `SmsGatewayService`.
The foreground service is considered RUNNING only after the server binds successfully.
## 2. Service behavior
Update `SmsGatewayService`.
Start flow:
1. Set state STARTING.
2. Load settings.
3. Start foreground notification promptly.
4. Start Ktor server off the main thread.
5. Bind configured port.
6. Set state RUNNING.
7. Persist `serverEnabled = true`.
8. Log server started.
Failure flow:
1. Set state ERROR.
2. Persist `serverEnabled = false`.
3. Log safe error details.
4. Update notification.
5. Stop foreground mode if appropriate.
Stop flow:
1. Set state STOPPING.
2. Stop accepting requests.
3. Stop Ktor safely.
4. Set state STOPPED.
5. Persist `serverEnabled = false`.
6. Log server stopped.
7. Stop foreground service.
## 3. Response envelope
Create consistent response models.
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
  "timestamp": "2026-07-11T21:30:00Z"
}
```
Use ISO 8601 UTC timestamps.
Do not expose internal exception text unless safely mapped.
## 4. Request ID middleware
For every request:
- Read optional `X-Request-ID`.
- Validate allowed characters.
- Enforce a safe maximum length.
- Generate a secure unique ID when absent or invalid.
- Return it in `X-Request-ID`.
- Include it in error responses.
- Make it available to logging.
Do not trust arbitrary large caller values.
## 5. Authentication middleware
All endpoints except `/api/health` require:
`Authorization: Bearer API_KEY`
Requirements:
- Reject missing header.
- Reject malformed scheme.
- Reject invalid token.
- Use API key manager verification.
- Never log token.
- Use constant-time comparison.
- Return HTTP 401.
- Use stable codes:
  - `MISSING_API_KEY`
  - `INVALID_API_KEY`
Do not reveal whether the key format or hash matched partially.
## 6. Audit middleware
Record each request after completion.
Store:
- Method
- Path
- Remote address when available
- Response code
- Duration
- Whether authentication succeeded
- Timestamp
Never store:
- Authorization header
- API key
- Request body
- Full SMS content
- Webhook secret
Audit logging failure must not fail the request.
## 7. Error mapping
Implement stable HTTP mapping.
Use:
- 400 invalid input
- 401 missing or invalid authentication
- 403 blocked operation
- 404 missing record
- 409 state conflict
- 429 rate limit
- 500 internal error
- 503 temporarily unavailable
Add codes:
- `INVALID_REQUEST`
- `RECORD_NOT_FOUND`
- `PORT_IN_USE`
- `SERVER_START_FAILED`
- `DATABASE_ERROR`
- `INTERNAL_ERROR`
- `INVALID_WEBHOOK_URL`
- `WEBHOOK_NOT_CONFIGURED`
- `WEBHOOK_DISABLED`
## 8. GET /api/health
No authentication.
Return safe operational information:
- status
- deviceId
- serverTime
- version
- smsPermission
- receiveSmsPermission
- notificationPermission
- batteryOptimizationDisabled
- simAvailable if determinable without extra permission
- webhookEnabled
- pendingWebhookCount
Status values:
- running
- degraded
- stopped
Do not expose:
- API hash
- API key
- Webhook secret
- Internal file paths
- Stack traces
If required permissions or SIM are missing while server runs, report degraded.
## 9. GET /api/settings
Authentication required.
Return only safe settings:
- deviceId
- serverPort
- webhookEnabled
- webhookUrl
- webhookSecretConfigured
- apiKeyConfigured
- allowedPrefixes as a list
- rateLimitPerMinute
- dailySmsLimitEnabled
- dailySmsLimit
- maxRetryCount
- retryBaseDelaySeconds
- autoStartEnabled
- requireHttpsWebhook
Never return:
- API key hash
- Raw API key
- Webhook secret
- Encrypted secret
- Keystore metadata
## 10. PUT /api/settings
Authentication required.
Support safe updates:
- deviceId
- webhook URL
- webhook enabled
- allowed prefixes
- rate limit
- daily limit enabled
- daily limit
- max retries
- base retry delay
- auto start
- require HTTPS webhook
Rules:
- Validate all input.
- Use partial update semantics only if clearly implemented.
- Reject invalid server port in this endpoint unless port support is implemented.
- Do not accept API key hash.
- Do not accept encrypted secrets.
- Do not overwrite webhook secret when omitted.
- Return sanitized updated settings.
## 11. POST /api/settings/webhook
Authentication required.
Request may contain:
- webhookUrl
- enabled
- webhookSecret
- maxRetryCount
Rules:
- Require valid URL when enabling.
- Enforce HTTPS when configured.
- Never log secret.
- Encrypt secret before storage.
- Omitted secret keeps existing secret.
- Empty secret must not silently clear.
- Explicit clear operation may be a separate boolean.
- Return only masked/safe settings.
## 12. Server port update
Provide a controlled mechanism.
Preferred endpoint:
`PUT /api/settings/server`
Request:
- serverPort
- restartNow
Rules:
- Validate 1024 to 65535.
- Save only after validation.
- If restartNow is false, indicate restart required.
- If true, schedule controlled restart after response is sent.
- Do not terminate the current request before returning.
- If restart fails, log clearly.
- UI will later provide confirmation.
If reliable self-restart cannot be completed in this phase, persist the new port and document that service restart is required.
## 13. GET /api/logs/system
Authentication required.
Filters:
- limit
- offset
- level
- category
- dateFrom
- dateTo
- sortDirection
Defaults:
- limit 50
- offset 0
- descending date
Maximum limit:
100
Use database-backed filtering and paging.
Response includes:
- items
- limit
- offset
- returned
- total
Do not expose sensitive details.
## 14. GET /api/logs/audit
Authentication required.
Support paging and date filtering.
Return safe audit fields only.
Do not return remote address if privacy mode later disables it.
