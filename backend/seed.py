import json
from datetime import date, timedelta

from app import create_app, db
from app.models import (
    Device,
    PackageCard,
    PackageMaterial,
    PackageScheduleItem,
    PackageTask,
    StudyPackage,
)
from app.xp import calculate_level, current_week_key

app = create_app()


def ensure_package(
    external_id: str,
    title: str,
    subject: str,
    description: str,
    is_public: bool = True,
    target_device_uid: str | None = None,
):
    package = StudyPackage.query.filter_by(external_id=external_id).first()

    if package is None:
        package = StudyPackage(
            external_id=external_id,
            title=title,
            subject=subject,
            description=description,
            is_public=is_public,
            target_device_uid=target_device_uid,
        )
        db.session.add(package)

    return package


def ensure_schedule_item(
    external_id: str,
    package_external_id: str,
    subject: str,
    lesson_type: str,
    day_of_week: int,
    start_time: str,
    end_time: str,
    room: str | None = None,
    teacher: str | None = None,
    parity: str | None = None,
):
    item = PackageScheduleItem.query.filter_by(external_id=external_id).first()

    if item is None:
        item = PackageScheduleItem(
            external_id=external_id,
            package_external_id=package_external_id,
            subject=subject,
            lesson_type=lesson_type,
            day_of_week=day_of_week,
            start_time=start_time,
            end_time=end_time,
            room=room,
            teacher=teacher,
            parity=parity,
        )
        db.session.add(item)

    return item


def ensure_task(
    external_id: str,
    package_external_id: str,
    title: str,
    subject: str,
    deadline_date: date,
    effort_minutes: int,
    priority: int,
):
    task = PackageTask.query.filter_by(external_id=external_id).first()

    if task is None:
        task = PackageTask(
            external_id=external_id,
            package_external_id=package_external_id,
            title=title,
            subject=subject,
            deadline_date=deadline_date,
            effort_minutes=effort_minutes,
            priority=priority,
        )
        db.session.add(task)

    return task


def ensure_material(
    external_id: str,
    package_external_id: str,
    title: str,
    source_label: str,
    summary_text: str,
    key_facts: list[str],
):
    material = PackageMaterial.query.filter_by(external_id=external_id).first()

    if material is None:
        material = PackageMaterial(
            external_id=external_id,
            package_external_id=package_external_id,
            title=title,
            source_label=source_label,
            summary_text=summary_text,
            key_facts_json=json.dumps(key_facts, ensure_ascii=False),
        )
        db.session.add(material)

    return material


def ensure_card(
    external_id: str,
    package_external_id: str,
    material_external_id: str,
    front: str,
    back: str,
    hint: str | None,
    position: int,
):
    card = PackageCard.query.filter_by(external_id=external_id).first()

    if card is None:
        card = PackageCard(
            external_id=external_id,
            package_external_id=package_external_id,
            material_external_id=material_external_id,
            front=front,
            back=back,
            hint=hint,
            position=position,
        )
        db.session.add(card)

    return card


def ensure_device(
    device_uid: str,
    nickname: str,
    total_xp: int,
    weekly_xp: int,
    streak_days: int,
    rating_opt_in: bool,
    today: date,
    period_key: str,
):
    device = Device.query.filter_by(device_uid=device_uid).first()

    if device is None:
        device = Device(device_uid=device_uid)
        db.session.add(device)

    device.nickname = nickname
    device.rating_opt_in = rating_opt_in
    device.total_xp = total_xp
    device.weekly_xp = weekly_xp
    device.weekly_period_key = period_key
    device.level = calculate_level(total_xp)
    device.streak_days = streak_days
    device.last_active_date = today if streak_days > 0 else None

    return device


