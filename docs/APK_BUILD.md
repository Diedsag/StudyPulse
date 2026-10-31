# StudyPulse: как собрать APK

## Самый быстрый способ: debug APK

Debug APK подходит для установки на своё устройство или эмулятор.

### Через Android Studio

1. Открой проект.
2. Выбери:

```text
Build -> Build Bundle(s) / APK(s) -> Build APK(s)
```

3. Дождись окончания сборки.
4. В правом нижнем углу появится уведомление.
5. Нажми `locate`.

Обычно APK лежит здесь:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

### Через PowerShell / командную строку

Перейди в папку `android`:

```powershell
Set-Location -LiteralPath 'C:\Users\lelec\AndroidStudioProjects\StudyPulse\StudyPulse\android'
```

Собери debug APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

Готовый файл:

```text
android\app\build\outputs\apk\debug\app-debug.apk
```

---

## Установка APK на устройство

Подключи телефон через USB и разреши отладку.

Проверь устройство:

```powershell
adb devices
```

Установи APK:

```powershell
adb install -r "C:\Users\lelec\AndroidStudioProjects\StudyPulse\StudyPulse\android\app\build\outputs\apk\debug\app-debug.apk"
```

Если нужно удалить старую версию перед установкой:

```powershell
adb uninstall com.studypulse.sync
```

Затем снова:

```powershell
adb install "C:\Users\lelec\AndroidStudioProjects\StudyPulse\StudyPulse\android\app\build\outputs\apk\debug\app-debug.apk"
```

---

## Release APK без подписи

Можно собрать release-вариант:

```powershell
Set-Location -LiteralPath 'C:\Users\lelec\AndroidStudioProjects\StudyPulse\StudyPulse\android'
.\gradlew.bat :app:assembleRelease
```

Но такой APK обычно не устанавливается как полноценный релизный, потому что он не подписан.

Путь будет примерно такой:

```text
android\app\build\outputs\apk\release\app-release-unsigned.apk
```

Для настоящей раздачи нужен подписанный release APK или AAB.

---

## Что использовать сейчас

Для демонстрации и проверки достаточно:

```text
debug APK
```

Он собирается быстрее и не требует keystore.

Для защиты/показа лучше заранее один раз собрать debug APK и проверить установку на реальном устройстве.