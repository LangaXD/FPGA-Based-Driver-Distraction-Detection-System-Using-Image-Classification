import logging
import os
from pathlib import Path

logger = logging.getLogger("distraction_alert_backend")

CREDENTIALS_PATH = Path(__file__).resolve().parent.parent / "firebase-service-account.json"

_firebase_app = None
_init_attempted = False


def _get_firebase_app():
    # Lazily initialised, and allowed to simply not be configured yet: this
    # backend must stay usable (alerts still recorded, /api/alerts still
    # works) even before a real Firebase project exists, exactly like the
    # Android app's own placeholder google-services.json situation.
    global _firebase_app, _init_attempted
    if _init_attempted:
        return _firebase_app
    _init_attempted = True
    if not CREDENTIALS_PATH.exists():
        logger.warning(
            "firebase-service-account.json not found at %s - push notifications are disabled "
            "until a real Firebase project's service account key is placed there.",
            CREDENTIALS_PATH,
        )
        return None
    try:
        import firebase_admin
        from firebase_admin import credentials

        cred = credentials.Certificate(str(CREDENTIALS_PATH))
        _firebase_app = firebase_admin.initialize_app(cred)
    except Exception:
        logger.exception("Failed to initialise Firebase Admin SDK")
        _firebase_app = None
    return _firebase_app


def send_alert_push(fcm_tokens: list[str], class_name: str, confidence: float, timestamp: str) -> int:
    """Send an FCM data message to every registered device. Returns how many succeeded.
    Silently sends zero if Firebase isn't configured yet - the alert is still recorded
    in the database either way, this only affects the push notification."""
    app = _get_firebase_app()
    if app is None or not fcm_tokens:
        return 0

    from firebase_admin import messaging

    sent = 0
    for token in fcm_tokens:
        message = messaging.Message(
            data={
                "title": "Distraction alert",
                "body": f"Sustained {class_name.replace('_', ' ')} detected ({confidence:.0%} confidence)",
                "class_name": class_name,
                "confidence": str(confidence),
                "timestamp": timestamp,
            },
            token=token,
        )
        try:
            messaging.send(message, app=app)
            sent += 1
        except Exception:
            logger.exception("Failed to send FCM push to token ending ...%s", token[-6:])
    return sent
