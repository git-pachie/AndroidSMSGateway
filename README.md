# Android SMS Gateway

Android SMS Gateway is an Android app that exposes SMS gateway capabilities from a device through a local HTTP server. The project combines a Compose-based UI, a foreground service, local persistence, and network APIs for sending, receiving, and monitoring SMS activity.

## Project Overview

- Package: `com.sanshare.smsgateway`
- Minimum SDK: Android 8.0 (API 26)
- Target SDK: Android 16 / API 36
- Language: Kotlin
- UI: Jetpack Compose
- DI: Hilt
- Database: Room
- HTTP stack: Ktor with Netty

## Repository Layout

- `app/` Android application source, resources, tests, and Room schemas
- `Docs/` project documentation
- `Android_SMS_Gateway_Codex_Prompts/` implementation and planning prompts
- `gradle/` Gradle version catalog and wrapper files

## Requirements

- Android Studio with Android SDK for API 36
- JDK 17
- An Android device or emulator for testing

## Build

Use the Gradle wrapper from the repository root:

```bash
./gradlew assembleDebug
```

Run unit tests with:

```bash
./gradlew testDebugUnitTest
```

## Android Permissions

The app requests permissions needed for SMS delivery, message reception, notifications, network access, boot handling, wake locks, and battery optimization handling. Test on a real device if you need end-to-end SMS behavior.

## Manual Receive Test

1. Grant `RECEIVE_SMS` and `SEND_SMS`.
2. Start the gateway from the app.
3. Send an SMS to the gateway phone number.
4. Confirm the inbox list shows one logical received message.
5. For a long multipart SMS, confirm the app stores one combined inbox record rather than one item per segment.
6. Configure a fixed webhook URL and use the dashboard `Test Webhook` action.
7. Confirm webhook attempts appear in the inbox detail view and in `GET /api/webhook/attempts`.
8. Enable Auto Start, reboot the device, and confirm the gateway service starts again after boot.

## Webhook Troubleshooting

- Incoming SMS text never controls the destination URL. Only the saved webhook URL is used.
- If webhook forwarding is disabled, received SMS stays stored locally with `DISABLED` status.
- If retries do not run immediately, check network connectivity and WorkManager battery restrictions.
- Retry delay is exponential and subject to WorkManager minimum backoff rules.
- TLS certificate failures are treated as permanent webhook errors.
- Use `POST /api/webhook/test` or the dashboard `Test Webhook` button before relying on live traffic.

## Notes

- Local machine files such as `local.properties`, `.gradle/`, and build outputs are ignored by Git.
- The app includes a foreground service: `SmsGatewayService`.
