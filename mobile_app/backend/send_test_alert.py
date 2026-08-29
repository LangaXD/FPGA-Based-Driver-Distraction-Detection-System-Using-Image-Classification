#!/usr/bin/env python3
"""Simulates the ZC702 board firing a real distraction alert, for testing the
backend and Android app without needing the physical board connected.
Usage: python3 send_test_alert.py [class_name] [confidence]
"""
import sys
import urllib.request
import json

BASE_URL = "http://127.0.0.1:8001"

class_name = sys.argv[1] if len(sys.argv) > 1 else "texting_left"
confidence = float(sys.argv[2]) if len(sys.argv) > 2 else 0.87

payload = json.dumps({"class_name": class_name, "confidence": confidence}).encode()
req = urllib.request.Request(
    f"{BASE_URL}/api/alert-event",
    data=payload,
    headers={"Content-Type": "application/json"},
    method="POST",
)
with urllib.request.urlopen(req) as resp:
    print(resp.status, resp.read().decode())
