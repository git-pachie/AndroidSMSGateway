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

- `src/mobile/android/` Android application source, resources, tests, and Gradle files
- `src/web/` .NET 9 webhook receiver API with Swagger, text logging, and SQLite logging
- `src/mobile/android/Docs/` Android project documentation
- `src/mobile/android/Android_SMS_Gateway_Codex_Prompts/` implementation and planning prompts

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

The Android project also supports the compatibility task path used by older tooling:

```bash
./gradlew :app:assembleDebug
```

## Webhook API

Run the webhook receiver from the repository root:

```bash
cd src/web
dotnet run
```

Default endpoints:

- Health: `http://YOUR_COMPUTER_IP:5111/health`
- Swagger UI: `http://YOUR_COMPUTER_IP:5111/swagger`
- Webhook receiver: `http://YOUR_COMPUTER_IP:5111/webhook`

Webhook logging outputs:

- Text log: `src/web/logs/webhook-executions.log`
- SQLite database: `src/web/logs/webhook-executions.db`

Webhook receiver configuration lives in `src/web/appsettings.json`:

- `Server:Urls`
  Sets the bind address. Default is `http://0.0.0.0:5111`.
- `Webhook:ExpectedBearerToken`
  Shared secret expected from the Android app as `Authorization: Bearer ...`.
- `Webhook:LogFilePath`
  Text log file path.
- `Webhook:DatabasePath`
  SQLite database path.

## Android API And Webhook Setup

Assumptions:

- Android phone API: `http://PHONE_IP:8080`
- Webhook receiver API: `http://COMPUTER_IP:5111`

1. Start the Android gateway service on the phone.
2. Start the .NET webhook receiver with `dotnet run`.
3. If you want to use plain HTTP on the LAN, disable HTTPS-only webhook validation:

```bash
curl -X PUT "http://PHONE_IP:8080/api/settings" \
  -H "Authorization: Bearer YOUR_ANDROID_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "requireHttpsWebhook": false
  }'
```

4. Configure the webhook URL and secret in the Android gateway:

```bash
curl -X POST "http://PHONE_IP:8080/api/settings/webhook" \
  -H "Authorization: Bearer YOUR_ANDROID_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "webhookUrl": "http://COMPUTER_IP:5111/webhook",
    "enabled": true,
    "webhookSecret": "sms-gateway-test-secret"
  }'
```

5. Put the same secret into `src/web/appsettings.json`:

```json
"Webhook": {
  "LogFilePath": "logs/webhook-executions.log",
  "DatabasePath": "logs/webhook-executions.db",
  "ExpectedBearerToken": "sms-gateway-test-secret"
}
```

6. Restart the webhook API after changing `appsettings.json`.
7. Verify Android settings:

```bash
curl -H "Authorization: Bearer YOUR_ANDROID_API_KEY" \
  "http://PHONE_IP:8080/api/settings"
```

8. Trigger a webhook test from Android:

```bash
curl -X POST "http://PHONE_IP:8080/api/webhook/test" \
  -H "Authorization: Bearer YOUR_ANDROID_API_KEY"
```

The Android app currently sends webhook events for:

- Incoming SMS: `sms.received`
- Webhook test: `sms.gateway.test`
- Outgoing accepted/pending: `sms.outgoing.pending`
- Outgoing sent: `sms.outgoing.sent`
- Outgoing failed: `sms.outgoing.failed`
- Outgoing delivered: `sms.outgoing.delivered`

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
