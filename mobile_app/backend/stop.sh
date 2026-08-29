#!/usr/bin/env bash
cd "$(dirname "$0")"
if [ -f backend.pid ]; then
	kill "$(cat backend.pid)" 2>/dev/null && echo "Stopped." || echo "Process not running (stale pid file)."
	rm -f backend.pid
else
	echo "No backend.pid file found - is it running? (ss -tlnp | grep 8001)"
fi
