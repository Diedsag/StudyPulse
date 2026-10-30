# StudyPulse Technical Decisions

## 1. Платформа

Android-клиент + Flask REST API.

## 2. Язык Android

Kotlin.

## 3. UI

Jetpack Compose + Material 3.

Причина:

- быстрее генерировать экраны;
- меньше boilerplate;
- удобно работать со StateFlow.

Если Compose вызовет проблемы по времени, fallback — XML, но решение фиксируем заранее: начинаем с Compose.

## 4. Архитектура Android

MVVM + Repository + UseCases.

Слои:

UI
 ↓
ViewModel
 ↓
UseCases
 ↓
Repositories
 ↓
Room / DataStore / Retrofit / AiGateway

## 5. Локальное хранение

Room.

Room — источник истины для:

- пакетов;
- расписания;
- заданий;
- материалов;
- карточек;
- логов повторений;
- очереди синхронизации;
- кэша статистики и рейтинга.

## 6. Настройки

DataStore Preferences.

Храним:

- server URL;
- device_uid;
- nickname;
- rating opt-in;
- last sync.

## 7. Сетевой клиент

Retrofit + OkHttp + kotlinx.serialization.

Причина:

- стабильно;
- много примеров;
- хорошо сочетается с Coroutines.

## 8. DI

Ручной AppContainer.

Не используем Hilt в MVP.

Причина:

- меньше версионных зависимостей;
- быстрее собрать;
- проще отлаживать.

## 9. Авторизация

Без логина.

Используем device_uid.

Причина:

- MVP не требует реальной безопасности;
- ускоряет разработку;
- сервер всё равно может считать рейтинг и статистику.

В roadmap: JWT / OAuth / роли.

## 10. Backend

Flask + SQLAlchemy + SQLite.

Причина:

- быстрый старт;
- понятен;
- легко запускать локально;
- достаточно для MVP.

## 11. Синхронизация

Offline-first.

Клиент копит события в PendingEventEntity.

Сервер принимает батчи.

Идемпотентность через client_event_id.

## 12. Планировщик

Локальный, детерминированный.

Горизонт: 3 дня.

Дневное окно: 08:00–22:00.

Минимальный слот: 30 минут.

Максимальная дневная нагрузка: 180 минут.

Сортировка задач:

1. deadline;
2. priority desc;
3. remaining minutes desc.

## 13. Интервальное повторение

Упрощённый SM-2.

Оценки:

- AGAIN = 1;
- HARD = 2;
- GOOD = 3;
- EASY = 4.

Правила:

AGAIN:
- repetitions = 0;
- lapses + 1;
- interval = 0;
- dueAt = now + 10 minutes;
- ease -= 0.2, min 1.3.

HARD:
- если interval=0, new interval=1 day;
- иначе interval *= 1.2;
- ease -= 0.15, min 1.3.

GOOD:
- repetitions 0 → interval 1 day;
- repetitions 1 → interval 3 days;
- иначе interval *= ease.

EASY:
- repetitions 0 → interval 3 days;
- repetitions 1 → interval 5 days;
- иначе interval *= ease * 1.3;
- ease += 0.15, max 3.0.

## 14. AI-часть

В MVP нет настоящего AI.

Есть AiGateway интерфейс.

Реализация:

- MockAiGateway;
- заранее подготовленные ответы;
- или простой keyword search по material.summary_text и key_facts.

## 15. Рейтинг

Серверный.

Клиент может показывать preliminary XP локально, но финальные значения берутся с сервера.

## 16. Уведомления и виджет

Не входят в MVP.

Могут быть Should/Could, только если основной сценарий уже стабилен.

## 17. Шифрование

SQLCipher не используем в MVP.

В roadmap.

## 18. Фоновая синхронизация

WorkManager не обязателен в MVP.

Достаточно ручной кнопки Sync и авто-sync при открытии приложения, если есть сеть.

## 19. Тестирование

Ручное по чек-листу.

Юнит-тесты желательны для:

- scheduler;
- planner;
- XP calculation;
- sync deduplication.

Но не блокируют MVP, если время ограничено.