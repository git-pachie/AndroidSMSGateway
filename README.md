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

## Notes

- Local machine files such as `local.properties`, `.gradle/`, and build outputs are ignored by Git.
- The app includes a foreground service: `SmsGatewayService`.
