# StudyPulse Data Model

## Принцип

Room на Android — источник истины для учебного процесса.  
Flask + SQLAlchemy — источник истины для каталога пакетов, принятых событий, XP, уровня, streak и рейтинга.  
DataStore — источник истины для настроек устройства.

## Android: DataStore

Храним:

- server_url
- device_uid
- nickname
- rating_opt_in
- last_sync_at
- theme_mode, если будет
- daily_goal, если будет

Пример ключей:

object SettingsKeys {
    val SERVER_URL = stringPreferencesKey("server_url")
    val DEVICE_UID = stringPreferencesKey("device_uid")
    val NICKNAME = stringPreferencesKey("nickname")
    val RATING_OPT_IN = booleanPreferencesKey("rating_opt_in")
    val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
}

## Android: Room сущности

### StudyPackageEntity

Поля:

- externalId: String, PK
- title: String
- subject: String?
- description: String?
- isPublic: Boolean
- targetDeviceUid: String?
- updatedAt: Long
- downloadedAt: Long?
- isDownloaded: Boolean

### ScheduleItemEntity

Шаблон занятия внутри пакета.

Поля:

- externalId: String, PK
- packageExternalId: String, FK to StudyPackageEntity.externalId
- subject: String
- lessonType: String?
- dayOfWeek: Int, 1..7
- startTime: String, например "10:00"
- endTime: String, например "11:30"
- room: String?
- teacher: String?
- parity: String?例如 "every", "numerator", "denominator"

Индексы:

- packageExternalId
- dayOfWeek

### ClassInstanceEntity

Конкретное занятие на дату. Нужно для отмены.

Поля:

- id: Long, auto PK
- templateExternalId: String
- date: String, ISO yyyy-MM-dd
- status: String
  - PLANNED
  - CANCELLED
  - MOVED
- overriddenStartTime: String?
- overriddenEndTime: String?

Для MVP достаточно PLANNED и CANCELLED.

### TaskEntity

Поля:

- externalId: String, PK
- packageExternalId: String, FK
- title: String
- subject: String?
- deadlineDate: String, ISO yyyy-MM-dd
- effortMinutes: Int
- priority: Int
  - 1 low
  - 2 medium
  - 3 high
- status: String
  - PENDING
  - IN_PROGRESS
  - DONE
- remainingMinutes: Int

### StudySlotEntity

Поля:

- id: Long, auto PK
- taskExternalId: String, FK
- startDateTime: Long
- endDateTime: Long
- status: String
  - PROPOSED
  - PLANNED
  - DONE
  - MISSED
- materialExternalId: String?

### MaterialEntity

Поля:

- externalId: String, PK
- packageExternalId: String, FK
- title: String
- sourceLabel: String?
- summaryText: String
- keyFactsJson: String?

keyFactsJson можно хранить как JSON-массив строк.

### CardEntity

Поля:

- externalId: String, PK
- packageExternalId: String, FK
- materialExternalId: String?
- front: String
- back: String
- hint: String?
- position: Int

Поля интервального повторения:

- easeFactor: Double, default 2.5
- intervalDays: Int, default 0
- repetitions: Int, default 0
- lapses: Int, default 0
- dueAt: Long
- createdAt: Long
- updatedAt: Long

### ReviewLogEntity

Поля:

- id: Long, auto PK
- clientEventId: String
- cardExternalId: String
- packageExternalId: String
- rating: Int
  - 1 AGAIN
  - 2 HARD
  - 3 GOOD
  - 4 EASY
- reviewedAt: Long
- previousIntervalDays: Int
- newIntervalDays: Int
- previousEaseFactor: Double
- newEaseFactor: Double
- synced: Boolean, default false

### PendingEventEntity

Универсальная очередь синхронизации.

Поля:

- clientEventId: String, PK
- eventType: String
- payloadJson: String
- occurredAt: Long
- createdAt: Long
- retryCount: Int
- lastError: String?
- synced: Boolean

### UserStatsCacheEntity

Поля:

- deviceUid: String, PK
- nickname: String?
- totalXp: Int
- weeklyXp: Int
- level: Int
- streakDays: Int
- totalReviews: Int
- reviewsToday: Int
- completedSlots: Int
- pendingEventsCount: Int
- lastSyncAt: Long
- fetchedAt: Long

### LeaderboardEntryEntity

Поля:

