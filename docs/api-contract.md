# StudyPulse API Contract

Base URL для локальной разработки:

http://10.0.2.2:5000

Для физического устройства:

http://<IP_компьютера>:5000

## 1. Health check

GET /health

Response:

{
  "status": "ok",
  "service": "studypulse-api",
  "time": "2026-10-31T10:00:00Z"
}

## 2. Регистрация устройства

POST /api/devices/register
Content-Type: application/json

Request:

{
  "device_uid": "android-uuid-123",
  "nickname": "Student Demo",
  "rating_opt_in": true
}

Response:

{
  "device_uid": "android-uuid-123",
  "nickname": "Student Demo",
  "created": true,
  "total_xp": 0,
  "weekly_xp": 0,
  "level": 1,
  "streak_days": 0,
  "server_time": "2026-10-31T10:00:00Z"
}

Правила:

- если device_uid уже существует, не создавать дубль;
- обновлять last_seen_at;
- можно обновлять nickname, если он пришёл;
- response всегда содержит актуальные XP/level/streak.

## 3. Список учебных пакетов

GET /api/packages?device_uid=android-uuid-123

Response:

{
  "packages": [
    {
      "external_id": "physics-lecture-5",
      "title": "Физика: Лекция 5 и лабораторная",
      "subject": "Физика",
      "description": "Теорема о среднем, подготовка к лабе",
      "is_public": true,
      "target_device_uid": null,
      "updated_at": "2026-10-31T09:00:00Z"
    },
    {
      "external_id": "history-tickets-1-10",
      "title": "История: Билеты 1–10",
      "subject": "История",
      "description": "Основные даты и понятия",
      "is_public": true,
      "target_device_uid": null,
      "updated_at": "2026-10-31T09:10:00Z"
    }
  ],
  "server_time": "2026-10-31T10:00:00Z"
}

Правила:

- возвращаются public пакеты;
- также возвращаются пакеты, где target_device_uid равен device_uid;
- если device_uid неизвестен, лучше вернуть 404, клиент сначала регистрируется.

## 4. Бандл учебного пакета

GET /api/packages/{external_id}/bundle?device_uid=android-uuid-123

Response:

{
  "package": {
    "external_id": "physics-lecture-5",
    "title": "Физика: Лекция 5 и лабораторная",
    "subject": "Физика",
    "description": "Теорема о среднем, подготовка к лабе",
    "is_public": true,
    "target_device_uid": null,
    "updated_at": "2026-10-31T09:00:00Z"
  },
  "schedule": [
    {
      "external_id": "physics-lecture-5-slot-1",
      "subject": "Физика",
      "lesson_type": "lecture",
      "day_of_week": 3,
      "start_time": "10:00",
      "end_time": "11:30",
      "room": "405",
      "teacher": "Иванов И. И.",
      "parity": "every"
    }
  ],
  "tasks": [
    {
      "external_id": "physics-lab-3",
      "title": "Лабораторная №3",
      "subject": "Физика",
      "deadline_date": "2026-11-02",
      "effort_minutes": 120,
      "priority": 3
    }
  ],
  "materials": [
    {
      "external_id": "physics-lecture-5-summary",
      "title": "Лекция 5: теорема о среднем",
      "source_label": "Конспект лекции 5, стр. 2",
      "summary_text": "Теорема о среднем значении связывает интеграл функции и её значение в некоторой точке промежутка.",
      "key_facts": [
        "Функция должна быть непрерывна на отрезке",
        "Существует точка c, где значение функции равно среднему",
        "Теорема полезна для оценки интегралов"
      ]
    }
  ],
  "cards": [
    {
      "external_id": "physics-card-1",
      "material_external_id": "physics-lecture-5-summary",
      "front": "Сформулируй теорему о среднем значении",
      "back": "Если функция непрерывна на [a,b], то существует точка c из [a,b], такая что интеграл равен f(c)*(b-a).",
      "hint": "Интеграл и значение функции",
      "position": 1
    }
  ],
  "server_time": "2026-10-31T10:00:00Z"
}

Правила:

- если пакет не найден — 404;
- если пакет не доступен устройству — 403;
- клиент сохраняет всё в Room в одной транзакции.

## 5. Синхронизация событий

POST /api/sync/batch
Content-Type: application/json

Request:

{
  "device_uid": "android-uuid-123",
  "events": [
    {
      "client_event_id": "evt-001",
      "event_type": "CARD_REVIEWED",
      "occurred_at": "2026-10-31T10:05:00Z",
      "payload": {
        "package_external_id": "physics-lecture-5",
        "card_external_id": "physics-card-1",
        "rating": 3,
        "previous_interval_days": 0,
        "new_interval_days": 1
      }
    },
    {
      "client_event_id": "evt-002",
      "event_type": "SLOT_COMPLETED",
      "occurred_at": "2026-10-31T10:30:00Z",
      "payload": {
        "task_external_id": "physics-lab-3",
        "slot_id": 12,
        "duration_minutes": 45
      }
    }
  ]
}

Response:

{
  "accepted": 2,
  "duplicated": 0,
  "rejected": 0,
  "xp_awarded_total": 11,
  "total_xp": 11,
  "weekly_xp": 11,
  "level": 1,
  "streak_days": 1,
  "server_time": "2026-10-31T10:31:00Z"
}

Правила:

- max events per batch: 100;
- client_event_id уникален;
- если client_event_id уже есть, событие считается duplicated и XP не начисляется повторно;
- rating должен быть 1..4 для CARD_REVIEWED;
- occurred_at не должен быть сильно в будущем, например больше server_time + 5 minutes;
- неизвестный device_uid — 404;
- невалидный event_type — rejected;
- сервер возвращает актуальные total_xp, weekly_xp, level, streak_days.

## 6. Статистика пользователя

GET /api/me/stats?device_uid=android-uuid-123

Response:

{
  "device_uid": "android-uuid-123",
  "nickname": "Student Demo",
  "total_xp": 120,
  "weekly_xp": 45,
  "level": 2,
  "streak_days": 3,
  "total_reviews": 24,
  "reviews_today": 6,
  "completed_slots": 5,
  "last_sync_at": "2026-10-31T10:31:00Z"
}

## 7. Рейтинг

GET /api/leaderboard?device_uid=android-uuid-123&period=week&limit=10

Response:

{
  "period": "week",
  "entries": [
    {
      "rank": 1,
      "device_uid": "android-uuid-001",
      "nickname": "Fox-001",
      "weekly_xp": 180,
      "total_xp": 720,
      "level": 5,
      "streak_days": 6
    },
    {
      "rank": 2,
      "device_uid": "android-uuid-123",
      "nickname": "Student Demo",
      "weekly_xp": 120,
      "total_xp": 450,
      "level": 3,
      "streak_days": 3
    }
  ],
  "me": {
    "rank": 2,
    "weekly_xp": 120,
    "total_xp": 450,
    "level": 3,
    "streak_days": 3
  },
  "server_time": "2026-10-31T10:35:00Z"
}

Правила:

- period=week для MVP;
- limit по умолчанию 10;
- устройства с rating_opt_in=false не участвуют в leaderboard;
- своё место возвращается даже если пользователь вне top limit.

## Ошибки

Единый формат не обязателен, но желательно:

{
  "error": "invalid_request",
  "message": "device_uid is required"
}

HTTP статусы:

- 400 — невалидный запрос;
- 403 — нет доступа к пакету;
- 404 — устройство или пакет не найдены;
- 413 — слишком большой batch;
- 500 — ошибка сервера.