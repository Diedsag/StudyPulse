StudyPulse network configuration

Problem

Earlier, connecting from a real phone to a local Flask server required editing
android/app/src/main/res/xml/network_security_config.xml and adding the
computer LAN IP manually. That was inconvenient and fragile.

Solution

Android now uses variant-specific network security configs:

- debug build allows cleartext HTTP for development servers;
- release build allows cleartext only for localhost, 127.0.0.1 and emulator 10.0.2.2;
- main config keeps the safe default.

Files

android/app/src/debug/res/xml/network_security_config.xml
android/app/src/release/res/xml/network_security_config.xml
android/app/src/main/res/xml/network_security_config.xml

Developer workflow

For debug APK, set server URL in app settings to:

http://<computer-local-ip>:5000

Example:

http://192.168.1.45:5000

No XML edit is needed.

USB alternative

If phone is connected by USB, you can also use adb reverse:

adb reverse tcp:5000 tcp:5000

Then set server URL to:

http://localhost:5000

This works even with stricter configs because localhost is allowed.

Production note

For a real release product, use HTTPS. Do not ship a release build with global
cleartext enabled.