- deviceUid: String, PK
- nickname: String
- weeklyXp: Int
- totalXp: Int
- level: Int
- streakDays: Int
- rank: Int
- fetchedAt: Long

## Backend: SQLAlchemy модели

### Device

Поля:

- id
- device_uid, unique, indexed
- nickname
- rating_opt_in
- total_xp
- weekly_xp
- level
- streak_days
- last_active_date
- created_at
- last_seen_at

### StudyPackage

Поля:

- id
- external_id, unique, indexed
- title
- subject
- description
- is_public
- target_device_uid
- created_at
- updated_at

### PackageScheduleItem

Поля:

- id
- package_external_id, FK/index
- external_id, unique/index
- subject
- lesson_type
- day_of_week
- start_time
- end_time
- room
- teacher
- parity

### PackageTask

Поля:

- id
- package_external_id, FK/index
- external_id, unique/index
- title
- subject
- deadline_date
- effort_minutes
- priority

### PackageMaterial

Поля:

- id
- package_external_id, FK/index
- external_id, unique/index
- title
- source_label
- summary_text
- key_facts_json

### PackageCard

Поля:

- id
- package_external_id, FK/index
- material_external_id, index
- external_id, unique/index
- front
- back
- hint
- position

### SyncEvent

Поля:

- id
- client_event_id, unique/index
- device_uid, index
- event_type
- occurred_at
- payload_json
- xp_awarded
- created_at

## Типы событий

Обязательные:

- CARD_REVIEWED
- SLOT_COMPLETED
- PLAN_ACCEPTED
- CLASS_CANCELLED
- PACKAGE_DOWNLOADED

Опциональные:

- TASK_COMPLETED
- MATERIAL_OPENED
- QUESTION_ASKED

Для MVP достаточно первых пяти.

## Payload событий

### CARD_REVIEWED

{
  "package_external_id": "physics-lecture-5",
  "card_external_id": "physics-card-1",
  "rating": 3,
  "previous_interval_days": 0,
  "new_interval_days": 1
}

### SLOT_COMPLETED

{
  "task_external_id": "physics-lab-3",
  "slot_id": 12,
  "duration_minutes": 45
}

### PLAN_ACCEPTED

{
  "package_external_id": "physics-lecture-5",
  "slots_count": 4,
  "total_minutes": 180
}

### CLASS_CANCELLED

{
  "template_external_id": "physics-lecture-5-slot-1",
  "date": "2026-11-04"
}

### PACKAGE_DOWNLOADED

{
  "package_external_id": "physics-lecture-5"
}

## XP на сервере

| Событие | XP |
|---|---:|
| CARD_REVIEWED, rating=AGAIN | 1 |
| CARD_REVIEWED, rating=HARD | 2 |
| CARD_REVIEWED, rating=GOOD | 3 |
| CARD_REVIEWED, rating=EASY | 4 |
| SLOT_COMPLETED | 5 + бонус за длительность |
| PLAN_ACCEPTED | 2 |
| CLASS_CANCELLED | 0 |
| PACKAGE_DOWNLOADED | 1 |

Бонус за слот:

xp = 5 + min(duration_minutes / 15, 10)

Пример:

- 45 минут → 5 + 3 = 8 XP;
- 90 минут → 5 + 6 = 11 XP;
- максимум бонуса 10.

## Уровни

| Уровень | Total XP |
|---:|---:|
| 1 | 0 |
| 2 | 100 |
| 3 | 250 |
| 4 | 500 |
| 5 | 900 |
| 6 | 1500 |
| 7 | 2500 |

## Streak

Streak считается на сервере по датам, когда было принято хотя бы одно событие, кроме PACKAGE_DOWNLOADED.

Правила:

- если сегодня уже есть активность, streak не увеличивается повторно;
- если последняя активность была вчера, streak +1;
- если пропущен день, streak = 1;
- дата берётся по серверному времени.

## Leaderboard

Недельный рейтинг:

- сортировка по weekly_xp;
- top 10;
- показываются nickname, weekly_xp, total_xp, level, streak_days;
- своё место вычисляется отдельно, даже если пользователь не в top 10.

## Weekly XP

Для MVP можно хранить поле weekly_xp в Device.

При синхронизации:

- если текущая ISO-неделя отличается от сохранённой, обнулить weekly_xp;
- затем начислить XP за принятые события.

Альтернатива — считать weekly_xp агрегатом из SyncEvent за последние 7 дней. Для демо проще хранить поле.