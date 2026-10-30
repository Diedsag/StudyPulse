from flask import Blueprint, jsonify, request

from .. import db
from ..models import Device, utcnow
from ..serializers import device_to_dict

devices_bp = Blueprint("devices", __name__)


@devices_bp.post("/register")
def register_device():
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

    nickname = data.get("nickname")
    rating_opt_in = data.get("rating_opt_in", True)

    device = Device.query.filter_by(device_uid=device_uid).first()
    created = False

    if device is None:
        device = Device(
            device_uid=device_uid,
            nickname=nickname,
            rating_opt_in=bool(rating_opt_in),
        )
        db.session.add(device)
        created = True
    else:
        if nickname is not None:
            device.nickname = nickname

        if "rating_opt_in" in data:
            device.rating_opt_in = bool(rating_opt_in)

        device.last_seen_at = utcnow()

    db.session.commit()

    payload = device_to_dict(device)
    payload["created"] = created
    payload["server_time"] = utcnow().isoformat()

    return jsonify(payload)