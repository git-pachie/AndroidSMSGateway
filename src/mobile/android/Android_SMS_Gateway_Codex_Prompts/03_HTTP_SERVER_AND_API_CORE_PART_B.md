# Continuation of 03 Http Server And Api Core
Continue from the immediately preceding prompt part.
Preserve all work already completed in this phase.
Do not repeat completed tasks unless required to fix a defect.
## 15. Placeholder SMS routes
Register route structure for:
- `POST /api/sms/send`
- `GET /api/sms/status/{messageId}`
- `GET /api/sms/sent`
- `GET /api/sms/inbox`
Until later phases:
- Return HTTP 501 or a stable feature-not-ready response.
- Do not create fake successful responses.
- Keep route contracts ready for implementation.
## 16. API models
Create serialized request and response DTOs.
Keep API DTOs separate from Room entities.
Create mappers.
Avoid exposing database implementation fields directly.
## 17. Network address utility
Implement device IP display.
Requirements:
- Prefer active Wi-Fi or LAN address.
- Avoid deprecated APIs where possible.
- Handle multiple interfaces.
- Exclude loopback.
- Present a safe best candidate.
- Return null when unavailable.
Build base URL:
`http://DEVICE_IP:PORT`
Do not claim encryption.
## 18. Foreground notification
Update notification body:
`Server: http://DEVICE_IP:PORT`
When unavailable:
`Server running on port PORT`
Show ERROR state safely if startup fails.
## 19. Tests
Add tests for:
- Health route without authentication
- Private route missing token
- Private route invalid token
- Valid token
- Request ID generation
- Request ID sanitization
- Settings does not expose secrets
- Invalid settings payload
- Paging maximum
- Negative offset
- Error mapping
- Audit log excludes authorization header
- Server duplicate-start prevention
Use Ktor test facilities where practical.
## 20. Documentation
Create an initial `API.md`.
Document:
- Base URL
- Private network assumption
- Authentication
- Request IDs
- Response envelope
- Error envelope
- Health endpoint
- Settings endpoints
- Logs endpoints
- Placeholder SMS endpoints
- HTTP cleartext warning
## 21. Build verification
Run:
`./gradlew clean assembleDebug`
Run:
`./gradlew test`
Run lint if practical.
Fix all build-breaking problems.
## 22. Final response
Report:
- Server implementation
- Bound address and default port
- Authentication design
- Middleware
- Endpoints implemented
- Tests
- Build result
- Remaining work for Phase 4
Do not implement real SMS sending in this phase.
