Continue the existing Android SMS Gateway project.
Implement Phase 4: real outgoing SMS sending, multipart handling, sent and delivery callbacks, send API, status API, sent logs API, validation, limits, and test SMS integration.
Preserve all previous phases.
Create actual files, run the build, and fix errors.
## 1. Android SMS sender
Implement `SmsSender`.
Use Android `SmsManager`.
Support:
- Default active SIM
- Optional subscription ID
- Single-part SMS
- Multipart SMS
- Asynchronous status callbacks
- Unique local message ID
- Sent and delivered PendingIntents
- Database status updates
Do not claim SENT before Android confirms the sent callback.
## 2. Permission handling
Before accepting a send:
- Check `SEND_SMS`.
- Return `SMS_PERMISSION_DENIED` if absent.
- Do not crash.
- Update dashboard warning.
- Record a safe system log.
Do not request permission from inside the HTTP route.
The Android UI handles permission requests.
## 3. SIM handling
Before sending:
- Determine whether SMS-capable subscription exists.
- Validate optional subscription ID.
- Use `SubscriptionManager` only when permissions allow.
- Handle permission denial gracefully.
- Return:
  - `NO_SIM_AVAILABLE`
  - `INVALID_SUBSCRIPTION`
  - `SMS_NO_SERVICE`
  where appropriate.
Do not require READ_PHONE_STATE unless truly necessary.
If it is needed, add it to the manifest and runtime permission flow with clear justification.
## 4. Input validation
Validate:
Destination:
- Required
- Trimmed
- Reasonable maximum length
- Leading plus allowed
- Digits and allowed formatting only
- Normalize cautiously
- Do not incorrectly rewrite international numbers
Message:
- Required
- Not blank
- Maximum 5,000 characters
Client reference:
- Optional
- Maximum 200 characters
Allowed prefixes:
- If list is empty, allow all.
- Otherwise destination must match one configured prefix.
- Reject using `PREFIX_NOT_ALLOWED`.
## 5. Rate limit
Apply to `POST /api/sms/send`.
Default:
60 requests per minute
Use:
- Global rate limit
- Per-remote-address limit when address is available
Return HTTP 429.
Error:
`RATE_LIMIT_EXCEEDED`
Return retry-after information.
Add `Retry-After` header where practical.
## 6. Daily limit
Before accepting:
- Count accepted logical SMS for current UTC day.
- Apply settings.
- Default enabled at 500.
- One multipart request counts as one logical SMS.
- Store actual segment count separately.
Return:
`DAILY_SMS_LIMIT_EXCEEDED`
Use appropriate HTTP status.
## 7. Database lifecycle
When request is accepted:
1. Validate.
2. Insert sent SMS record with PENDING.
3. Return stable local message ID.
4. Start async send.
5. Update to SENDING.
6. Split message.
7. Send.
8. Process sent callbacks.
9. Process delivery callbacks.
10. Update final status.
Track timestamps:
- createdAt
- sendingAt
- sentAt
- deliveredAt
- failedAt
## 8. Multipart logic
Use:
`SmsManager.divideMessage`
For every segment:
- Unique PendingIntent request code
- Message ID in extras
- Segment index in extras
- Total segment count in extras
- Sent action
- Delivered action
Prevent PendingIntent collisions.
Use correct mutability flags.
Logical message state:
- SENT only after all segments report sent successfully.
- FAILED if any required segment fails.
- DELIVERED only after all segments confirm delivery.
- If delivery reporting is unavailable, keep SENT.
- UNKNOWN only for unresolved platform state.
Persist per-segment callback state.
If needed, create a `sms_segments` table with:
- id
- sentSmsId
- segmentIndex
- totalSegments
- sentStatus
- deliveryStatus
- sentResultCode
- deliveryResultCode
- updatedAt
If adding a table, increment Room version and create a real migration.
## 9. Sent result receiver
Create `SmsSentReceiver`.
Map Android result codes to stable errors.
At minimum:
- `SMS_GENERIC_FAILURE`
- `SMS_RADIO_OFF`
- `SMS_NULL_PDU`
- `SMS_NO_SERVICE`
- `SMS_LIMIT_EXCEEDED`
- `SMS_FDN_CHECK_FAILURE`
- `SMS_SHORT_CODE_NOT_ALLOWED`
- `SMS_SHORT_CODE_NEVER_ALLOWED`
- `UNKNOWN_SMS_ERROR`
Update segment and logical message status transactionally.
Keep receiver work short.
Use `goAsync` and coroutine support safely if database work is required.
## 10. Delivery receiver
Create `SmsDeliveredReceiver`.
Track per-segment delivery.
Update logical message to DELIVERED only after all expected delivery callbacks succeed.
Carrier delivery reports are not guaranteed.
Document that limitation.
## 11. POST /api/sms/send
Authentication required.
Request:
```json
{
  "to": "+639171234567",
  "message": "Your OTP code is 123456",
  "clientReference": "OTP-000001",
  "subscriptionId": null
}
```
Successful response:
HTTP 202 Accepted
```json
{
  "success": true,
  "data": {
    "messageId": 123,
    "status": "PENDING",
    "to": "+639171234567",
    "clientReference": "OTP-000001",
    "createdAt": "2026-07-11T21:30:00Z"
  }
}
```
Do not return SENT immediately.
## 12. GET /api/sms/status/{messageId}
Authentication required.
Return:
- messageId
- to
- message
- status
- createdAt
- sendingAt
- sentAt
- deliveredAt
- failedAt
- clientReference
- segmentCount
- subscriptionId
- simSlot
- errorCode
- errorMessage
Return 404 with `RECORD_NOT_FOUND` when absent.
Consider message-body privacy.
For now return the message only on this authenticated endpoint.
## 13. GET /api/sms/sent
Authentication required.
Filters:
- limit
- offset
- status
- to
- clientReference
- dateFrom
- dateTo
- sortDirection
Defaults:
- limit 50
- offset 0
- descending
Maximum:
100
Use database queries.
Return paging metadata.
## 14. Idempotency
Support optional client reference lookup.
Do not silently deduplicate all matching client references unless explicitly documented.
Prefer adding optional `Idempotency-Key` support:
- Validate safe length.
- Store key in sent SMS record or dedicated field.
- Same key with same payload returns existing accepted message.
- Same key with different payload returns conflict.
Only implement if it can be done correctly.
Otherwise document that callers must avoid duplicate retries.
## 15. Dashboard integration
Update metrics:
- Sent today
- Pending
- Sending
- Sent
- Delivered
- Failed
- Last sent time
Show SIM availability when possible.
Show degraded state when sending cannot work.
## 16. Test SMS UI
Implement a usable test SMS screen.
Fields:
- Destination
- Message
- Client reference
- Optional SIM selection
Show:
- Character count
- Estimated segment count
- Validation errors
- Sending progress
- Local message ID
- Current status
Use the same domain use case as the HTTP API.
Do not duplicate business rules.
## 17. Manifest and receivers
Declare sent and delivery receivers correctly.
Use explicit app-internal broadcasts where possible.
Protect receivers from spoofing.
Set exported flags correctly.
Do not make app-internal callback receivers publicly exported.
