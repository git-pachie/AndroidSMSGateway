# Android SMS Gateway

Android SMS Gateway is an Android app that exposes SMS gateway capabilities from a device through a local HTTP server. This repository also includes a .NET 9 webhook receiver API for capturing incoming and outgoing SMS webhook events.

## Repository Layout

- `src/mobile/android/` Android application source, resources, tests, and Gradle files
- `src/web/` .NET 9 webhook receiver API with Swagger, text logging, and SQLite logging

## Requirements

- JDK 17
- Android Studio with Android SDK for API 36
- .NET 9 SDK
- An Android device or emulator for testing

## Build The Android App

From the repository root:

```bash
./gradlew assembleDebug
```

Run unit tests:

```bash
./gradlew testDebugUnitTest
```

The compatibility task path is also available:

```bash
./gradlew :app:assembleDebug
```

## Run The Webhook API

From the repository root:

```bash
cd src/web
dotnet run
```

Default URLs:

- Health: `http://YOUR_COMPUTER_IP:5111/health`
- Swagger UI: `http://YOUR_COMPUTER_IP:5111/swagger`
- Webhook endpoint: `http://YOUR_COMPUTER_IP:5111/webhook`

Webhook logs are written to:

- Text log: `src/web/logs/webhook-executions.log`
- SQLite database: `src/web/logs/webhook-executions.db`

## Configure Android API And Webhook

Assumptions:

- Android phone API: `http://PHONE_IP:8080`
- Webhook receiver API: `http://COMPUTER_IP:5111`

1. Start the Android gateway service on the phone.
2. Start the .NET webhook receiver with `dotnet run`.
3. If you are using plain HTTP on your LAN, disable HTTPS-only webhook validation:

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
{
  "Webhook": {
    "LogFilePath": "logs/webhook-executions.log",
    "DatabasePath": "logs/webhook-executions.db",
    "ExpectedBearerToken": "sms-gateway-test-secret"
  }
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

## Webhook Events

The Android app sends these webhook event types:

- `sms.received`
- `sms.gateway.test`
- `sms.outgoing.pending`
- `sms.outgoing.sent`
- `sms.outgoing.failed`
- `sms.outgoing.delivered`

## Notes

- The Android-specific README remains at `src/mobile/android/README.md`.
- Local build outputs, logs, and machine-specific files are ignored by Git.
