from datetime import date

from flask import Blueprint, jsonify, request

from .. import db
from ..models import Device, utcnow
from ..xp import calculate_level, current_week_key

stats_bp = Blueprint("stats", __name__)


def ensure_current_week(device, today: date) -> bool:
    period_key = current_week_key(today)

    if device.weekly_period_key != period_key:
        device.weekly_period_key = period_key
        device.weekly_xp = 0
        return True

    return False


def device_stats_payload(device, today: date):
    period_key = current_week_key(today)

    weekly_xp = device.weekly_xp or 0
    if device.weekly_period_key != period_key:
        weekly_xp = 0

    total_xp = device.total_xp or 0
    level = device.level or calculate_level(total_xp)

    return {
        "device_uid": device.device_uid,
        "total_xp": total_xp,
        "weekly_xp": weekly_xp,
        "level": level,
        "streak_days": device.streak_days or 0,
        "rating_opt_in": bool(device.rating_opt_in),
        "weekly_period_key": period_key,
        "server_time": utcnow().isoformat(),
    }


@stats_bp.get("/me/stats")
def me_stats():
    device_uid = request.args.get("device_uid")

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

    today = date.today()
    changed = ensure_current_week(device, today)

    if changed:
        db.session.commit()

    return jsonify(device_stats_payload(device, today))


@stats_bp.get("/leaderboard")
def leaderboard():
    device_uid = request.args.get("device_uid")

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

    current_device = Device.query.filter_by(device_uid=device_uid).first()

    if current_device is None:
        return (
            jsonify(
                {
                    "error": "not_found",
                    "message": "device_uid not registered",
                }
            ),
            404,
        )

    period = (request.args.get("period") or "week").strip().lower()
    if period not in {"week", "all"}:
        period = "week"

    try:
        limit = int(request.args.get("limit", 20))
    except Exception:
        limit = 20

    limit = max(1, min(limit, 50))

    today = date.today()

    candidates = Device.query.filter_by(rating_opt_in=True).all()
    changed = False

    for device in candidates:
        if ensure_current_week(device, today):
            changed = True

    if changed:
        db.session.commit()

    if period == "week":
        candidates.sort(
            key=lambda device: (
                -(device.weekly_xp or 0),
                -(device.total_xp or 0),
                (device.nickname or "").lower(),
            )
        )
    else:
        candidates.sort(
            key=lambda device: (
                -(device.total_xp or 0),
                -(device.weekly_xp or 0),
                (device.nickname or "").lower(),
            )
        )

    entries = []

    for rank, device in enumerate(candidates[:limit], start=1):
        score = device.weekly_xp if period == "week" else device.total_xp

        entries.append(
            {
                "rank": rank,
                "device_uid": device.device_uid,
                "nickname": device.nickname,
                "score": score or 0,
                "level": device.level or 1,
                "streak_days": device.streak_days or 0,
                "is_current_user": device.device_uid == device_uid,
            }
        )

    return jsonify(
        {
            "period": period,
            "entries": entries,
            "server_time": utcnow().isoformat(),
        }
    )