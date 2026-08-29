# Firebase Cloud Messaging setup

The app is wired up to receive push notifications via Firebase Cloud
Messaging (FCM), but it ships without a real Firebase project connected -
that's a one-time setup step tied to your own Google account, not
something that can be baked into the repo. `app/google-services.json.PLACEHOLDER`
explains this in-place; without a real `app/google-services.json`, the
`com.google.gms.google-services` Gradle plugin fails the build at sync
time, so this needs to be done before the app will build at all. The
steps below walk through creating that file for real.

## 1. Create the Firebase project

1. Go to https://console.firebase.google.com and sign in with any Google
   account (your university or personal account is fine - this is a
   student research prototype, not a managed org project).
2. Click **Add project**, give it any name (e.g. "Distraction Alert"),
   and finish the wizard (Google Analytics is not needed - you can
   disable it).

## 2. Register the Android app

1. In the new project's console, click the Android icon ("Add app").
2. **Android package name**: enter exactly
   ```
   com.msc.distractionalert
   ```
   This must match `applicationId` and `namespace` in `app/build.gradle`.
   If you ever rename the package, update it in both places.
3. App nickname and debug signing certificate SHA-1 are optional for FCM
   to work (SHA-1 only matters for Dynamic Links / Auth, not messaging) -
   you can skip that field.
4. Click **Register app**.

## 3. Download and place the config file

1. Firebase will offer a `google-services.json` download - download it.
2. Move it into the project at:
   ```
   app/google-services.json
   ```
   (same folder as `app/build.gradle`).
3. You can leave `app/google-services.json.PLACEHOLDER` in place or delete
   it - it's inert either way (the `.PLACEHOLDER` extension means Gradle
   never reads it).

## 4. Sync and build

1. Open the project in Android Studio.
2. Let Gradle sync (it should pick up the new `google-services.json`
   automatically - the `com.google.gms.google-services` plugin is already
   applied in `app/build.gradle` and the classpath is already declared in
   the root `build.gradle`).
3. Build and run on a device or emulator with Google Play services
   installed (most emulator images with a Play Store icon in the AVD
   Manager work; bare "Google APIs" images also work for FCM).

## 5. Verify the token reaches your backend

On first successful login, `LoginActivity` fetches the current FCM
registration token and POSTs it to `{server_url}/api/fcm-token`. You can
confirm this is happening by:

- Checking Logcat for OkHttp's request log line (`--> POST .../fcm-token`),
  enabled via the `HttpLoggingInterceptor` in `RetrofitProvider`.
- Checking the backend's logs/database for the received token.

If the token never arrives, the most common causes are: the backend
`server_url` in Settings is wrong or unreachable from the device/emulator,
or `google-services.json` doesn't match the app's package name.

## 6. Send a test push from the backend

The backend should send an FCM **data message** (not a plain notification
message) shaped like:

```json
{
  "message": {
    "token": "<the device's FCM token>",
    "data": {
      "title": "Distraction detected",
      "body": "Texting Right - 92% confidence",
      "class_name": "texting_right",
      "confidence": "0.92",
      "timestamp": "2026-08-15T14:32:07Z"
    }
  }
}
```

`DistractionFirebaseMessagingService.onMessageReceived()` reads exactly
these five data keys. Using a notification message instead of a data
message will NOT reliably trigger `onMessageReceived()` while the app is
backgrounded, which is why the backend must send a data-only payload.
