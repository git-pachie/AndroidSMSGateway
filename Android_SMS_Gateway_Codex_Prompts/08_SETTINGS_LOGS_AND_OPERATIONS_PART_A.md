Continue the existing Android SMS Gateway project.
Implement Phase 8: complete settings, logs, audit UI, data management, boot reliability, port restart workflow, diagnostics export, and operational safeguards.
Preserve all previous phases.
Create actual files, run the build, and fix errors.
## 1. Settings screen
Create grouped sections:
- Gateway Identity
- HTTP Server
- API Security
- SMS Limits
- Webhook
- Device Reliability
- Data Management
- About
Use a ViewModel with immutable state.
Validate before saving.
Show save progress and result.
## 2. Gateway Identity
Fields:
- Device ID
Rules:
- Required
- Trimmed
- Reasonable maximum length
- Safe characters
- No secrets
- Update health and webhook payload after save
## 3. HTTP Server settings
Fields:
- Server port
- Auto Start after reboot
- Trusted-network warning
Port validation:
- 1024 to 65535
When port changes:
1. Validate.
2. Ask whether to restart now.
3. Save the new port.
4. If restart selected, stop server safely.
5. Start on new port.
6. Show success.
7. If bind fails, show clear error.
8. Avoid leaving duplicate servers.
9. Preserve old port or provide recovery if practical.
Do not restart before the UI receives confirmation.
## 4. Auto Start
Toggle:
- Persist setting.
- BootReceiver reads it.
- Show warning about manufacturer battery restrictions.
- Do not start gateway immediately just because toggle changed unless user chooses.
Add manual verification guidance.
## 5. API Security settings
Show:
- API key configured
- Key identifier
- Regenerate API key button
- Rate limit per minute
- Trusted-network warning
Regeneration flow:
- Confirmation
- Generate new key
- Persist hash
- Old key invalid immediately
- Show new key once
- Copy action
- Never reveal later
Rate limit validation:
- Minimum sensible value
- Maximum safe value
- Reject zero or negative
## 6. SMS Limits settings
Fields:
- Daily limit enabled
- Daily limit value
- Allowed number prefixes
Rules:
- Daily limit positive
- Prefix list trimmed
- Deduplicated
- Empty means allow all
- Show examples
- Warn that carrier charges still apply
- Explain multipart counting as one logical app message
## 7. Webhook settings
Fields:
- Enabled
- URL
- Secret
- Secret configured indicator
- Require HTTPS
- Maximum retries
- Base retry delay
- Test Webhook
Rules:
- Existing secret never displayed.
- Blank secret input keeps current secret.
- Separate Clear Secret action.
- Confirmation before clearing.
- Enabling requires valid URL.
- HTTPS required when toggle enabled.
- Maximum retries bounded.
- Retry delay respects WorkManager minimum.
- Saving other fields must not erase secret.
Test action:
- Show progress.
- Show response code.
- Show duration.
- Show safe summary.
- Do not show headers or secret.
## 8. Device Reliability
Show:
- SEND_SMS permission
- RECEIVE_SMS permission
- Notification permission
- READ_SMS if used
- Battery optimization
- Auto Start state
- SIM availability
- Current network state
Actions:
- Request permissions
- Open app settings
- Open battery settings
- Refresh state
Explain that some manufacturers require separate auto-start permission.
Do not provide manufacturer-specific instructions that are unverified.
## 9. Logs screen
Create tabs:
- System Logs
- Webhook Attempts
- API Audit
Each tab supports:
- Search where meaningful
- Level/status filter
- Date filter
- Sort order
- Refresh
- Paging
- Empty state
- Error state
Use backend/database paging.
## 10. System logs UI
Show:
- Time
- Level
- Category
- Event code
- Message
- Safe details
Do not show secrets.
Add level chips.
Support clear logs with confirmation.
## 11. Webhook attempts UI
Show:
- SMS ID
- Attempt number
- Time
- Success
- Response code
- Duration
- Error code
- Safe response summary
Open related received message.
Do not show raw Authorization data.
## 12. API audit UI
Show:
- Method
- Path
- Response code
- Duration
- Authenticated
- Remote address if stored
- Time
Do not show request body.
Do not show API token.
## 13. Data management
Actions:
- Export sanitized diagnostics
- Clear system logs
- Clear API audit logs
- Clear webhook attempt logs where safe
- Clear sent history
- Clear received history
Every destructive action requires confirmation.
For related records:
- Respect foreign keys.
- Delete in safe order.
- Use transactions.
- Explain consequences.
Do not offer “Clear All” without strong confirmation.
## 14. Sanitized diagnostics export
Create a local export file.
Include:
- App version
- Android version
- Device model
- Gateway state
- Permission states
- Server port
- Device ID
- Safe settings summary
- Recent system logs
- Recent error codes
- Counts
Exclude:
- API key
- API hash
- Webhook secret
- Encrypted secret
- Full SMS bodies
- Full phone numbers
- Authorization headers
- Raw database file
Use Android share sheet.
Use FileProvider correctly.
Delete old temporary exports when appropriate.
## 15. Privacy-safe previews
Create masking utilities.
Phone numbers:
- Show limited ending digits in logs where possible.
Webhook URL:
- Show scheme, host, and shortened path.
Message body:
- Use short previews in lists.
- Full body only in authenticated app detail screens.
- Never include in diagnostics export.
## 16. Boot reliability
Verify BootReceiver.
Requirements:
- Auto-start only when enabled.
- Correct foreground service start call.
- No crash after reboot.
- No duplicate service start.
- Log safe startup outcome.
- Handle locked device limitations.
- Do not enter repeated restart loop.
## 17. Service recovery
Handle:
- Ktor server unexpected stop
- Port already in use
- Settings load failure
- Notification failure
- Process recreation
- Network interface change
The service should report ERROR or degraded state.
Do not silently restart indefinitely.
## 18. API enhancements
Ensure all settings endpoints match UI behavior.
Add or verify:
- Safe server settings update
- Webhook clear-secret operation
- Sanitized settings response
- Logs paging endpoints
- Audit paging endpoint
- Webhook attempts paging endpoint
All private endpoints require authentication.
## 19. Confirmation and feedback
Before delete:
- Show confirmation dialog.
After successful add, update, delete, start, stop, test, or retry:
- Show dismissible snackbar.
For failures:
- Show clear user-facing message.
- Keep technical details in safe logs.
