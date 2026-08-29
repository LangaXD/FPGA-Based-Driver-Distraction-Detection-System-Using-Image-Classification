import sqlite3
from contextlib import contextmanager
from pathlib import Path

# Deliberately a standalone SQLite file, not the VM's existing km_db
# (TimescaleDB/Postgres) container - this project has no reason to share
# a database with the kitchenmate/print4u projects already running here.
DB_PATH = Path(__file__).resolve().parent.parent / "distraction_alerts.db"

# Alert images are stored as plain files (id.jpg), not as blobs in SQLite -
# keeps the DB small and lets them be served directly via FileResponse.
IMAGES_DIR = Path(__file__).resolve().parent.parent / "alert_images"


@contextmanager
def get_db():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    try:
        yield conn
        conn.commit()
    finally:
        conn.close()


def init_db():
    IMAGES_DIR.mkdir(exist_ok=True)
    with get_db() as conn:
        conn.execute("""
            CREATE TABLE IF NOT EXISTS alerts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp TEXT NOT NULL,
                class_name TEXT NOT NULL,
                confidence REAL NOT NULL,
                has_image INTEGER NOT NULL DEFAULT 0
            )
        """)
        # Safe migration for a DB created before has_image existed - SQLite
        # has no "ADD COLUMN IF NOT EXISTS", so check first.
        existing_columns = {row["name"] for row in conn.execute("PRAGMA table_info(alerts)").fetchall()}
        if "has_image" not in existing_columns:
            conn.execute("ALTER TABLE alerts ADD COLUMN has_image INTEGER NOT NULL DEFAULT 0")
        conn.execute("""
            CREATE TABLE IF NOT EXISTS sessions (
                token TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                created_at TEXT NOT NULL,
                expires_at TEXT NOT NULL
            )
        """)
        conn.execute("""
            CREATE TABLE IF NOT EXISTS fcm_tokens (
                fcm_token TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                registered_at TEXT NOT NULL
            )
        """)
