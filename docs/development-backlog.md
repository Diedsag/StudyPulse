# StudyPulse Development Backlog

Порядок работы после блока 0.

## Slice 1. Backend skeleton

Цель: Flask отвечает и есть seed-данные.

Задачи:

1. Создать структуру backend.
2. Поднять Flask app factory.
3. Добавить SQLAlchemy.
4. Добавить /health.
5. Добавить модели:
   - Device;
   - StudyPackage;
   - PackageScheduleItem;
   - PackageTask;
   - PackageMaterial;
   - PackageCard;
   - SyncEvent.
6. Добавить seed.
7. Реализовать:
   - POST /api/devices/register;
   - GET /api/packages;
   - GET /api/packages/{id}/bundle.
8. Проверить через браузер/curl/Postman.

Критерий:

- http://localhost:5000/health работает.
- http://10.0.2.2:5000/health работает из Android emulator.
- seed создаёт 2 пакета.

## Slice 2. Android skeleton

Цель: приложение запускается и имеет настройки.

Задачи:

1. Создать Android проект.
2. Подключить зависимости.
3. Создать структуру папок.
4. Добавить Application и AppContainer.
5. Добавить DataStore settings.
6. Добавить Retrofit client.
7. Добавить экран Settings.
8. Добавить проверку /health.
9. Сгенерировать device_uid.

Критерий:

- приложение запускается;
- сохраняет server URL;
- показывает device_uid;
- проверяет соединение.

## Slice 3. Packages download

Цель: приложение скачивает пакет и хранит локально.

Задачи:

1. Добавить Room entities.
2. Добавить DAO.
3. Добавить AppDatabase.
4. Добавить PackageRepository.
5. Добавить экран Packages.
6. Реализовать скачивание bundle.
7. Сохранять в Room транзакцией.
8. Показать статус downloaded.

Критерий:

- список пакетов приходит с сервера;
- пакет скачивается;
- без интернета список берётся из Room.

## Slice 4. Schedule and tasks

Цель: студент видит расписание и задания.

Задачи:

1. Экран Schedule.
2. Экран Tasks.
3. Отмена занятия.
4. ClassInstance или простая таблица cancellations.
5. Отображение статуса занятия.

Критерий:

- занятия отображаются;
- задание отображается;
- занятие можно отменить.

## Slice 5. Planner

Цель: приложение строит учебные слоты.

Задачи:

1. StudySlotEntity.
2. PlannerUseCase.
3. Расчёт занятых интервалов.
4. Расчёт свободных окон.
5. Распределение задач.
6. Предпросмотр плана.
7. Принятие плана.
8. Событие PLAN_ACCEPTED.

Критерий:

- кнопка «Спланировать» создаёт слоты;
- отменённое занятие освобождает окно;
- план можно принять.

## Slice 6. Materials and cards

Цель: внутри пакета есть материал и карточки.

Задачи:

1. Экран Material.
2. Отображение summary и key facts.
3. Экран Cards list.
4. Review screen.
5. SpacedRepetitionScheduler.
6. ReviewLog.
7. PendingEvent CARD_REVIEWED.

Критерий:

- материал открывается;
- карточки проходятся;
- dueAt обновляется;
- логи сохраняются.

## Slice 7. Slot completion

Цель: слот можно выполнить.

Задачи:

1. Кнопка «Отметить выполненным».
2. Обновление статуса слота.
3. PendingEvent SLOT_COMPLETED.
4. Возможно, уменьшение remainingMinutes у задачи.

Критерий:

- слот становится DONE;
- создаётся событие.

## Slice 8. Sync

Цель: офлайн-прогресс улетает на сервер.

Задачи:

1. Backend POST /api/sync/batch.
2. Android SyncRepository.
3. Отправка unsynced events.
4. Обработка accepted/duplicated/rejected.
5. Пометка synced=true.
6. Отображение pending count.
7. Обновление last_sync_at.

Критерий:

- после sync сервер получает события;
- дубли не начисляют XP повторно;
- pending count становится 0 при успехе.

## Slice 9. Stats and rating

Цель: показать прогресс и мотивацию.

Задачи:

1. Backend XP calculation.
2. Backend level calculation.
3. Backend streak calculation.
4. GET /api/me/stats.
5. GET /api/leaderboard.
6. Android StatsScreen.
7. Android LeaderboardScreen.
8. Локальный кэш.

Критерий:

- после sync обновляются XP/level/streak;
- leaderboard показывает своё место.

## Slice 10. Polish

Цель: стабильный продукт.

Задачи:

1. Empty states.
2. Error states.
3. Защита от crash.
4. Красивые seed-данные.
5. Проверка полного сценария.
6. Исправление багов.

Критерий:

- главный сценарий проходит без ручных вмешательств.