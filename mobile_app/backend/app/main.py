import base64
from datetime import datetime, timezone

from fastapi import Depends, FastAPI, HTTPException
from fastapi.responses import FileResponse

from .auth import create_session, require_auth, verify_password
from .database import IMAGES_DIR, get_db, init_db
from .firebase_push import send_alert_push
from .models import AlertEventRequest, AlertOut, FcmTokenRequest, LoginRequest, LoginResponse

app = FastAPI(
    title="Distraction Alert Backend",
    description=(
        "Receives distraction-alert events from the ZC702 board's hardware "
        "alert controller and forwards them to the companion Android app via "
        "Firebase Cloud Messaging. MSc project support service - not a "
        "production system."
    ),
)


@app.on_event("startup")
def on_startup():
    init_db()


@app.get("/api/health")
def health():
    return {"status": "ok"}


@app.post("/api/login", response_model=LoginResponse)
def login(body: LoginRequest):
    if not verify_password(body.username, body.password):
        raise HTTPException(status_code=401, detail="Invalid username or password")
    token = create_session(body.username)
    return LoginResponse(token=token)


@app.get("/api/alerts", response_model=list[AlertOut])
def list_alerts(username: str = Depends(require_auth)):
    with get_db() as conn:
        rows = conn.execute(
            "SELECT id, timestamp, class_name, confidence, has_image FROM alerts ORDER BY id DESC LIMIT 200"
        ).fetchall()
    return [
        AlertOut(id=row["id"], timestamp=row["timestamp"], class_name=row["class_name"],
                  confidence=row["confidence"], has_image=bool(row["has_image"]))
        for row in rows
    ]


@app.get("/api/alerts/{alert_id}/image")
def get_alert_image(alert_id: int, username: str = Depends(require_auth)):
    image_path = IMAGES_DIR / f"{alert_id}.jpg"
    if not image_path.exists():
        raise HTTPException(status_code=404, detail="No image recorded for this alert")
    return FileResponse(image_path, media_type="image/jpeg")


@app.post("/api/fcm-token")
def register_fcm_token(body: FcmTokenRequest, username: str = Depends(require_auth)):
    now = datetime.now(timezone.utc).isoformat()
    with get_db() as conn:
        conn.execute(
            "INSERT INTO fcm_tokens (fcm_token, username, registered_at) VALUES (?, ?, ?) "
            "ON CONFLICT(fcm_token) DO UPDATE SET username=excluded.username, registered_at=excluded.registered_at",
            (body.fcm_token, username, now),
        )
    return {"status": "registered"}


@app.post("/api/alert-event")
def receive_alert_event(body: AlertEventRequest):
    # Called by the ZC702 board itself (fpga/ alert-controller software
    # bridge), not by the phone app - no login step for the board, since it
    # is a fixed, physically-controlled piece of hardware on the same
    # project, not a third-party client. If this needs to be locked down
    # further later, a shared-secret header would be the next step.
    now = datetime.now(timezone.utc).isoformat()

    image_bytes = None
    if body.image_base64:
        try:
            image_bytes = base64.b64decode(body.image_base64)
        except Exception:
            image_bytes = None  # malformed data - still record the alert, just without an image

    with get_db() as conn:
        cursor = conn.execute(
            "INSERT INTO alerts (timestamp, class_name, confidence, has_image) VALUES (?, ?, ?, ?)",
            (now, body.class_name, body.confidence, int(image_bytes is not None)),
        )
        alert_id = cursor.lastrowid
        tokens = [row["fcm_token"] for row in conn.execute("SELECT fcm_token FROM fcm_tokens").fetchall()]

    if image_bytes is not None:
        (IMAGES_DIR / f"{alert_id}.jpg").write_bytes(image_bytes)

    sent = send_alert_push(tokens, body.class_name, body.confidence, now)
    return {
        "status": "recorded",
        "alert_id": alert_id,
        "has_image": image_bytes is not None,
        "push_notifications_sent": sent,
    }
