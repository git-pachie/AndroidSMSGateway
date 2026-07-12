# Continuation of 02 Database And Security
Continue from the immediately preceding prompt part.
Preserve all work already completed in this phase.
Do not repeat completed tasks unless required to fix a defect.
## 14. Validation
Create reusable validators.
Server port:
- Required range: 1024 to 65535
Destination number:
- Required
- Trim spaces
- Reasonable maximum length
- Allow leading plus
- Reject obviously invalid characters
SMS message:
- Required
- Must not be blank
- Maximum 5,000 characters
Client reference:
- Optional
- Maximum 200 characters
Query paging:
- limit default 50
- maximum 100
- offset non-negative
Webhook URL:
- Valid HTTP or HTTPS
- Require HTTPS when configured
- Reject unsupported schemes
- Reject blank URL when enabling webhook
Allowed prefixes:
- Parse comma-separated or list input
- Trim
- Deduplicate
- Reject invalid values
- Empty list means allow all destinations
## 15. Rate limiter
Create a thread-safe in-memory rate limiter foundation.
Default:
60 requests per minute
Support:
- Global send limit
- Per-remote-address limit when address is available
Return a decision containing:
- allowed
- retryAfterSeconds
- remaining estimate
Use monotonic time where appropriate.
## 16. Daily SMS limit
Create repository/use-case logic to enforce the daily limit.
Count accepted logical SMS messages for the current local day or clearly documented UTC day.
Prefer UTC for API consistency.
One multipart message counts as one logical application SMS.
Store segment count separately.
## 17. Logging repository
Connect the existing logging abstraction to the database.
Requirements:
- Safe asynchronous writes
- Truncation
- Secret redaction
- No recursive logging failures
- Log database failure to Logcat safely without crashing
## 18. Dashboard integration
Update the dashboard ViewModel to read:
- Device ID
- Server port
- Daily sent count
- Daily received count
- Failed outgoing count
- Pending webhook count
- Failed webhook count
The UI may remain basic, but the data pipeline must work.
## 19. Tests
Add unit tests for:
- API key generation
- API key verification
- Old key invalid after regeneration
- Constant-time verification behavior where testable
- Phone validation
- Message validation
- Allowed-prefix validation
- Server port validation
- Webhook URL validation
- Rate limiter
- Daily SMS limit
- Default settings initialization
- Entity/domain mapping
Add Room instrumentation tests if the environment supports them.
## 20. Build verification
Run:
`./gradlew clean assembleDebug`
Run:
`./gradlew test`
Run available Room or instrumentation checks if possible.
Fix build-breaking issues.
## 21. Final response
Report:
- Database version
- Tables created
- Indexes created
- Repository interfaces
- Security design
- Secret storage design
- Tests added
- Build result
- Remaining work for Phase 3
Do not implement the HTTP server yet beyond interfaces needed by later phases.
