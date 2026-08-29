from typing import Optional

from pydantic import BaseModel


class LoginRequest(BaseModel):
    username: str
    password: str


class LoginResponse(BaseModel):
    token: str


class AlertOut(BaseModel):
    id: int
    timestamp: str
    class_name: str
    confidence: float
    has_image: bool


class FcmTokenRequest(BaseModel):
    fcm_token: str


class AlertEventRequest(BaseModel):
    # Posted by the ZC702 board's alert controller once its hysteresis logic
    # (8 consecutive confident "distracted" ticks) actually fires - this is
    # not called once per classified frame, only once per real alert.
    class_name: str
    confidence: float
    # The frame that triggered the alert, JPEG-encoded then base64'd - optional
    # so callers that can't supply one still work. Lets the Android app show
    # what the driver was actually doing, not just a text label.
    image_base64: Optional[str] = None
