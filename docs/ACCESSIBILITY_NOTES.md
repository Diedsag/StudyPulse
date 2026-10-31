# StudyPulse — заметки по доступности

Фаза 4 включила минимальный accessibility-пакет для экрана «Материалы». Это не полная a11y-аудит проекта, а базовая семантика, чтобы TalkBack не читал экран фрагментированно.

---

## 1. Что сделано в MaterialsScreen

```text
1. state.message получил liveRegion = Polite.
2. Заголовок материала получил semantics heading().
3. Кнопки карточки получили contentDescription с глаголом и названием материала.
4. Ряд предметов получил selectableGroup(), role RadioButton и selected.
5. Описательный блок карточки объединён через semantics(mergeDescendants = true) { }.
6. Поле Название получает фокус при открытии формы через FocusRequester + runCatching.
```

---

## 2. Почему mergeDescendants только на описательный блок

Если объединить всю карточку, кнопки действий могут слиться в один узел доступности и стать недоступны по отдельности. Поэтому:

```text
описательный Column: mergeDescendants = true
блок кнопок: отдельные фокусы
```

---

## 3. Почему contentDescription включает действие и контекст

В Compose `contentDescription` на кнопке переопределяет читаемый текст потомка. Если написать просто «Повторить карточки», TalkBack может потерять контекст материала. Поэтому используется формат:

```text
Повторить карточки для материала <название>
Задать вопрос по материалу <название>
Редактировать материал <название>
Удалить материал <название>
```

---

## 4. Что осталось на будущую фазу

```text
FilterChip вместо Button/OutlinedButton для предметов;
перенос всех строк в strings.xml;
ModalBottomSheet/Dialog для формы;
полный TalkBack-прогон;
Google Accessibility Scanner;
Espresso AccessibilityChecks в androidTest;
распространение семантики на ScheduleScreen, ReviewScreen, StatsScreen, SettingsScreen.
```

Это не блокирует демо Фазы 4, но полезно для релизной полировки.