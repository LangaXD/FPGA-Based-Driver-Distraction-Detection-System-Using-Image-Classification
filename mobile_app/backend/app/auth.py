import os
import secrets
from datetime import datetime, timedelta, timezone

import bcrypt
from dotenv import load_dotenv
from fastapi import Header, HTTPException

from .database import get_db

# Loaded here (not via shell `source .env`) specifically because the admin
# password hash contains literal `$` characters that bash's `source` would
# try to expand as variable references, silently corrupting the hash.
load_dotenv()

# Single-user research prototype, not a multi-tenant production auth system -
# the admin username/password hash live in environment variables (.env),
# never in source control. Session tokens are opaque random strings stored
# server-side with an expiry, which is enough for a demo/viva but is not
# meant to be a hardened auth design.
#
# Uses the `bcrypt` package directly rather than passlib: passlib 1.7.4 (the
# last release) is incompatible with bcrypt>=4.1 (it probes a `__about__`
# attribute bcrypt removed), a known unresolved upstream issue - simplest fix
# is to skip the abandoned wrapper library entirely.
ADMIN_USERNAME = os.environ.get("ADMIN_USERNAME", "dulhara")
ADMIN_PASSWORD_HASH = os.environ.get("ADMIN_PASSWORD_HASH", "")
SESSION_LIFETIME_HOURS = 24 * 7


def verify_password(username: str, password: str) -> bool:
    if username != ADMIN_USERNAME or not ADMIN_PASSWORD_HASH:
        return False
    return bcrypt.checkpw(password.encode(), ADMIN_PASSWORD_HASH.encode())


def create_session(username: str) -> str:
    token = secrets.token_urlsafe(32)
    now = datetime.now(timezone.utc)
    expires = now + timedelta(hours=SESSION_LIFETIME_HOURS)
    with get_db() as conn:
        conn.execute(
            "INSERT INTO sessions (token, username, created_at, expires_at) VALUES (?, ?, ?, ?)",
            (token, username, now.isoformat(), expires.isoformat()),
        )
    return token


def require_auth(authorization: str = Header(default="")) -> str:
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Missing or malformed Authorization header")
    token = authorization.removeprefix("Bearer ").strip()
    with get_db() as conn:
        row = conn.execute("SELECT username, expires_at FROM sessions WHERE token = ?", (token,)).fetchone()
    if row is None:
        raise HTTPException(status_code=401, detail="Invalid session token")
    if datetime.fromisoformat(row["expires_at"]) < datetime.now(timezone.utc):
        raise HTTPException(status_code=401, detail="Session expired")
    return row["username"]
