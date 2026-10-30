import json
from datetime import date, datetime


def _iso_dt(value):
    if isinstance(value, datetime):
        return value.isoformat()
    return None


def _iso_date(value):
    if isinstance(value, date) and not isinstance(value, datetime):
        return value.isoformat()
    return None


def device_to_dict(device):
    return {
        "device_uid": device.device_uid,
        "nickname": device.nickname,
        "rating_opt_in": device.rating_opt_in,
        "total_xp": device.total_xp,
        "weekly_xp": device.weekly_xp,
        "level": device.level,
        "streak_days": device.streak_days,
        "last_active_date": _iso_date(device.last_active_date),
        "created_at": _iso_dt(device.created_at),
        "last_seen_at": _iso_dt(device.last_seen_at),
    }


def package_to_dict(package):
    return {
        "external_id": package.external_id,
        "title": package.title,
        "subject": package.subject,
        "description": package.description,
        "is_public": package.is_public,
        "target_device_uid": package.target_device_uid,
        "created_at": _iso_dt(package.created_at),
        "updated_at": _iso_dt(package.updated_at),
    }


def schedule_item_to_dict(item):
    return {
        "external_id": item.external_id,
        "package_external_id": item.package_external_id,
        "subject": item.subject,
        "lesson_type": item.lesson_type,
        "day_of_week": item.day_of_week,
        "start_time": item.start_time,
        "end_time": item.end_time,
        "room": item.room,
        "teacher": item.teacher,
        "parity": item.parity,
    }


def task_to_dict(task):
    return {
        "external_id": task.external_id,
        "package_external_id": task.package_external_id,
        "title": task.title,
        "subject": task.subject,
        "deadline_date": _iso_date(task.deadline_date),
        "effort_minutes": task.effort_minutes,
        "priority": task.priority,
    }


def material_to_dict(material):
    key_facts = []
    if material.key_facts_json:
        try:
            key_facts = json.loads(material.key_facts_json)
        except Exception:
            key_facts = []

    return {
        "external_id": material.external_id,
        "package_external_id": material.package_external_id,
        "title": material.title,
        "source_label": material.source_label,
        "summary_text": material.summary_text,
        "key_facts": key_facts,
    }


def card_to_dict(card):
    return {
        "external_id": card.external_id,
        "package_external_id": card.package_external_id,
        "material_external_id": card.material_external_id,
        "front": card.front,
        "back": card.back,
        "hint": card.hint,
        "position": card.position,
    }