# Android SMS Gateway — Codex Prompt Pack
This package contains phased prompts for building the Android SMS Gateway application.
## How to use
1. Extract this ZIP inside or beside your Android project repository.
2. Open Codex at the repository root.
3. Run the prompts in filename order.
4. Copy and paste one prompt at a time into Codex.
5. Do not proceed to the next prompt until the current phase builds successfully.
6. Keep all files from earlier phases. Later phases extend the same project.
7. If Codex reports build errors, ask it to fix them before continuing.
## Prompt order
1. `01_PROJECT_FOUNDATION.md`
2. `02_DATABASE_AND_SECURITY.md`
3. `03_HTTP_SERVER_AND_API_CORE.md`
4. `04_OUTGOING_SMS.md`
5. `05_INCOMING_SMS.md`
6. `06_WEBHOOK_AND_RETRY.md`
7. `07_COMPOSE_UI_DASHBOARD_MESSAGES.md`
8. `08_SETTINGS_LOGS_AND_OPERATIONS.md`
9. `09_TESTING_HARDENING_DOCUMENTATION.md`
10. `10_FINAL_AUDIT_AND_RELEASE.md`
## Project baseline
- Application name: Android SMS Gateway
- Package: `com.sanshare.smsgateway`
- Kotlin
- Jetpack Compose
- Material 3
- Hilt
- Room
- Ktor embedded server
- WorkManager
- Foreground service
- Android BroadcastReceiver
- SmsManager
- Private APK / sideload only
- No Google Play publication
- No Firebase
- No arbitrary URL execution from SMS content
## Important operating rule
Every prompt instructs Codex to inspect the existing repository before changing anything. Codex must preserve previous work, create actual files, run Gradle builds, and fix compilation problems before ending each phase.
## Security warning
This gateway must only be used on a trusted LAN or private VPN. Do not expose the Android HTTP server directly to the public internet.

## Exact generated execution order
1. `01_PROJECT_FOUNDATION_PART_A.md`
2. `01_PROJECT_FOUNDATION_PART_B.md`
3. `02_DATABASE_AND_SECURITY_PART_A.md`
4. `02_DATABASE_AND_SECURITY_PART_B.md`
5. `03_HTTP_SERVER_AND_API_CORE_PART_A.md`
6. `03_HTTP_SERVER_AND_API_CORE_PART_B.md`
7. `04_OUTGOING_SMS_PART_A.md`
8. `04_OUTGOING_SMS_PART_B.md`
9. `05_INCOMING_SMS.md`
10. `06_WEBHOOK_AND_RETRY_PART_A.md`
11. `06_WEBHOOK_AND_RETRY_PART_B.md`
12. `07_COMPOSE_UI_DASHBOARD_MESSAGES_PART_A.md`
13. `07_COMPOSE_UI_DASHBOARD_MESSAGES_PART_B.md`
14. `08_SETTINGS_LOGS_AND_OPERATIONS_PART_A.md`
15. `08_SETTINGS_LOGS_AND_OPERATIONS_PART_B.md`
16. `09_TESTING_HARDENING_DOCUMENTATION_PART_A.md`
17. `09_TESTING_HARDENING_DOCUMENTATION_PART_B.md`
18. `10_FINAL_AUDIT_AND_RELEASE.md`
