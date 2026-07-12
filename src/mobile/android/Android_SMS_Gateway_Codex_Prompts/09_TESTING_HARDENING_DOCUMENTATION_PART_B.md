# Continuation of 09 Testing Hardening Documentation
Continue from the immediately preceding prompt part.
Preserve all work already completed in this phase.
Do not repeat completed tasks unless required to fix a defect.
## 15. API.md
Complete:
- Base URL
- Authentication
- Request IDs
- Success envelope
- Error envelope
- HTTP codes
- All endpoints
- Request examples
- Response examples
- Paging
- Filters
- Rate limits
- Daily limits
- Webhook payload
- Retry behavior
- Error codes
- Curl examples
## 16. SECURITY.md
Create:
- Threat model
- Trusted network assumption
- Private APK assumption
- API key lifecycle
- Secret storage
- Cleartext HTTP limitation
- VPN recommendation
- Webhook HTTPS
- SMS abuse controls
- Prefix restrictions
- Daily limits
- Logging policy
- Data privacy
- Lost API key recovery
- Device replacement
- Incident response
- Security limitations
## 17. CHANGELOG.md
Create version `1.0.0`.
Summarize:
- Gateway service
- HTTP API
- Send SMS
- Receive SMS
- Webhook forwarding
- Retry queue
- UI
- Security
- Tests
- Documentation
## 18. Manual acceptance guide
Create `docs/MANUAL_ACCEPTANCE_TESTS.md`.
Include exact steps:
1. Build APK.
2. Install manually.
3. Grant permissions.
4. Start gateway.
5. Call health endpoint.
6. Send SMS through API.
7. Confirm destination receives.
8. Confirm status updates.
9. Send SMS to gateway phone.
10. Confirm inbox entry.
11. Configure webhook.
12. Confirm forwarding.
13. Disable internet.
14. Receive another SMS.
15. Confirm queued state.
16. Restore internet.
17. Confirm retry succeeds.
18. Regenerate API key.
19. Confirm old key fails.
20. Restart phone with Auto Start.
21. Confirm server restarts.
22. Confirm logs contain no secrets.
## 19. Troubleshooting guide
Create `docs/TROUBLESHOOTING.md`.
Cover:
- APK install blocked
- Permission denied
- No SIM
- Radio off
- No service
- SMS not delivered
- Port in use
- Cannot reach phone
- Wrong IP
- Wi-Fi isolation
- VPN routing
- Webhook timeout
- TLS error
- Battery optimization
- Auto Start failure
- Manufacturer process killing
- Lost API key
- Database migration failure
## 20. Curl examples
Include tested examples for:
- Health
- Send SMS
- Message status
- Sent list
- Inbox
- Settings
- Webhook test
- Manual retry
- Logs
Do not include real secrets.
## 21. Build verification
Run:
`./gradlew clean assembleDebug`
Run:
`./gradlew test`
Run:
`./gradlew lint`
If release build is configured:
`./gradlew assembleRelease`
Fix build-breaking issues.
Report tests that require a physical phone.
## 22. Final response
Report:
- Security issues found and fixed
- Test coverage areas
- Documentation created
- Build results
- APK paths
- Release limitations
- Physical-device tests remaining
- Remaining work for final audit
