from flask import Blueprint, jsonify

from ..models import utcnow

health_bp = Blueprint("health", __name__)


@health_bp.get("/health")
def health():
    return jsonify(
        {
            "status": "ok",
            "service": "studypulse-api",
            "time": utcnow().isoformat(),
        }
    )