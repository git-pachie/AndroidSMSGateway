Continue the existing Android SMS Gateway project.
Implement Phase 5: incoming SMS BroadcastReceiver, multipart reconstruction, duplicate prevention, local storage, inbox API, received-message UI, and boot receiver foundation.
Preserve all previous phases.
Create actual files, run the build, and fix errors.
## 1. Incoming SMS receiver
Create `SmsReceiver`.
Listen for the Android SMS received broadcast.
Requirements:
- Check RECEIVE_SMS permission state.
- Parse PDUs safely.
- Support format parameter.
- Support multipart messages.
- Extract sender.
- Extract message body.
- Extract timestamp.
- Extract subscription ID when available.
- Derive SIM slot where possible.
- Save locally before any webhook work.
- Keep receiver execution short.
- Use `goAsync` when asynchronous storage is required.
- Always finish PendingResult.
Do not execute URLs or commands from SMS content.
## 2. Multipart reconstruction
Incoming multipart SMS may arrive as multiple PDUs.
Implement robust reconstruction.
At minimum:
- Group segments from the same broadcast.
- Sort segments in platform order.
- Join body text.
- Use a consistent sender.
- Use earliest sensible timestamp.
- Store one logical received message.
If platform metadata exposes concat reference and sequence, use it safely.
Do not store each segment as a separate inbox message unless reconstruction is impossible.
## 3. Duplicate prevention
Create a message fingerprint.
Fingerprint may include:
- Normalized sender
- Received timestamp bucket
- Message body
- Subscription ID
Use a cryptographic hash.
Before insert:
- Query recent matching fingerprint.
- Avoid duplicate inserts caused by repeated broadcasts.
- Do not discard legitimately repeated SMS messages sent at different times.
Document the chosen time window.
Add a unique index only if it will not create false positives.
If changing schema, create a proper Room migration.
## 4. received_sms lifecycle
On receipt:
1. Parse incoming SMS.
2. Validate minimum required data.
3. Create fingerprint.
4. Insert received message.
5. Set `forwardStatus = PENDING`.
6. Update daily received metrics.
7. Write safe system log.
8. Schedule later webhook work only through an interface.
9. Return quickly.
Webhook forwarding is implemented in Phase 6.
For now, create a `WebhookScheduler` interface and a no-op or pending implementation that does not fake success.
## 5. Permissions
Dashboard must show RECEIVE_SMS state.
If permission is missing:
- Show clear warning.
- Provide a Request Permission action.
- Do not crash.
- Health endpoint reports degraded.
- Incoming receiver simply cannot function.
Handle READ_SMS separately.
The receiver may not need READ_SMS for broadcast processing.
Do not require unnecessary permissions.
## 6. Sender normalization
Create safe sender handling.
Requirements:
- Trim whitespace.
- Preserve international leading plus.
- Support alphanumeric sender IDs.
- Do not force all senders into numeric format.
- Store original display sender.
- Create a normalized form only if needed for matching.
Do not reject valid alphanumeric business senders.
## 7. SIM information
Attempt to capture:
- subscriptionId
- simSlot
Handle:
- Single SIM
- Dual SIM
- Missing metadata
- Permission restrictions
- Manufacturer differences
These values may be null.
Do not fail message storage when unavailable.
## 8. GET /api/sms/inbox
Authentication required.
Filters:
- limit
- offset
- from
- forwardStatus
- dateFrom
- dateTo
- sortDirection
Defaults:
- limit 50
- offset 0
- descending
Maximum limit:
100
Use database-backed queries.
Response item fields:
- smsId
- from
- message
- receivedAt
- subscriptionId
- simSlot
- forwardStatus
- retryCount
- lastForwardAttemptAt
- nextRetryAt
- webhookResponseCode
- errorCode
- errorMessage
Return paging metadata.
## 9. GET /api/sms/inbox/{smsId}
Authentication required.
Return full received message detail.
Include safe webhook status fields.
Webhook attempt history may be empty until Phase 6.
Return 404 when absent.
## 10. Received messages UI
Complete the Received tab.
List item:
- Sender
- Message preview
- Received time
- Webhook status chip
- Retry count
Detail screen:
- Full sender
- Full message
- Received timestamp
- Subscription ID
- SIM slot
- Forward status
- Retry count
- Last forward attempt
- Next retry
- Response code
- Error information
- Placeholder attempt-history section
Use database paging where practical.
Do not load the entire inbox.
## 11. Search and filtering
Provide UI filters:
- Sender search
- Forward status
- Date range or date preset
- Sort order
Apply filters through repository/database queries.
Do not filter only in memory.
## 12. Dashboard metrics
Update dashboard:
- Received today
- Pending webhook count
- Failed webhook count
- Last incoming SMS time
Health endpoint:
- RECEIVE_SMS permission
- Pending webhook count
- Degraded state when receive permission is missing
## 13. System logs
Log:
- Incoming SMS stored
- Duplicate ignored
- Parse failure
- Permission unavailable
- SIM metadata unavailable only at debug level
Do not log full SMS body.
Mask sender in release logs.
Store a short event summary.
## 14. Boot receiver foundation
Create `BootReceiver`.
Listen for:
- `BOOT_COMPLETED`
Behavior:
- Read Auto Start setting safely.
- Start gateway foreground service only when enabled.
- Use appropriate Android API.
- Keep receiver work short.
- Handle direct boot only if properly implemented.
- Do not auto-start when setting is false.
Declare receiver with correct exported value and permission.
If encrypted credential storage is unavailable before unlock, wait until normal boot completion.
## 15. Auto-start reliability
When Auto Start is enabled:
- Start service after boot.
- Service loads the configured port.
- Server startup failures are logged.
- Do not create restart loops.
When disabled:
- Receiver exits without side effects.
## 16. Incoming test strategy
Create testable abstractions around PDU parsing.
Unit test what can be tested without a device.
Instrumentation or manual test required for real carrier SMS.
Document physical-device steps.
## 17. Tests
Add tests for:
- PDU grouping helper
- Multipart body reconstruction
- Sender preservation
- Alphanumeric sender
- Fingerprint generation
- Duplicate-window behavior
- Inbox paging
- Inbox filters
- Inbox detail 404
- Auto-start enabled
- Auto-start disabled
- Receiver always completes async work
- Health degraded when receive permission missing
Use fakes for Android-specific dependencies.
## 18. Documentation
Update `API.md`.
Add:
- Inbox endpoint
- Inbox detail endpoint
- Received message fields
- Multipart receive behavior
- Duplicate prevention
- SIM metadata limitations
Update README draft with manual test:
1. Grant RECEIVE_SMS.
2. Start gateway.
3. Send SMS to gateway phone.
4. Confirm inbox entry.
5. Confirm one logical record for long multipart SMS.
6. Restart device with Auto Start enabled.
7. Confirm gateway restarts.
## 19. Build verification
Run:
`./gradlew clean assembleDebug`
Run:
`./gradlew test`
Run lint if practical.
Fix all build-breaking problems.
## 20. Final response
Report:
- Receiver implementation
- Multipart behavior
- Duplicate prevention
- Database migration
- Inbox endpoints
- UI updates
- Boot behavior
- Tests
- Build result
- Physical-device tests required
- Remaining work for Phase 6
Do not implement actual webhook HTTP delivery yet.
