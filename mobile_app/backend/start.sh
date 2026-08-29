#!/usr/bin/env bash
# Starts the backend with nohup so it survives the SSH session ending.
# Deliberately not a systemd unit: this keeps the footprint on this shared
# VM to "files in my own home directory" only, nothing touching root-owned
# system configuration. See README.md for how to promote it to a systemd
# --user service later if it needs to survive a VM reboot unattended.
#
# .env is loaded by the app itself (app/auth.py, via python-dotenv), not by
# `source .env` here - the admin password hash contains literal `$`
# characters that bash would otherwise try to expand as variables.
cd "$(dirname "$0")"
source venv/bin/activate
PORT=$(grep -E '^PORT=' .env | cut -d= -f2)
nohup uvicorn app.main:app --host 0.0.0.0 --port "${PORT:-8001}" > backend.log 2>&1 &
echo "Started, PID $!"
echo $! > backend.pid
