Perform the final audit and release preparation for the existing Android SMS Gateway project.
Do not add unrelated features.
Do not replace working architecture.
Inspect everything, run all checks, fix defects, and produce a final release-readiness report.
## 1. Functional checklist
Verify the application can:
- Start foreground service
- Start Ktor server
- Bind configured port
- Stop server safely
- Prevent duplicate server instances
- Expose public health endpoint
- Protect all private endpoints
- Generate API key
- Regenerate API key
- Invalidate old key
- Send single-part SMS
- Send multipart SMS
- Track sent callbacks
- Track delivered callbacks
- Receive incoming SMS
- Reconstruct multipart incoming SMS
- Prevent duplicate records
- Store sent messages
- Store received messages
- Forward incoming SMS
- Store webhook attempts
- Retry failed webhook delivery
- Retry after network returns
- Manually retry failed delivery
- Auto-start after reboot when enabled
- Show dashboard
- Show messages
- Show logs
- Save settings
- Export sanitized diagnostics
Fix missing mandatory behavior.
## 2. API checklist
Verify endpoints:
- `GET /api/health`
- `POST /api/sms/send`
- `GET /api/sms/status/{messageId}`
- `GET /api/sms/sent`
- `GET /api/sms/inbox`
- `GET /api/sms/inbox/{smsId}`
- `GET /api/settings`
- `PUT /api/settings`
- `POST /api/settings/webhook`
- Server settings endpoint if implemented
- `POST /api/webhook/test`
- `POST /api/webhook/retry/{smsId}`
- `GET /api/webhook/attempts`
- `GET /api/logs/system`
- `GET /api/logs/audit`
Verify:
- JSON only
- Correct HTTP codes
- Stable envelopes
- ISO 8601 UTC timestamps
- Paging limit maximum 100
- Negative offsets rejected
- Request IDs
- Authentication
- No secrets returned
## 3. Security checklist
Confirm:
- HTTP server warning is visible.
- README recommends VPN.
- No public internet recommendation.
- API key stored only as hash.
- Webhook secret encrypted.
- Secret never returned.
- Secret never logged.
- Authorization never logged.
- SMS cannot execute URL.
- SMS cannot execute command.
- No proxy endpoint.
- Rate limit active.
- Daily limit active.
- Prefix restriction active.
- Webhook HTTPS enforcement active.
- Error responses contain no stack trace.
- Diagnostics contain no SMS bodies.
- Release Logcat contains no full SMS body.
Search the repository for:
- Hardcoded API keys
- Hardcoded secrets
- Sample real phone numbers
- TODO
- FIXME
- `printStackTrace`
- Authorization logging
- Destructive migration
- Global lint suppression
- Insecure trust manager
- Hostname verifier bypass
- Cleartext webhook bypass
Fix unsafe findings.
## 4. Android checklist
Verify:
- Compile SDK
- Target SDK
- Minimum SDK
- Java 17
- Correct manifest permissions
- Correct exported flags
- Foreground service type
- Notification channel
- PendingIntent flags
- BootReceiver
- SmsReceiver
- Sent receiver
- Delivered receiver
- FileProvider
- Runtime permission flow
- Battery optimization flow
Ensure compatibility with the selected target SDK.
## 5. Database checklist
Verify:
- Current Room version
- Every migration path
- Exported schema
- Indices
- Foreign keys
- Transactions
- Paging queries
- Duplicate fingerprint logic
- Segment state logic
- Retry attempt history
- Safe deletion
Run migration tests.
## 6. Concurrency checklist
Review for races:
- Service start versus stop
- Duplicate Ktor start
- Port restart
- Multipart callbacks
- Multiple delivery callbacks
- Duplicate SMS broadcasts
- Duplicate WorkManager runs
- API key regeneration during request
- Settings update during webhook attempt
- App shutdown during database write
Use mutexes, atomic state, transactions, or idempotent operations where needed.
## 7. UX checklist
Verify:
- Start button state
- Stop confirmation
- Regenerate confirmation
- API key one-time display
- Permission warnings
- Battery warning
- SIM warning
- Webhook disabled state
- Empty lists
- Loading states
- Error states
- Dark mode
- Accessible touch targets
- Status not shown only by color
- No tiny text
- No secret displayed
## 8. Performance checklist
Verify:
- No database on main thread
- No HTTP on main thread
- BroadcastReceiver finishes quickly
- Lists use paging
- Logs are bounded
- Response bodies truncated
- Coroutines are scoped
- Flows are not duplicated unnecessarily
- Server shutdown is graceful
## 9. Test execution
Run:
`./gradlew clean assembleDebug`
Run:
`./gradlew test`
Run:
`./gradlew lint`
Run:
`./gradlew connectedAndroidTest`
only if a device or emulator is available.
Run:
`./gradlew assembleRelease`
if release signing configuration permits unsigned or local release assembly.
Fix failures.
Do not report a command as successful unless it actually ran successfully.
## 10. APK output
Identify actual generated paths.
Typical examples:
- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release-unsigned.apk`
Report only paths that exist.
Do not claim that real carrier SMS was tested unless a physical device test occurred.
## 11. Documentation audit
Verify files exist and match implementation:
- `README.md`
- `API.md`
- `SECURITY.md`
- `CHANGELOG.md`
- `docs/MANUAL_ACCEPTANCE_TESTS.md`
- `docs/TROUBLESHOOTING.md`
Update incorrect endpoint examples.
Update version numbers.
Update Gradle commands.
Update permissions.
Update known limitations.
## 12. Final limitations
Document honestly:
- Physical SIM required
- Carrier behavior varies
- Delivery receipts not guaranteed
- Manufacturer background restrictions vary
- HTTP API is unencrypted without VPN
- SMS permissions must be manually granted
- Google Play distribution is not intended
- Emulator cannot validate real carrier delivery
- Dual-SIM metadata may vary by manufacturer
## 13. Final release-readiness report
Create:
`docs/RELEASE_READINESS_REPORT.md`
Include:
- Project version
- Build date
- Git commit if available
- Build environment
- SDK versions
- Gradle version
- Kotlin version
- Database version
- Implemented features
- Implemented endpoints
- Security controls
- Automated tests
- Manual tests
- Build commands and results
- APK paths
- Known limitations
- Physical-device checks pending
- Release recommendation
## 14. Final response
Provide a concise summary:
- Build status
- Test status
- Lint status
- APK path
- Release readiness
- Known blockers
- Physical-device work still required
Do not begin a new phase.
Do not leave mandatory TODO items.
Do not claim success for checks that could not run.
