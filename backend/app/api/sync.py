import json
from datetime import datetime, timedelta, timezone

from flask import Blueprint, jsonify, request

from .. import db
from ..models import Device, SyncEvent, utcnow
from ..xp import update_device_progress

sync_bp = Blueprint("sync", __name__)

MAX_BATCH_SIZE = 100
FUTURE_TOLERANCE_MINUTES = 5


def parse_occurred_at(value):
    if not isinstance(value, str) or not value.strip():
        return None

    try:
        normalized = value.strip().replace("Z", "+00:00")
        parsed = datetime.fromisoformat(normalized)

        if parsed.tzinfo is None:
            parsed = parsed.replace(tzinfo=timezone.utc)

        return parsed.astimezone(timezone.utc)
    except Exception:
        return None


def as_int(value):
    if isinstance(value, bool):
        return None

    try:
        return int(value)
    except Exception:
        return None


def xp_for_event(event_type: str, payload: dict):
    if event_type == "CARD_REVIEWED":
        rating = as_int(payload.get("rating"))
        if rating not in (1, 2, 3, 4):
            return None
        return rating

    if event_type == "SLOT_COMPLETED":
        duration_minutes = as_int(payload.get("duration_minutes"))
        if duration_minutes is None or duration_minutes < 0:
            return None
        return 5 + min(duration_minutes // 15, 10)

    if event_type == "PLAN_ACCEPTED":
        return 2

    if event_type == "CLASS_CANCELLED":
        return 0

    if event_type == "PACKAGE_DOWNLOADED":
        return 1

    if event_type == "TASK_COMPLETED":
        return 10

    return None


@sync_bp.post("/batch")
def sync_batch():
    data = request.get_json(silent=True) or {}

    device_uid = data.get("device_uid")
    if not device_uid:
        return (
            jsonify(
                {
                    "error": "invalid_request",
                    "message": "device_uid is required",
                }
            ),
            400,
        )

    device = Device.query.filter_by(device_uid=device_uid).first()
    if device is None:
        return (
            jsonify(
                {
                    "error": "not_found",
                    "message": "device_uid not registered",
                }
            ),
            404,
        )

    events = data.get("events")
    if not isinstance(events, list):
        return (
            jsonify(
                {
                    "error": "invalid_request",
                    "message": "events must be a list",
                }
            ),
            400,
        )

    if len(events) > MAX_BATCH_SIZE:
        return (
            jsonify(
                {
                    "error": "invalid_request",
                    "message": "too many events in batch",
                }
            ),
            413,
        )

    server_now = utcnow()
    server_date = server_now.date()
    max_future = server_now + timedelta(minutes=FUTURE_TOLERANCE_MINUTES)

    accepted = 0
    duplicated = 0
    rejected = 0
    xp_awarded_total = 0

    for event in events:
        if not isinstance(event, dict):
            rejected += 1
            continue

        client_event_id = event.get("client_event_id")
        event_type = event.get("event_type")
        occurred_raw = event.get("occurred_at")
        payload = event.get("payload")

        if (
            not isinstance(client_event_id, str)
            or not client_event_id.strip()
            or not isinstance(event_type, str)
            or not event_type.strip()
            or not isinstance(payload, dict)
        ):
            rejected += 1
            continue

        exists = SyncEvent.query.filter_by(client_event_id=client_event_id).first()
        if exists is not None:
            duplicated += 1
            continue

        occurred_at = parse_occurred_at(occurred_raw)
        if occurred_at is None or occurred_at > max_future:
            rejected += 1
            continue

        awarded_xp = xp_for_event(event_type, payload)
        if awarded_xp is None:
            rejected += 1
            continue

        sync_event = SyncEvent(
            client_event_id=client_event_id,
            device_uid=device_uid,
            event_type=event_type,
            occurred_at=occurred_at,
            payload_json=json.dumps(payload, ensure_ascii=False),
            xp_awarded=awarded_xp,
        )

        db.session.add(sync_event)

        update_device_progress(
            device=device,
            awarded_xp=awarded_xp,
            event_type=event_type,
            event_date=occurred_at.date(),
            now_date=server_date,
        )

        accepted += 1
        xp_awarded_total += awarded_xp

    db.session.commit()

    return jsonify(
        {
            "accepted": accepted,
            "duplicated": duplicated,
            "rejected": rejected,
            "xp_awarded_total": xp_awarded_total,
            "total_xp": device.total_xp,
            "weekly_xp": device.weekly_xp,
            "level": device.level,
            "streak_days": device.streak_days,
            "server_time": server_now.isoformat(),
        }
    )