with app.app_context():
    today = date.today()
    period_key = current_week_key(today)

    physics = ensure_package(
        external_id="physics-lecture-5",
        title="Физика: Лекция 5 и лабораторная",
        subject="Физика",
        description="Лекция по термодинамике и подготовка к лабораторной работе.",
    )

    history = ensure_package(
        external_id="history-tickets-1-10",
        title="История: Билеты 1–10",
        subject="История",
        description="Краткие ответы по первым десяти билетам к зачёту.",
    )

    ensure_schedule_item(
        external_id="physics-lecture-5-monday",
        package_external_id=physics.external_id,
        subject="Физика",
        lesson_type="лекция",
        day_of_week=1,
        start_time="10:00",
        end_time="11:30",
        room="204",
        teacher="Иванов А. П.",
    )

    ensure_schedule_item(
        external_id="physics-lab-wednesday",
        package_external_id=physics.external_id,
        subject="Физика",
        lesson_type="лабораторная",
        day_of_week=3,
        start_time="14:00",
        end_time="15:30",
        room="105",
        teacher="Иванов А. П.",
    )

    ensure_schedule_item(
        external_id="history-seminar-tuesday",
        package_external_id=history.external_id,
        subject="История",
        lesson_type="семинар",
        day_of_week=2,
        start_time="09:00",
        end_time="10:30",
        room="312",
        teacher="Петрова С. В.",
    )

    ensure_schedule_item(
        external_id="history-lecture-thursday",
        package_external_id=history.external_id,
        subject="История",
        lesson_type="лекция",
        day_of_week=4,
        start_time="11:00",
        end_time="12:30",
        room="101",
        teacher="Петрова С. В.",
    )

    ensure_task(
        external_id="physics-task-lab-5",
        package_external_id=physics.external_id,
        title="Лабораторная работа №5",
        subject="Физика",
        deadline_date=today + timedelta(days=2),
        effort_minutes=90,
        priority=3,
    )

    ensure_task(
        external_id="physics-task-theory",
        package_external_id=physics.external_id,
        title="Разобрать теорию по лекции 5",
        subject="Физика",
        deadline_date=today + timedelta(days=5),
        effort_minutes=60,
        priority=2,
    )

    ensure_task(
        external_id="history-task-tickets-1-3",
        package_external_id=history.external_id,
        title="Выучить билеты 1–3",
        subject="История",
        deadline_date=today + timedelta(days=1),
        effort_minutes=45,
        priority=3,
    )

    ensure_task(
        external_id="history-task-tickets-4-10",
        package_external_id=history.external_id,
        title="Подготовить билеты 4–10",
        subject="История",
        deadline_date=today + timedelta(days=7),
        effort_minutes=120,
        priority=2,
    )

    physics_material = ensure_material(
        external_id="physics-material-5",
        package_external_id=physics.external_id,
        title="Лекция 5: основы термодинамики",
        source_label="Конспект лекции",
        summary_text=(
            "На лекции разобрали первые начала термодинамики, внутреннюю энергию, "
            "работу газа и количество теплоты. Основной акцент сделан на законе "
            "сохранения энергии и изолированных системах."
        ),
        key_facts=[
            "Внутренняя энергия зависит от температуры и агрегатного состояния.",
            "Первое начало термодинамики: ΔU = Q - A.",
            "В изолированной системе полная энергия сохраняется.",
            "Работа газа положительна при расширении.",
        ],
    )

    history_material = ensure_material(
        external_id="history-material-1-10",
        package_external_id=history.external_id,
        title="Билеты 1–10: краткий конспект",
        source_label="Учебные материалы",
        summary_text=(
            "Краткие ответы по первым десяти билетам: ключевые даты, личности, "
            "причины событий и последствия для развития государства."
        ),
        key_facts=[
            "Билеты 1–3 охватывают ранний период.",
            "Билеты 4–6 связаны с реформами.",
            "Билеты 7–10 посвящены внешнеполитическим событиям.",
            "Для каждого билета важно знать дату, причину и следствие.",
        ],
    )

    ensure_card(
        external_id="physics-card-1",
        package_external_id=physics.external_id,
        material_external_id=physics_material.external_id,
        front="Как формулируется первое начало термодинамики?",
        back="ΔU = Q - A: изменение внутренней энергии равно количеству теплоты минус работа газа.",
        hint="Связь энергии, теплоты и работы.",
        position=1,
    )

    ensure_card(
        external_id="physics-card-2",
        package_external_id=physics.external_id,
        material_external_id=physics_material.external_id,
        front="Что такое внутренняя энергия?",
        back="Энергия хаотического движения и взаимодействия частиц тела.",
        hint="Зависит от температуры и состояния.",
        position=2,
    )

    ensure_card(
        external_id="physics-card-3",
        package_external_id=physics.external_id,
        material_external_id=physics_material.external_id,
        front="Когда работа газа положительна?",
        back="При расширении газа.",
        hint="Смотрите направление изменения объёма.",
        position=3,
    )

    ensure_card(
        external_id="history-card-1",
        package_external_id=history.external_id,
        material_external_id=history_material.external_id,
        front="Что важно знать при ответе по билету 1?",
        back="Дату, причину события, ключевых участников и последствие.",
        hint="Структура ответа: причина → событие → итог.",
        position=1,
    )

    ensure_card(
        external_id="history-card-2",
        package_external_id=history.external_id,
        material_external_id=history_material.external_id,
        front="Какие темы входят в билеты 4–6?",
        back="Период реформ и изменение государственного устройства.",
        hint="Обратите внимание на причины реформ.",
        position=2,
    )

    ensure_card(
        external_id="history-card-3",
        package_external_id=history.external_id,
        material_external_id=history_material.external_id,
        front="Что проверяют в билетах 7–10?",
        back="Внешнеполитические события и их влияние на страну.",
        hint="Важно называть последствия.",
        position=3,
    )

    demo_devices = [
        ("demo-alina", "Алина", 820, 180, 5, True),
        ("demo-maxim", "Максим", 640, 150, 4, True),
        ("demo-polina", "Полина", 510, 140, 4, True),
        ("demo-ivan", "Иван", 390, 90, 3, True),
        ("demo-secret", "Студент без рейтинга", 1200, 300, 6, False),
    ]

    for device_uid, nickname, total_xp, weekly_xp, streak_days, rating_opt_in in demo_devices:
        ensure_device(
            device_uid=device_uid,
            nickname=nickname,
            total_xp=total_xp,
            weekly_xp=weekly_xp,
            streak_days=streak_days,
            rating_opt_in=rating_opt_in,
            today=today,
            period_key=period_key,
        )

    db.session.commit()

    print("Seed complete.")
    print(f"Week period key: {period_key}")