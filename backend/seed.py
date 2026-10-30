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
    utcnow,
)

app = create_app()


def seed():
    with app.app_context():
        db.drop_all()
        db.create_all()

        today = date.today()

        devices = [
            Device(
                device_uid="android-uuid-123",
                nickname="Student Demo",
                rating_opt_in=True,
                total_xp=0,
                weekly_xp=0,
                level=1,
                streak_days=0,
                last_active_date=None,
            ),
            Device(
                device_uid="android-uuid-001",
                nickname="Fox-001",
                rating_opt_in=True,
                total_xp=720,
                weekly_xp=180,
                level=5,
                streak_days=6,
                last_active_date=today,
            ),
            Device(
                device_uid="android-uuid-002",
                nickname="Learner-773",
                rating_opt_in=True,
                total_xp=450,
                weekly_xp=90,
                level=3,
                streak_days=3,
                last_active_date=today - timedelta(days=1),
            ),
            Device(
                device_uid="android-uuid-003",
                nickname="Brainy-Fox-12",
                rating_opt_in=True,
                total_xp=260,
                weekly_xp=60,
                level=2,
                streak_days=2,
                last_active_date=today - timedelta(days=2),
            ),
        ]

        packages = [
            StudyPackage(
                external_id="physics-lecture-5",
                title="Физика: Лекция 5 и лабораторная",
                subject="Физика",
                description="Теорема о среднем, подготовка к лабораторной работе",
                is_public=True,
                target_device_uid=None,
                created_at=utcnow(),
                updated_at=utcnow(),
            ),
            StudyPackage(
                external_id="history-tickets-1-10",
                title="История: Билеты 1–10",
                subject="История",
                description="Основные даты, понятия и персоналии",
                is_public=True,
                target_device_uid=None,
                created_at=utcnow(),
                updated_at=utcnow(),
            ),
        ]

        schedule = [
            PackageScheduleItem(
                external_id="physics-lecture-5-slot-1",
                package_external_id="physics-lecture-5",
                subject="Физика",
                lesson_type="lecture",
                day_of_week=3,
                start_time="10:00",
                end_time="11:30",
                room="405",
                teacher="Иванов И. И.",
                parity="every",
            ),
            PackageScheduleItem(
                external_id="physics-lab-5-slot-1",
                package_external_id="physics-lecture-5",
                subject="Физика",
                lesson_type="lab",
                day_of_week=5,
                start_time="14:00",
                end_time="15:30",
                room="212",
                teacher="Петрова А. С.",
                parity="every",
            ),
            PackageScheduleItem(
                external_id="history-seminar-1",
                package_external_id="history-tickets-1-10",
                subject="История",
                lesson_type="seminar",
                day_of_week=2,
                start_time="09:00",
                end_time="10:30",
                room="108",
                teacher="Сидоров П. П.",
                parity="every",
            ),
        ]

        tasks = [
            PackageTask(
                external_id="physics-lab-3",
                package_external_id="physics-lecture-5",
                title="Лабораторная №3",
                subject="Физика",
                deadline_date=today + timedelta(days=2),
                effort_minutes=120,
                priority=3,
            ),
            PackageTask(
                external_id="physics-theorem-review",
                package_external_id="physics-lecture-5",
                title="Повторить теорему о среднем",
                subject="Физика",
                deadline_date=today + timedelta(days=1),
                effort_minutes=45,
                priority=2,
            ),
            PackageTask(
                external_id="history-tickets-prep",
                package_external_id="history-tickets-1-10",
                title="Подготовить билеты 1–10",
                subject="История",
                deadline_date=today + timedelta(days=5),
                effort_minutes=180,
                priority=2,
            ),
        ]

        materials = [
            PackageMaterial(
                external_id="physics-lecture-5-summary",
                package_external_id="physics-lecture-5",
                title="Лекция 5: теорема о среднем значении",
                source_label="Конспект лекции 5, стр. 2",
                summary_text=(
                    "Теорема о среднем значении связывает определённый интеграл функции "
                    "и значение самой функции в некоторой точке промежутка. "
                    "Если функция непрерывна на отрезке, то существует точка, "
                    "в которой значение функции равно среднему значению на этом отрезке."
                ),
                key_facts_json=json.dumps(
                    [
                        "Функция должна быть непрерывна на отрезке [a, b]",
                        "Существует точка c из [a, b], где f(c) равно среднему значению",
                        "Интеграл равен f(c) * (b - a)",
                        "Теорема полезна для оценки интегралов",
                    ],
                    ensure_ascii=False,
                ),
            ),
            PackageMaterial(
                external_id="history-tickets-summary",
                package_external_id="history-tickets-1-10",
                title="История: ключевые даты билетов 1–10",
                source_label="Учебная подборка, стр. 1",
                summary_text=(
                    "Краткая подборка ключевых дат и понятий для подготовки "
                    "к экзаменационным билетам по истории."
                ),
                key_facts_json=json.dumps(
                    [
                        "1918 — первая Конституция РСФСР",
                        "1922 — образование СССР",
                        "1936 — Сталинская Конституция",
                        "1945 — окончание Великой Отечественной войны",
                    ],
                    ensure_ascii=False,
                ),
            ),
        ]

        cards = [
            PackageCard(
                external_id="physics-card-1",
                package_external_id="physics-lecture-5",
                material_external_id="physics-lecture-5-summary",
                front="Сформулируй теорему о среднем значении",
                back=(
                    "Если функция непрерывна на отрезке [a, b], "
                    "то существует точка c из [a, b], такая что "
                    "интеграл функции по этому отрезку равен f(c) * (b - a)."
                ),
                hint="Интеграл и значение функции",
                position=1,
            ),
            PackageCard(
                external_id="physics-card-2",
                package_external_id="physics-lecture-5",
                material_external_id="physics-lecture-5-summary",
                front="Какое условие важно для теоремы о среднем?",
                back="Функция должна быть непрерывна на всём отрезке [a, b].",
                hint="Непрерывность",
                position=2,
            ),
            PackageCard(
                external_id="physics-card-3",
                package_external_id="physics-lecture-5",
                material_external_id="physics-lecture-5-summary",
                front="Запиши формулу теоремы о среднем значении",
                back="∫[a到b] f(x) dx = f(c) * (b - a), где c ∈ [a, b].",
                hint="Интеграл равен значению в точке, умноженному на длину отрезка",
                position=3,
            ),
            PackageCard(
                external_id="history-card-1",
                package_external_id="history-tickets-1-10",
                material_external_id="history-tickets-summary",
                front="Когда была принята первая Конституция РСФСР?",
                back="1918 год.",
                hint="После революции",
                position=1,
            ),
            PackageCard(
                external_id="history-card-2",
                package_external_id="history-tickets-1-10",
                material_external_id="history-tickets-summary",
                front="Когда был образован СССР?",
                back="1922 год.",
                hint="После Гражданской войны",
                position=2,
            ),
            PackageCard(
                external_id="history-card-3",
                package_external_id="history-tickets-1-10",
                material_external_id="history-tickets-summary",
                front="Как называется Конституция 1936 года?",
                back="Сталинская Конституция.",
                hint="Советская конституция 1930-х",
                position=3,
            ),
        ]

        db.session.add_all(devices + packages + schedule + tasks + materials + cards)
        db.session.commit()

        print(
            "Seed complete: "
            f"devices={len(devices)}, "
            f"packages={len(packages)}, "
            f"schedule={len(schedule)}, "
            f"tasks={len(tasks)}, "
            f"materials={len(materials)}, "
            f"cards={len(cards)}"
        )


if __name__ == "__main__":
    seed()