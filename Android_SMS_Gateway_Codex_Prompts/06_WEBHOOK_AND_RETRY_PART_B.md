# Continuation of 06 Webhook And Retry
Continue from the immediately preceding prompt part.
Preserve all work already completed in this phase.
Do not repeat completed tasks unless required to fix a defect.
## 18. Tests
Add tests for:
- Payload serialization
- Fixed configured URL only
- Webhook cannot use SMS-provided URL
- Bearer header added when configured
- Secret not returned
- 2xx success
- 4xx policy
- 5xx retry
- Timeout retry
- TLS failure mapping
- Retry exhaustion
- Unique work naming
- Duplicate scheduling prevention
- Manual retry 202
- Already forwarded conflict
- Test webhook does not create SMS record
- Response truncation
- Attempt paging
## 19. Documentation
Update `API.md`.
Document:
- Webhook payload
- Headers
- Bearer authentication
- HMAC if implemented
- Success condition
- Retry policy
- Manual retry endpoint
- Test endpoint
- Attempt endpoint
- Security warning
Update README with webhook troubleshooting.
## 20. Build verification
Run:
`./gradlew clean assembleDebug`
Run:
`./gradlew test`
Run lint if practical.
Fix all build-breaking problems.
## 21. Final response
Report:
- HTTP client
- Payload
- Authentication
- Retry policy
- WorkManager behavior
- New endpoints
- UI changes
- Tests
- Build result
- Remaining work for Phase 7
