# Distraction Alert Backend

FastAPI service that receives distraction-alert events from the ZC702 board's alert controller and forwards them to the companion Android app via Firebase Cloud Messaging. Runs standalone with its own SQLite database - no external services required beyond an optional Firebase project for push.

## What it does

- `POST /api/alert-event` - called by the board when its hysteresis-based alert controller actually fires (not per-frame). Records the alert and pushes it to every registered phone via Firebase.
- `POST /api/login`, `GET /api/alerts`, `POST /api/fcm-token` - used by the Android app (login, alert history, registering the phone for push notifications).
- `GET /api/health` - plain liveness check.

Full request/response shapes are in `app/models.py` and `app/main.py`.

## Setup

```bash
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

Create `.env` (never commit this):

```
ADMIN_USERNAME=dulhara
ADMIN_PASSWORD_HASH=<bcrypt hash - generate with the snippet below>
PORT=8001
```

Generate the password hash:

```bash
source venv/bin/activate
python3 -c "from passlib.hash import bcrypt; print(bcrypt.hash('your-chosen-password'))"
```

## Firebase Cloud Messaging setup (required for push notifications to actually work)

1. Go to the [Firebase console](https://console.firebase.google.com/), create a new project.
2. Add an Android app to it with package name `com.msc.distractionalert` (must match the Android app's `applicationId` exactly - see `mobile_app/DistractionAlertApp/FIREBASE_SETUP.md`).
3. Download the generated `google-services.json` and place it in the Android project as instructed there.
4. In Firebase console → Project Settings → Service Accounts → "Generate new private key". This downloads a JSON file.
5. Copy that file to this backend directory as `firebase-service-account.json` (same directory as `requirements.txt`, next to `app/`). Never commit it.

Until step 5 is done, the backend still works fully (alerts are recorded, `/api/alerts` still returns them) - push notifications are just silently skipped, logged as a warning.

## Running

```bash
./start.sh   # runs in the background via nohup, logs to backend.log, PID in backend.pid
./stop.sh    # stops it
```

Check it's up: `curl http://127.0.0.1:8001/api/health`

## Testing without the physical board

```bash
source venv/bin/activate
python3 send_test_alert.py texting_left 0.87
```

This fires the exact same endpoint the board's alert controller calls, so it exercises the full push-notification path.

## Board-side integration

`fpga/board_scripts/alert_loop_infer.py` calls `POST /api/alert-event` with `{"class_name": ..., "confidence": ...}` on the same alert rising-edge that sounds the physical buzzer - so a real alert on the board shows up in the app within a couple of seconds. Point it at wherever this backend is actually deployed by setting the request URL in that script to your own host.
