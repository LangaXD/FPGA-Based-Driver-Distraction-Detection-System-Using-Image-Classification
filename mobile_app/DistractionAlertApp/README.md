# Distraction Alert App

Android companion app for the driver-distraction project. Receives real-time alerts (pushed via Firebase Cloud Messaging from the FastAPI backend in `mobile_app/backend/`, which in turn receives them from the ZC702 board's alert controller) and shows alert history and a per-class frequency chart.

## What's implemented

- **Login** (`ui.login`) - username/password against `POST /api/login`, bearer token stored in plain `SharedPreferences` (see the comment in `data/local/PrefsManager.kt` for why that's an acceptable tradeoff for a single-user research prototype rather than a production auth surface).
- **Alert history** (`ui.history`) - RecyclerView + pull-to-refresh, backed by a Room database that caches `GET /api/alerts` responses and stores alerts pushed by FCM so they appear immediately, before the next refresh.
- **Stats chart** (`ui.chart`) - MPAndroidChart bar chart of alert count per distraction class, sharing the same `AlertsViewModel`/Room data as History via a `TabLayout` + `ViewPager2` on `MainActivity`.
- **Settings** (`ui.settings`) - editable server base URL, a push-notification toggle, and logout.
- **FCM integration** (`fcm.DistractionFirebaseMessagingService`) - handles data-message pushes, shows a heads-up notification, caches the alert locally, and posts new/refreshed tokens to `POST /api/fcm-token`.

Architecture: Retrofit + OkHttp for networking, Kotlin coroutines throughout, Room for local persistence, and a ViewModel + repository layer per feature (`AuthRepository`, `AlertRepository`). No DI framework - the object graph is small enough that `DistractionAlertApp` (the `Application` subclass) wiring up two repositories by hand is simpler than adding Hilt/Koin.

Compiles and runs cleanly (`BUILD SUCCESSFUL`), tested on a real emulator.

## First-time setup

1. **A Firebase project of your own.** The app builds against the Firebase Messaging SDK but ships without a real project connected - see [`FIREBASE_SETUP.md`](FIREBASE_SETUP.md) for the steps. Without a real `app/google-services.json`, the build fails at the `google-services` Gradle plugin step.
2. **Gradle sync in Android Studio.** The Gradle wrapper jar isn't committed (standard practice for a binary file); Android Studio regenerates it on first sync, or run `gradle wrapper --gradle-version 8.7` with a local Gradle install.
3. **A backend to talk to** - point Settings → Server URL at wherever `mobile_app/backend/` ends up running (defaults to `http://10.0.2.2:8001`, the emulator's alias for your dev machine's localhost).
4. **A device or emulator with Google Play services** for FCM to work - most emulator images with a Play Store icon work, as do "Google APIs" images.
5. **Launcher icon** - `app/src/main/res/drawable/ic_launcher.xml` is a simple hand-drawn placeholder (steering wheel + alert dot). Regenerate a proper adaptive icon via Android Studio's Image Asset tool if you want something more polished for a demo.

## Backend API contract this app was built against

- `POST /api/login` - `{"username", "password"}` -> `200 {"token"}` / `401`
- `GET /api/alerts` - `Authorization: Bearer <token>` -> `200 [{"id", "timestamp", "class_name", "confidence"}, ...]`, newest first
- `POST /api/fcm-token` - `Authorization: Bearer <token>`, `{"fcm_token"}` -> `200`
- FCM data payload: `{"title", "body", "class_name", "confidence", "timestamp"}`

See `data/remote/ApiService.kt` and `fcm/DistractionFirebaseMessagingService.kt` for exactly how each field is used.

## Versions targeted

- Android Gradle Plugin 8.5.2, Gradle 8.7
- Kotlin 1.9.24 (JVM target 17)
- `compileSdk` / `targetSdk` 34, `minSdk` 24
- Key libraries: Retrofit 2.11.0, OkHttp 4.12.0 (logging interceptor included, level `BASIC`), Room 2.6.1, Firebase BOM 33.1.2, MPAndroidChart 3.1.0 (via JitPack, declared in `settings.gradle`), Material Components 1.12.0, Lifecycle/ViewModel 2.8.4

## Project layout

```
app/src/main/java/com/msc/distractionalert/
  DistractionAlertApp.kt        Application subclass; manual DI + notification channel
  data/
    model/                      Alert (Room entity), DTOs, request/response shapes
    local/                      Room (AlertDao, AppDatabase) + PrefsManager (SharedPreferences)
    remote/                     ApiService (Retrofit interface), RetrofitProvider
    repository/                 AuthRepository, AlertRepository
  ui/
    login/                      LoginActivity, LoginViewModel
    main/                       MainActivity (tabs host), AlertsViewModel (shared)
    history/                    HistoryFragment, AlertAdapter
    chart/                      ChartFragment (MPAndroidChart bar chart)
    settings/                   SettingsActivity, SettingsViewModel
  fcm/                          DistractionFirebaseMessagingService
  util/                         DistractionClasses (class name <-> display name), TimeFormat
```
