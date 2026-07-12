Continue the existing Android SMS Gateway project.
Implement Phase 2: Room database, repositories, settings initialization, API key security, limits, and core validation.
Do not recreate the project.
Inspect the existing repository first and preserve Phase 1.
Create actual files and run the build.
## 1. Architecture rules
Use:
- Room
- DAO interfaces
- Repository interfaces in the domain layer
- Repository implementations in the data layer
- Coroutines
- Flow
- Database transactions where needed
- Hilt dependency injection
Do not:
- Access DAOs directly from Composables
- Run database work on the main thread
- Use destructive migrations in release builds
- Store API keys in plain text
- Store webhook secrets unprotected
- Log secrets
## 2. Database
Create `AppDatabase`.
Enable Room schema export.
Add a schema directory to source control.
Start database version at 1 unless the project already has a database.
Provide migration infrastructure.
Do not use fallback-to-destructive-migration for release.
## 3. sent_sms table
Create `SentSmsEntity`.
Fields:
- `id: Long`, primary key, auto-generated
- `toNumber: String`
- `message: String`
- `clientReference: String?`
- `status: String`
- `errorCode: String?`
- `errorMessage: String?`
- `createdAt: Long`
- `sendingAt: Long?`
- `sentAt: Long?`
- `deliveredAt: Long?`
- `failedAt: Long?`
- `retryCount: Int`
- `segmentCount: Int`
- `subscriptionId: Int?`
- `simSlot: Int?`
Statuses:
- PENDING
- SENDING
- SENT
- DELIVERED
- FAILED
- UNKNOWN
Create indexes for:
- createdAt
- status
- clientReference
- toNumber
## 4. received_sms table
Create `ReceivedSmsEntity`.
Fields:
- `id: Long`, primary key, auto-generated
- `fromNumber: String`
- `message: String`
- `receivedAt: Long`
- `subscriptionId: Int?`
- `simSlot: Int?`
- `forwardStatus: String`
- `webhookResponseCode: Int?`
- `webhookResponseBody: String?`
- `lastForwardAttemptAt: Long?`
- `nextRetryAt: Long?`
- `retryCount: Int`
- `errorCode: String?`
- `errorMessage: String?`
- `messageFingerprint: String?`
Forward statuses:
- PENDING
- RETRYING
- FORWARDED
- FAILED
Create indexes for:
- receivedAt
- forwardStatus
- fromNumber
- messageFingerprint
Use the fingerprint to support duplicate prevention.
## 5. app_settings table
Create a single-row settings entity.
Fields:
- `id: Int`
- `deviceId: String`
- `serverPort: Int`
- `apiKeyHash: String`
- `apiKeyIdentifier: String?`
- `webhookUrl: String?`
- `webhookEnabled: Boolean`
- `webhookSecretEncrypted: String?`
- `maxRetryCount: Int`
- `retryBaseDelaySeconds: Int`
- `allowedPrefixes: String?`
- `dailySmsLimitEnabled: Boolean`
- `dailySmsLimit: Int`
- `rateLimitPerMinute: Int`
- `autoStartEnabled: Boolean`
- `requireHttpsWebhook: Boolean`
- `serverEnabled: Boolean`
- `createdAt: Long`
- `updatedAt: Long`
Use one stable settings row ID.
## 6. system_logs table
Create `SystemLogEntity`.
Fields:
- `id: Long`
- `level: String`
- `category: String`
- `eventCode: String?`
- `message: String`
- `details: String?`
- `createdAt: Long`
Levels:
- DEBUG
- INFO
- WARNING
- ERROR
Sanitize and truncate values before storage.
Never store secrets.
## 7. webhook_attempts table
Create `WebhookAttemptEntity`.
Fields:
- `id: Long`
- `receivedSmsId: Long`
- `attemptNumber: Int`
- `requestUrlSummary: String`
- `responseCode: Int?`
- `responseBodySummary: String?`
- `durationMs: Long?`
- `success: Boolean`
- `errorCode: String?`
- `errorMessage: String?`
- `attemptedAt: Long`
Create a foreign key to received SMS where safe.
Add indexes for receivedSmsId and attemptedAt.
## 8. request_audit_logs table
Create `RequestAuditLogEntity`.
Fields:
- `id: Long`
- `method: String`
- `path: String`
- `remoteAddress: String?`
- `responseCode: Int`
- `durationMs: Long`
- `authenticated: Boolean`
- `createdAt: Long`
Never store:
- Authorization header
- API key
- Webhook secret
- Complete SMS body
## 9. DAOs
Create DAOs for all entities.
Support:
- Insert
- Update
- Get by ID
- Delete where appropriate
- Count by status
- Count by day
- Paged queries
- Database-backed sorting
- Database-backed filtering
For sent SMS filters support:
- status
- destination
- client reference
- date range
- limit
- offset
- sort direction
For received SMS filters support:
- sender
- forward status
- date range
- limit
- offset
- sort direction
Do not load entire tables into memory to filter.
Enforce a maximum page size in repository logic.
## 10. Repositories
Create domain repository interfaces and data implementations for:
- Settings
- Sent SMS
- Received SMS
- Webhook attempts
- System logs
- API audit logs
Expose Flow where live observation is useful.
Use DTO or domain mappings rather than leaking Room entities everywhere.
## 11. Default settings
Initialize settings on first launch:
- Device ID: `android-gateway-01`
- Server port: 8080
- Rate limit: 60 requests per minute
- Daily SMS limit enabled: true
- Daily SMS limit: 500
- Webhook enabled: false
- Webhook URL: null
- Webhook secret: null
- Maximum webhook retries: 5
- Retry base delay: 30 seconds or safe WorkManager-compatible value
- Allowed prefixes: empty
- Auto Start: false
- Require HTTPS webhook: true
- Server enabled: false
Initialization must be idempotent.
## 12. API key manager
Create a secure API key manager.
Requirements:
- Generate at least 32 cryptographically random bytes.
- Encode using URL-safe Base64 without unnecessary padding.
- Display the full key only at initial generation or regeneration.
- Store only a strong hash.
- Store a short non-secret identifier if useful.
- Verify keys using constant-time comparison.
- Do not log raw keys.
- Old keys must stop working immediately after regeneration.
Use a modern password/key derivation or keyed hashing approach suitable for API token verification.
If using SHA-256, include a secure application-specific design and explain it.
Prefer storing a salted hash if practical.
## 13. Webhook secret protection
Protect the webhook secret.
Use AndroidX Security Crypto or Android Keystore-based encryption where compatible.
Requirements:
- Never return the existing secret from repository APIs.
- Provide only `secretConfigured: Boolean`.
- Blank UI input later must mean keep existing secret.
- Clearing the secret must be explicit.
- Handle key invalidation gracefully.
- Do not crash if decryption fails.
