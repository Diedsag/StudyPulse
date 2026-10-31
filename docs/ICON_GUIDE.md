# StudyPulse: руководство по иконке

## Рекомендуемый стиль иконки

Иконка должна быть простой, геометричной и читаемой в маленьком размере.

### Концепция

```text
Тёмно-синий фон
Бело-голубой символ
Календарь + пульс/волна + галочка
Без текста
Без лишних деталей
```

### Смысл

- календарь — расписание и планирование;
- пульс — ежедневный ритм учёбы;
- галочка — выполненные задачи и прогресс.

---

## Промт для ChatGPT / другого image generator

Скопируй этот промт:

```text
Create a minimalist flat vector-style mobile app icon for "StudyPulse".

Size: 1024x1024 pixels, square canvas.
No text, no letters, no words.
Centered symbol only.

Concept: a simple rounded calendar page combined with a subtle pulse/wave line and a small checkmark. The symbol should communicate study planning, daily rhythm, progress, and completion.

Style: modern Material Design, flat geometric vector, crisp edges, high contrast, clean negative space, simple shapes, no photorealism, no shadows, no gradients unless extremely subtle.

Colors:
- background: deep navy blue #0F172A
- main symbol: white and electric blue #2E5BFF
- accent: teal #6FD9CB

Composition:
- leave generous padding around the symbol;
- keep the main symbol inside the central 65-70% of the canvas;
- suitable for Android adaptive icon foreground;
- transparent background preferred, or solid deep navy background.

Important:
- no app name text;
- no watermark;
- no realistic photo;
- no complex illustration;
- no tiny details that disappear at small sizes.
```

---

## Если ChatGPT сгенерировал изображение

Сохрани как PNG, например:

```text
studypulse_icon_source.png
```

Проверь:

- квадратное;
- без текста;
- символ по центру;
- не слишком мелкий;
- хорошо читается в размере 48x48;
- лучше прозрачный фон, но можно и сплошной тёмно-синий.

---

## Как добавить иконку в Android Studio

Самый быстрый способ — через Image Asset.

### Шаги

1. Открой Android Studio.
2. В дереве проекта выбери:

```text
app/src/main/res
```

3. Правый клик по `res` -> `New` -> `Image Asset`.
4. В окне Asset Studio выбери:

```text
Icon Type: Launcher Icons (Adaptive and Legacy)
```

5. Вкладка `Foreground Layer`:

```text
Asset Type: Image
Source Asset: выбранный PNG-файл
```

6. Проверь preview.

Если символ слишком большой и выходит за безопасную зону, уменьши `Scaling` примерно до 80–90%.

7. Вкладка `Background Layer`:

```text
Asset Type: Color
Color: #0F172A
```

8. Нажми `Next`, затем `Finish`.

Android Studio сгенерирует:

```text
mipmap-anydpi-v26/ic_launcher.xml
mipmap-anydpi-v26/ic_launcher_round.xml
mipmap-* / ic_launcher.webp или png
mipmap-* / ic_launcher_round.webp или png
drawable / ic_launcher_foreground.png
values / ic_launcher_background.xml
```

После этого сделай:

```text
Build -> Clean Project
Build -> Rebuild Project
```

---

## Альтернатива: своя простая векторная иконка

Если не хочешь генерировать картинку, можно сделать очень простую векторную иконку:

```text
Фон: #0F172A
Передний план: белый прямоугольник со скруглением, как календарь
Внутри: синяя линия пульса
Маленькая teal-галочка в углу
```

Это будет даже надёжнее, чем сгенерированный растр, потому что вектор не теряет качество.

Но для быстрого старта PNG через Image Asset — нормальный вариант.