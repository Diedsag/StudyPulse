# StudyPulse Phase 2 Data Model

## Назначение документа

Этот документ описывает целевую модель данных StudyPulse для Фазы 2.

Текущая модель после Блока 10 ориентирована на серверные учебные пакеты. Фаза 2 должна поддержать локальные данные, которые студент создаёт сам.

## Главный принцип

    Локальные данные — primary source of truth
    Сервер — демо-контент, прогресс, XP, рейтинг, будущая обработка материалов

## Текущие сущности после Блока 10

Основные локальные сущности:

    StudyPackageEntity
    ScheduleItemEntity
    TaskEntity
    MaterialEntity
    CardEntity
    ClassInstanceEntity
    StudySlotEntity
    ReviewLogEntity
    PendingEventEntity
    StatsSnapshotEntity

Эти сущности хорошо работают для скачанных пакетов, но недостаточно для личного планировщика.

## Недостающие сущности Фазы 2

Нужно добавить или существенно расширить:

    SubjectEntity
    ScheduleTemplateEntity
    ClassInstanceEntity
    TaskEntity
    MaterialEntity
    MaterialChunkEntity
    StudySlotEntity
    PendingEventEntity

## SubjectEntity

Предмет как самостоятельная сущность.

Поля:

    id
    externalId
    name
    colorArgb
    teacher
    source
    packageExternalId
    createdAt
    updatedAt

Значения source:

    LOCAL
    PACKAGE

Назначение:

- предметы можно создавать вручную;
- предметы из пакетов не конфликтуют с ручными;
- цвет предмета используется в расписании, заданиях, материалах и виджете.

## ScheduleTemplateEntity

Недельный шаблон занятия.

Поля:

    id
    subjectId
    lessonType
    dayOfWeek
    startTime
    endTime
    room
    teacher
    parity
    source
    packageExternalId
    createdAt
    updatedAt

Значения lessonType:

    ЛЕКЦИЯ
    СЕМИНАР
    ЛАБОРАТОРНАЯ
    ЭКЗАМЕН
    ЗАЧЁТ
    ДРУГОЕ

Значения parity:

    EVERY
    NUMERATOR
    DENOMINATOR

Назначение:

- хранить повторяющиеся занятия;
- отдельно отменять конкретную дату через ClassInstance;
- не ломать всю серию при одной отмене.

## ClassInstanceEntity

Конкретное занятие на дату.

Поля:

    templateId
    date
    status
    overriddenStartTime
    overriddenEndTime
    overriddenRoom
    cancellationScope
    updatedAt

Значения status:

    PLANNED
    CANCELLED
    MOVED

Значения cancellationScope:

    SINGLE
    ALL_FUTURE

Назначение:

- отменить одно занятие;
- отменить все будущие занятия серии;
- перенести занятие на другую дату или время;
- дать планировщику корректные занятые интервалы.

## TaskEntity

Задание.

Текущие поля нужно расширить:

    id
    externalId
    subjectId
    packageExternalId
    title
    description
    deadlineDate
    effortMinutes
    remainingMinutes
    priority
    status
    source
    createdAt
    updatedAt

Значения status:

    PENDING
    IN_PROGRESS
    DONE
    OVERDUE

Значения source:

    LOCAL
    PACKAGE
    MATERIAL

Назначение:

- задания можно создавать вручную;
- задания могут приходить из пакетов;
- позже задания могут создаваться из материалов;
- планировщик работает с remainingMinutes, а не только effortMinutes.

## MaterialEntity

Учебный материал.

Поля:

    id
    externalId
    subjectId
    packageExternalId
    title
    sourceType
    sourceLabel
    sourceUri
    rawText
    summaryText
    processingStatus
    createdAt
    updatedAt

Значения sourceType:

    TEXT
    PDF
    PHOTO
    AUDIO
    PACKAGE

Значения processingStatus:

    READY
    PROCESSING
    FAILED

Назначение:

- материалы можно добавлять вручную;
- материалы могут приходить из пакетов;
- позже фото/аудио/PDF будут обрабатываться в саммари и чанки;
- материал привязывается к предмету, а не только к пакету.

