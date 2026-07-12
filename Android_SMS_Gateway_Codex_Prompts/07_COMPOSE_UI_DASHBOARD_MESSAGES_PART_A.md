Continue the existing Android SMS Gateway project.
Implement Phase 7: complete professional Compose dashboard, message lists, message details, operational states, permission actions, and responsive navigation.
Preserve all previous phases.
Do not rewrite working business logic.
Create actual files, run the build, and fix errors.
## 1. UI objectives
Create a professional operational interface.
The application should look like a private infrastructure tool, not a tutorial demo.
Use:
- Jetpack Compose
- Material 3
- Responsive layout
- Light theme
- Dark theme
- Accessible contrast
- Clear status colors
- Meaningful icons
- Empty states
- Loading states
- Error states
- Confirmation dialogs
- Snackbars
- Pull-to-refresh where useful
Avoid:
- Tiny text
- Excessive decoration
- Generic placeholder cards
- Secrets displayed in normal screens
- Business logic inside Composables
## 2. Navigation
Main destinations:
- Dashboard
- Messages
- Logs
- Settings
Use bottom navigation on phones.
For larger widths, an adaptive navigation rail is acceptable.
Support:
- Sent message detail
- Received message detail
- Test SMS
- Security/API key display flow
- Webhook test result
Preserve navigation state across rotation.
## 3. Dashboard ViewModel
Create immutable dashboard state.
Include:
- Loading
- Gateway service state
- Service uptime
- Device ID
- Device IP
- API base URL
- Server port
- SEND_SMS permission
- RECEIVE_SMS permission
- READ_SMS permission if used
- Notification permission
- Battery optimization state
- SIM availability
- Active subscription summary
- Webhook enabled
- Webhook URL preview
- Sent today
- Received today
- Pending outgoing
- Failed outgoing
- Pending webhook
- Failed webhook
- Last incoming SMS
- Last sent SMS
- Last successful webhook
- Last failed webhook
- Error message
- Refresh state
Use Flow combinations carefully.
Avoid excessive database queries.
## 4. Dashboard layout
Create sections.
Gateway Status card:
- Running, Starting, Stopped, Stopping, Error
- Uptime
- Device ID
- API base URL
- Copy API URL action
Device Readiness card:
- Send permission
- Receive permission
- Notification permission
- Battery optimization
- SIM availability
Webhook card:
- Enabled or disabled
- Masked URL preview
- Pending count
- Failed count
- Test Webhook action
Today card:
- Sent
- Received
- Failed outgoing
- Failed webhook
Recent Activity card:
- Last sent
- Last received
- Last successful webhook
## 5. Dashboard actions
Provide:
- Start Gateway
- Stop Gateway
- Copy API URL
- Open Settings
- View Logs
- Send Test SMS
- Test Webhook
- Request Missing Permissions
- Open Battery Optimization Settings
- Regenerate API Key
Require confirmation before:
- Stop Gateway
- Regenerate API Key
Disable invalid actions during STARTING or STOPPING.
Show operation progress.
## 6. API key display flow
The API key must not be retrievable from stored hash.
Support:
- One-time display after first generation
- One-time display after regeneration
- Copy button
- Warning to save it securely
- Close confirmation if desired
- No later reveal action
- Only regeneration when lost
Do not take screenshots prevention unless implemented carefully.
Do not log clipboard content.
## 7. Permission request flow
Request permissions from the UI.
Explain why each is needed.
Handle:
- Granted
- Denied
- Denied permanently
- Notification permission not applicable on old Android
- Manufacturer-specific settings unavailable
Provide Open App Settings when permanently denied.
Do not block access to the app when permissions are missing.
Show degraded mode.
## 8. Battery optimization flow
Show:
- Optimized or unrestricted
- Why unrestricted mode improves reliability
- Open system settings action
- Refresh after returning
Do not claim the app can disable optimization itself.
## 9. Messages screen
Create tabs:
- Sent
- Received
Add:
- Search
- Status filter
- Date filter
- Sort order
- Refresh
- Empty state
- Loading state
- Paging
Use database-backed paging.
Do not load all messages.
## 10. Sent list
Each item shows:
- Masked or formatted destination
- Message preview
- Status chip
- Created time
- Client reference
- Segment count
- Error indicator
Status chips:
- PENDING
- SENDING
- SENT
- DELIVERED
- FAILED
- UNKNOWN
Open detail on tap.
## 11. Sent detail
Show:
- Full destination
- Full message
- Client reference
- Status
- Timeline
- Created time
- Sending time
- Sent time
- Delivered time
- Failed time
- Segment count
- Subscription ID
- SIM slot
- Error code
- Error message
Provide copy actions only where safe.
Do not expose internal stack traces.
## 12. Received list
Each item shows:
- Sender
- Message preview
- Received time
- Webhook status
- Retry count
- Error indicator
Statuses:
- PENDING
- RETRYING
- FORWARDED
- FAILED
- DISABLED if implemented
Open detail on tap.
## 13. Received detail
Show:
- Full sender
- Full message
- Received time
- Subscription ID
- SIM slot
- Forward status
- Retry count
- Last attempt
- Next retry
- Response code
- Error code
- Error message
- Attempt history
Provide Manual Retry when allowed.
Disable retry while work is active.
## 14. Attempt history UI
Each attempt shows:
- Attempt number
- Time
- Success or failure
- Response code
- Duration
- Error code
- Safe response summary
Do not show:
- Authorization header
- Secret
- Full raw HTTP exchange
## 15. Test SMS screen
Fields:
- Destination
- Message
- Client reference
- SIM selection when available
Show:
- Character count
- Estimated segment count
- Validation
- Send button
- Progress
- Local message ID
- Current status
Use the shared send use case.
Do not bypass rate and daily limits.
