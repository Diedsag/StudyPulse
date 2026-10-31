# StudyPulse: Фаза 3.1 — онбординг и профиль студента

## Цель

Сделать первый запуск приложения понятным для нового пользователя.

До Фазы 3.1 пользователь сразу попадал в интерфейс с множеством вкладок и технических сущностей. После Фазы 3.1 приложение сначала проводит короткий онбординг и сохраняет локальный профиль студента.

---

## Что добавлено

### 1. StudentProfile

Локальный профиль студента хранится в DataStore.

Поля:

```text
onboardingCompleted
displayName
studyStartHour
studyEndHour
defaultDailyMinutes
notificationsEnabled
autoPlanEnabled
```

Файл:

```text
android/app/src/main/java/com/studypulse/sync/data/profile/ProfileRepository.kt
```

---

### 2. Онбординг

Минимальный онбординг из 4 шагов:

```text
1. Приветствие
2. Имя пользователя
3. Удобное время учёбы и дневная нагрузка
4. Уведомления и автоплан
```

Файлы:

```text
android/app/src/main/java/com/studypulse/sync/ui/onboarding/OnboardingViewModel.kt
android/app/src/main/java/com/studypulse/sync/ui/onboarding/OnboardingScreen.kt
```

---

### 3. Интеграция в MainActivity

Приложение теперь открывается экраном `StudyPulseRoot`.

Логика:

```text
если onboardingCompleted == false -> показать OnboardingScreen
если onboardingCompleted == true  -> показать основное приложение
```

---

## Что сознательно НЕ делаем в 3.1

В этой фазе не добавляем:

```text
аккаунт;
email;
пароль;
OAuth;
серверную регистрацию;
облачную синхронизацию профиля;
сложную настройку уведомлений;
настоящий автоплан;
импорт календаря;
OCR;
RAG.
```

Это будет позже, если потребуется.

---

## Как сбросить онбординг

Во время разработки можно сбросить онбординг очисткой данных приложения:

```powershell
adb shell pm clear com.studypulse.sync
```

После этого приложение снова покажет онбординг.

---

## Критерий готовности

```text
Пользователь после установки видит понятный онбординг,
указывает базовые предпочтения,
сохраняет профиль локально,
и попадает в основное приложение.
```