## MaterialChunkEntity

Фрагмент материала для Q&A и будущих цитат.

Поля:

    id
    materialId
    text
    orderIndex
    pageNumber
    timecodeStartMs
    timecodeEndMs
    createdAt

Назначение:

- хранить куски текста материала;
- искать по ключевым словам в mock RAG;
- формировать цитаты;
- подготовить базу для будущего векторного поиска.

## CardEntity

Карточка для интервального повторения.

Текущая модель в целом подходит, но в Фазе 2 нужно учесть:

    materialId
    subjectId
    packageExternalId
    source

Карточки могут появляться:

- из пакета;
- из материала;
- вручную;
- позже из mock OCR.

## StudySlotEntity

Учебный слот плана.

Поля:

    id
    taskId
    subjectId
    packageExternalId
    materialId
    startDateTime
    endDateTime
    status
    createdAt
    updatedAt

Значения status:

    PROPOSED
    PLANNED
    IN_PROGRESS
    DONE
    MISSED
    CANCELLED

Назначение:

- Proposed — предложенный план;
- Planned — принятый план;
- In progress — запущен фокус;
- Done — выполнен;
- Missed — пропущен;
- Cancelled — отменён пользователем.

Важно:

    Слот должен быть связан не только с заданием, но и с предметом и релевантным материалом.

## PendingEventEntity

Локальная очередь событий синхронизации.

Текущие типы событий:

    PACKAGE_DOWNLOADED
    PLAN_ACCEPTED
    CLASS_CANCELLED
    CARD_REVIEWED
    SLOT_COMPLETED
    TASK_COMPLETED

В Фазе 2 можно добавить локальные события, но не обязательно сразу синхронизировать их на сервер:

    SUBJECT_CREATED
    SCHEDULE_TEMPLATE_CREATED
    TASK_CREATED
    MATERIAL_ADDED
    PLAN_EXPORTED

Принцип:

    Сначала события прогресса.
    Потом события CRUD локальных данных, если появится multi-device sync.

## Связи между сущностями

Целевая граф:

    Subject
      -> ScheduleTemplate
          -> ClassInstance
      -> Task
      -> Material
          -> MaterialChunk
          -> Card
      -> StudySlot
          -> Task
          -> Material

Экран Сегодня агрегирует:

    ClassInstance
    StudySlot
    Task
    Card
    MaterialChunk / Citation

## Миграция данных

Так как текущий проект учебный и база локальная, допустимо использовать:

    fallbackToDestructiveMigration()

Но в документации нужно зафиксировать, что для релиза потребуется нормальная миграция.

Стратегия Фазы 2:

1. Добавить новые таблицы.
2. Перевести существующие package-derived данные в новую модель с source = PACKAGE.
3. Оставить старые таблицы временно, если это упрощает миграцию.
4. После стабилизации удалить дублирующие сущности.

## Planner inputs

Планировщик должен читать:

    Активные Task
    Remaining minutes
    Deadline date
    Priority
    ScheduleTemplate
    ClassInstance status
    Отменённые занятия
    Существующие StudySlot
    Прошедшие слоты
    Предпочтения пользователя

## Planner outputs

Планировщик создаёт:

    StudySlot со статусом PROPOSED

После принятия:

    PROPOSED -> PLANNED

После запуска фокуса:

    PLANNED -> IN_PROGRESS

После завершения:

    IN_PROGRESS -> DONE

Если время вышло, а слот не выполнен:

    PLANNED -> MISSED

## Q&A inputs

Q&A должен читать:

    Material
    MaterialChunk
    Subject
    Task context
    Slot context

## Q&A outputs

Q&A возвращает:

    answer text
    citations
    source material
    source page или timecode
    confidence flag

Если контекст не найден:

    answer = null
    fallback available = true
    message = В ваших материалах этого нет

## Итоговые приоритеты модели данных

1. Subject.
2. ScheduleTemplate.
3. Расширенный ClassInstance.
4. Ручной Task.
5. Ручной Material.
6. MaterialChunk.
7. Расширенный StudySlot.
8. Связь slot -> material по релевантности.