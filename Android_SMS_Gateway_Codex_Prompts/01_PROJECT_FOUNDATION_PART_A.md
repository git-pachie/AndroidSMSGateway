You are a senior Android architect and Kotlin developer.
Work inside the current repository and implement Phase 1 of a private Android SMS Gateway application.
Do not only explain. Create and modify actual project files.
## 1. Inspect first
Before changing anything:
1. Inspect the repository structure.
2. Determine whether an Android project already exists.
3. Preserve useful existing work.
4. Do not overwrite unrelated files.
5. Identify Gradle, Android SDK, Kotlin, and Java compatibility.
6. Produce a brief implementation plan.
7. Then begin implementation immediately.
## 2. Project identity
Application name:
Android SMS Gateway
Package name:
`com.sanshare.smsgateway`
Purpose:
Turn a dedicated Android phone with an active SIM into a private SMS gateway that will eventually:
- Host a private local HTTP API.
- Send SMS through the phone SIM.
- Receive incoming SMS.
- Store sent and received messages locally.
- Forward incoming SMS to one configured webhook.
- Retry failed webhook deliveries.
- Run through a foreground service.
- Show status and logs in a native UI.
This is a private APK installed manually.
It will not be published to Google Play.
## 3. Required technology
Use:
- Kotlin
- Gradle Kotlin DSL
- Jetpack Compose
- Material 3
- MVVM
- Clean Architecture principles
- Kotlin Coroutines
- StateFlow
- Hilt
- Room dependency placeholders ready for Phase 2
- Ktor dependency placeholders ready for Phase 3
- WorkManager dependency placeholders ready for Phase 6
- Kotlin Serialization
- Java toolchain 17
Use a current stable Android SDK supported by the installed toolchain.
Prefer:
- Minimum SDK 26
- Compile SDK equal to target SDK
- Latest stable target SDK available locally
Do not use:
- Flutter
- React Native
- Xamarin
- .NET MAUI
- Firebase
- Advertising SDKs
- Analytics SDKs
- Cloud backend dependencies
## 4. Required project structure
Create a maintainable package structure similar to:
app/src/main/java/com/sanshare/smsgateway/
- `SmsGatewayApplication.kt`
- `MainActivity.kt`
- `core/constants`
- `core/error`
- `core/result`
- `core/logging`
- `core/security`
- `core/network`
- `core/time`
- `core/validation`
- `data/local`
- `data/repository`
- `domain/model`
- `domain/repository`
- `domain/usecase`
- `service`
- `receiver`
- `http`
- `sms`
- `webhook`
- `security`
- `ui/navigation`
- `ui/theme`
- `ui/component`
- `ui/dashboard`
- `ui/messages`
- `ui/logs`
- `ui/settings`
- `ui/testsms`
- `util`
Do not create meaningless empty files.
Only create files that provide a useful foundation.
## 5. Gradle setup
Configure:
- Android application plugin
- Kotlin Android plugin
- Kotlin serialization plugin
- KSP if required
- Hilt plugin
- Compose
- Material 3
- Lifecycle runtime
- ViewModel Compose
- Navigation Compose
- Coroutines
- Room
- Ktor embedded server
- Ktor content negotiation
- Ktor Kotlin serialization
- Ktor client or OkHttp
- WorkManager
- AndroidX Security Crypto where compatible
- Unit test libraries
- Android instrumentation test libraries
Use a version catalog if appropriate.
Avoid unstable alpha dependencies unless required.
Ensure:
`./gradlew assembleDebug`
can run successfully.
## 6. Application class
Create `SmsGatewayApplication`.
Requirements:
- Annotate for Hilt.
- Perform only lightweight startup.
- Do not start the gateway automatically unless settings later allow it.
- Do not initialize heavy network or database work on the main thread.
## 7. Main activity
Create a Compose-based `MainActivity`.
Requirements:
- Use edge-to-edge safely.
- Host the application navigation.
- Request no permissions immediately without context.
- Show a usable application shell.
- Support light and dark themes.
- Avoid business logic inside the Activity.
## 8. Navigation shell
Create main destinations:
- Dashboard
- Messages
- Logs
- Settings
Use bottom navigation on phones.
Create placeholder screens that are functional enough to compile and navigate.
Each placeholder must state that the feature will be implemented in a later phase.
## 9. Gateway state model
Create a reusable gateway service state model:
- `STOPPED`
- `STARTING`
- `RUNNING`
- `STOPPING`
- `ERROR`
Create immutable UI state for the dashboard.
Prepare interfaces for observing gateway state.
Do not implement the HTTP server yet.
## 10. Foreground service shell
Create `SmsGatewayService`.
Requirements:
- Valid Android Service declaration.
- Hilt-compatible if needed.
- Support explicit start and stop actions.
- Use a notification channel.
- Start as a foreground service correctly.
- Use `START_STICKY` where justified.
- Prevent duplicate initialization.
- Do not start the Ktor server yet.
- Track service state safely.
- Provide a persistent notification.
- Notification title: `SMS Gateway Running`
- Notification body may show that server setup is pending.
- Notification opens the app.
- Add a safe Stop action.
Handle Android version differences.
Use proper PendingIntent flags.
## 11. Manifest foundation
Add permissions required across the project:
- `android.permission.INTERNET`
- `android.permission.ACCESS_NETWORK_STATE`
- `android.permission.ACCESS_WIFI_STATE`
- `android.permission.SEND_SMS`
- `android.permission.RECEIVE_SMS`
- `android.permission.READ_SMS`
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.POST_NOTIFICATIONS`
- `android.permission.WAKE_LOCK`
- `android.permission.RECEIVE_BOOT_COMPLETED`
Add specialized foreground service permission only if required by the chosen target SDK and service type.
Do not add `READ_PHONE_STATE` unless later functionality truly needs it.
Declare:
- Application class
- Main activity
- Foreground service
Prepare receiver declarations only when real receiver classes exist.
## 12. Permission utilities
Create utilities to inspect:
- SEND_SMS
- RECEIVE_SMS
- READ_SMS
- POST_NOTIFICATIONS
- Battery optimization status
Do not request all permissions automatically.
Create a permission-state model usable by the dashboard.
## 13. Battery optimization utility
Create a utility that:
- Detects whether the app is ignoring battery optimizations.
- Creates an Intent to the appropriate settings screen.
- Handles unsupported devices safely.
- Does not crash if no settings activity is available.
## 14. UI design baseline
Use Material 3.
Create reusable components:
- Status card
- Status chip
- Section header
- Warning banner
- Primary operation button
- Confirmation dialog wrapper
The UI must look operational and professional, not like a tutorial template.
Use accessible text sizes and contrast.
Do not hardcode secrets or sample API keys.
## 15. Dashboard shell
Show:
- Gateway status
- Device ID placeholder
- Server port placeholder: 8080
- Device IP placeholder
- SEND_SMS permission state
- RECEIVE_SMS permission state
- Notification permission state
- Battery optimization state
- Start Gateway button
- Stop Gateway button
- Open Settings button
Wire Start and Stop buttons to the foreground service shell.
Require confirmation before stopping.
Do not claim the HTTP server is running yet.
## 16. Error foundation
Create:
- Stable application error model
- Error code enum or sealed hierarchy
- Result wrapper or sealed result type
Include initial errors:
- `SMS_PERMISSION_DENIED`
- `RECEIVE_SMS_PERMISSION_DENIED`
- `NOTIFICATION_PERMISSION_DENIED`
- `PORT_IN_USE`
- `SERVER_START_FAILED`
- `DATABASE_ERROR`
- `INTERNAL_ERROR`
- `INVALID_REQUEST`
Do not expose stack traces in user-facing errors.
## 17. Logging foundation
Create a logging abstraction.
Requirements:
- Debug logging may use Logcat.
- Release logging must not expose secrets.
- Never log API keys.
- Never log webhook secrets.
- Never log authorization headers.
- Avoid full SMS bodies in Logcat.
Database-backed logs will be added later.
