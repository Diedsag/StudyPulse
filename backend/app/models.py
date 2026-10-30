from datetime import datetime, timezone

from . import db


def utcnow():
    return datetime.now(timezone.utc)


class Device(db.Model):
    __tablename__ = "devices"

    id = db.Column(db.Integer, primary_key=True)
    device_uid = db.Column(db.String(120), unique=True, nullable=False, index=True)
    nickname = db.Column(db.String(120), nullable=True)
    rating_opt_in = db.Column(db.Boolean, default=True, nullable=False)

    total_xp = db.Column(db.Integer, default=0, nullable=False)
    weekly_xp = db.Column(db.Integer, default=0, nullable=False)
    level = db.Column(db.Integer, default=1, nullable=False)
    streak_days = db.Column(db.Integer, default=0, nullable=False)
    last_active_date = db.Column(db.Date, nullable=True)

    created_at = db.Column(db.DateTime, default=utcnow, nullable=False)
    last_seen_at = db.Column(db.DateTime, default=utcnow, nullable=False)


class StudyPackage(db.Model):
    __tablename__ = "study_packages"

    id = db.Column(db.Integer, primary_key=True)
    external_id = db.Column(db.String(120), unique=True, nullable=False, index=True)
    title = db.Column(db.String(200), nullable=False)
    subject = db.Column(db.String(120), nullable=True)
    description = db.Column(db.Text, nullable=True)

    is_public = db.Column(db.Boolean, default=True, nullable=False)
    target_device_uid = db.Column(db.String(120), nullable=True, index=True)

    created_at = db.Column(db.DateTime, default=utcnow, nullable=False)
    updated_at = db.Column(db.DateTime, default=utcnow, onupdate=utcnow, nullable=False)


class PackageScheduleItem(db.Model):
    __tablename__ = "package_schedule_items"

    id = db.Column(db.Integer, primary_key=True)
    package_external_id = db.Column(
        db.String(120),
        db.ForeignKey("study_packages.external_id"),
        nullable=False,
        index=True,
    )

    external_id = db.Column(db.String(120), unique=True, nullable=False, index=True)
    subject = db.Column(db.String(120), nullable=False)
    lesson_type = db.Column(db.String(40), nullable=True)
    day_of_week = db.Column(db.Integer, nullable=False)
    start_time = db.Column(db.String(5), nullable=False)
    end_time = db.Column(db.String(5), nullable=False)
    room = db.Column(db.String(80), nullable=True)
    teacher = db.Column(db.String(120), nullable=True)
    parity = db.Column(db.String(20), nullable=True)


class PackageTask(db.Model):
    __tablename__ = "package_tasks"

    id = db.Column(db.Integer, primary_key=True)
    package_external_id = db.Column(
        db.String(120),
        db.ForeignKey("study_packages.external_id"),
        nullable=False,
        index=True,
    )

    external_id = db.Column(db.String(120), unique=True, nullable=False, index=True)
    title = db.Column(db.String(200), nullable=False)
    subject = db.Column(db.String(120), nullable=True)
    deadline_date = db.Column(db.Date, nullable=False)
    effort_minutes = db.Column(db.Integer, nullable=False, default=60)
    priority = db.Column(db.Integer, nullable=False, default=2)


class PackageMaterial(db.Model):
    __tablename__ = "package_materials"

    id = db.Column(db.Integer, primary_key=True)
    package_external_id = db.Column(
        db.String(120),
        db.ForeignKey("study_packages.external_id"),
        nullable=False,
        index=True,
    )

    external_id = db.Column(db.String(120), unique=True, nullable=False, index=True)
    title = db.Column(db.String(200), nullable=False)
    source_label = db.Column(db.String(200), nullable=True)
    summary_text = db.Column(db.Text, nullable=False)
    key_facts_json = db.Column(db.Text, nullable=True)


class PackageCard(db.Model):
    __tablename__ = "package_cards"

    id = db.Column(db.Integer, primary_key=True)
    package_external_id = db.Column(
        db.String(120),
        db.ForeignKey("study_packages.external_id"),
        nullable=False,
        index=True,
    )
    material_external_id = db.Column(
        db.String(120),
        db.ForeignKey("package_materials.external_id"),
        nullable=True,
        index=True,
    )

    external_id = db.Column(db.String(120), unique=True, nullable=False, index=True)
    front = db.Column(db.Text, nullable=False)
    back = db.Column(db.Text, nullable=False)
    hint = db.Column(db.Text, nullable=True)
    position = db.Column(db.Integer, default=0, nullable=False)