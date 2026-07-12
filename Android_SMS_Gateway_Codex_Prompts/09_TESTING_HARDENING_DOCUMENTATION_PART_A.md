Continue the existing Android SMS Gateway project.
Implement Phase 9: comprehensive testing, security hardening, privacy review, reliability review, documentation, release configuration, and manual acceptance guide.
Preserve all previous phases.
Do not remove working features.
Create actual files, run all available checks, and fix errors.
## 1. Full architecture review
Inspect:
- Dependency boundaries
- Hilt modules
- Repository interfaces
- Database access
- Coroutine dispatchers
- Service lifecycle
- HTTP server lifecycle
- BroadcastReceiver execution
- WorkManager uniqueness
- Compose state management
Fix:
- Main-thread database work
- Leaking Android Context
- Duplicate singleton instances
- Race conditions
- Unstructured coroutines
- Missing cancellation
- Blocking calls on main thread
- Unsafe mutable shared state
## 2. Security review
Verify:
- API key never stored in plain text
- API key hash never returned through API
- API key never logged
- Webhook secret encrypted
- Webhook secret never returned
- Authorization headers never logged
- SMS content cannot choose webhook URL
- No arbitrary command execution
- No public generic proxy endpoint
- Rate limit enforced
- Daily limit enforced
- Allowed prefixes enforced
- HTTPS webhook enforcement works
- Request IDs sanitized
- API errors do not expose stack traces
- Diagnostics export excludes secrets
Fix every violation.
## 3. HTTP security review
Verify:
- Only `/api/health` is public
- All other endpoints require Bearer token
- Missing key returns 401
- Invalid key returns 401
- Rate limit returns 429
- Invalid input returns 400
- Missing record returns 404
- Conflict returns 409
- Unavailable service returns 503 where appropriate
- All responses are JSON
- Request body size has a safe limit
- Query values are bounded
- CORS is disabled unless explicitly needed
- No directory listing
- No static file exposure
Add safe request-size limits.
## 4. Android component review
Verify manifest:
- Correct exported flags
- Main activity exported correctly
- Internal receivers not exported
- SMS receiver declared correctly
- BootReceiver declared correctly
- Service declared correctly
- Foreground service permissions correct
- No unnecessary dangerous permissions
- PendingIntent mutability correct
- FileProvider restricted correctly
Remove unnecessary permissions.
## 5. Database review
Verify:
- Foreign keys
- Indexes
- Migrations
- No destructive migration in release
- Paging queries
- Transactional updates
- Duplicate prevention
- Retry counts
- Timestamp consistency
- Enum serialization safety
- Schema export
Create migration tests for every version transition.
## 6. Logging review
Verify:
- No raw API token
- No raw webhook secret
- No Authorization header
- No full SMS body in release Logcat
- No full phone number in release diagnostics
- Response body truncated
- Error messages sanitized
- Audit logs contain no body
Add automated redaction tests.
## 7. Reliability review
Test scenarios:
- Start gateway
- Stop gateway
- Duplicate start
- Duplicate stop
- Port conflict
- Port change
- App process recreation
- Device reboot
- Wi-Fi loss
- Wi-Fi return
- Webhook down
- Webhook recovery
- No SIM
- SIM temporarily unavailable
- SEND_SMS revoked
- RECEIVE_SMS revoked
- Notification permission denied
- Battery optimization enabled
- Duplicate incoming broadcast
- Duplicate WorkManager execution
- Database exception
- HTTP client timeout
Ensure no crash.
## 8. Unit tests
Ensure meaningful coverage for:
- API key hashing
- API key verification
- API key regeneration
- Phone validation
- Allowed prefixes
- Message validation
- Port validation
- URL validation
- Rate limiting
- Daily limits
- Request ID validation
- Error mapping
- Timestamp formatting
- Message fingerprint
- Multipart send aggregation
- Multipart receive aggregation
- Android result mapping
- Webhook retry decision
- Response truncation
- Secret redaction
- Diagnostics sanitization
Avoid tests that only assert constants.
## 9. Ktor route tests
Test:
- Public health
- Missing auth
- Invalid auth
- Valid auth
- Send accepted
- Send validation error
- Rate limited
- Daily limit exceeded
- Status found
- Status missing
- Sent paging
- Inbox paging
- Settings safe response
- Settings invalid input
- Webhook test
- Manual retry
- Logs paging
- Audit paging
- Secret fields absent
- Error envelope format
- Request ID response header
## 10. Room tests
Test:
- Insert sent
- Update status
- Segment aggregation
- Insert received
- Duplicate detection
- Forward status
- Attempt history
- Filters
- Paging
- Sorting
- Counts
- Date ranges
- Transaction rollback
- Migrations
## 11. Compose tests
Test:
- Dashboard states
- Permission warning
- Service start action
- Stop confirmation
- Regenerate confirmation
- One-time API key display
- Sent list
- Received list
- Detail screens
- Settings validation
- Secret preservation
- Clear secret confirmation
- Logs filters
- Diagnostics action
- Dark mode
## 12. Static analysis
Run:
- Gradle build
- Unit tests
- Android lint
- KSP generation
- Room schema validation
Configure lint to fail on serious issues.
Do not suppress warnings globally.
Use targeted suppressions with explanations only when necessary.
## 13. Release configuration
Configure release build:
- R8/minification if safe
- Resource shrinking if safe
- No debug logging
- No embedded secrets
- No signing keys committed
- ProGuard rules for Room, Hilt, Ktor, serialization where required
- Clear version name and version code
- Reproducible build guidance
Do not create or commit a real production keystore.
Provide signing instructions.
## 14. README.md
Create a complete README.
Include:
- Purpose
- Features
- Architecture
- Technology stack
- Requirements
- Android versions
- Build instructions
- Debug APK
- Release APK
- Manual installation
- Permissions
- Starting and stopping
- Finding device IP
- API key handling
- VPN recommendation
- Cleartext HTTP warning
- Webhook setup
- Auto Start
- Battery optimization
- Troubleshooting
- Physical-device testing
- Known manufacturer limitations
- Security warning
