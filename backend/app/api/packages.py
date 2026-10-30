from flask import Blueprint, jsonify, request

from ..models import (
    Device,
    PackageCard,
    PackageMaterial,
    PackageScheduleItem,
    PackageTask,
    StudyPackage,
    utcnow,
)
from ..serializers import (
    card_to_dict,
    material_to_dict,
    package_to_dict,
    schedule_item_to_dict,
    task_to_dict,
)

packages_bp = Blueprint("packages", __name__)


def _device_or_none(device_uid):
    if not device_uid:
        return None
    return Device.query.filter_by(device_uid=device_uid).first()


@packages_bp.get("")
def list_packages():
    device_uid = request.args.get("device_uid")
    device = _device_or_none(device_uid)

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

    packages = (
        StudyPackage.query.filter(
            (StudyPackage.is_public.is_(True))
            | (StudyPackage.target_device_uid == device_uid)
        )
        .order_by(StudyPackage.updated_at.desc())
        .all()
    )

    return jsonify(
        {
            "packages": [package_to_dict(package) for package in packages],
            "server_time": utcnow().isoformat(),
        }
    )


@packages_bp.get("/<external_id>/bundle")
def get_package_bundle(external_id):
    device_uid = request.args.get("device_uid")
    device = _device_or_none(device_uid)

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

    package = StudyPackage.query.filter_by(external_id=external_id).first()

    if package is None:
        return (
            jsonify(
                {
                    "error": "not_found",
                    "message": "package not found",
                }
            ),
            404,
        )

    if not package.is_public and package.target_device_uid != device_uid:
        return (
            jsonify(
                {
                    "error": "forbidden",
                    "message": "package is not available for this device",
                }
            ),
            403,
        )

    schedule = (
        PackageScheduleItem.query.filter_by(package_external_id=external_id)
        .order_by(PackageScheduleItem.day_of_week, PackageScheduleItem.start_time)
        .all()
    )

    tasks = (
        PackageTask.query.filter_by(package_external_id=external_id)
        .order_by(PackageTask.deadline_date, PackageTask.priority.desc())
        .all()
    )

    materials = (
        PackageMaterial.query.filter_by(package_external_id=external_id)
        .order_by(PackageMaterial.title)
        .all()
    )

    cards = (
        PackageCard.query.filter_by(package_external_id=external_id)
        .order_by(PackageCard.position)
        .all()
    )

    return jsonify(
        {
            "package": package_to_dict(package),
            "schedule": [schedule_item_to_dict(item) for item in schedule],
            "tasks": [task_to_dict(task) for task in tasks],
            "materials": [material_to_dict(material) for material in materials],
            "cards": [card_to_dict(card) for card in cards],
            "server_time": utcnow().isoformat(),
        }
    )