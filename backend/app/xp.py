from datetime import date, timedelta

LEVEL_THRESHOLDS = [
    0,
    100,
    250,
    500,
    900,
    1500,
    2500,
]


def current_week_key(value: date) -> str:
    iso = value.isocalendar()
    return f"{iso[0]}-W{iso[1]:02d}"


def calculate_level(total_xp: int) -> int:
    level = 1

    for index, threshold in enumerate(LEVEL_THRESHOLDS, start=1):
        if total_xp >= threshold:
            level = index

    return level


def update_device_progress(
    device,
    awarded_xp: int,
    event_type: str,
    event_date: date,
    now_date: date,
) -> None:
    period_key = current_week_key(now_date)

    if device.weekly_period_key != period_key:
        device.weekly_period_key = period_key
        device.weekly_xp = 0

    device.total_xp = (device.total_xp or 0) + awarded_xp
    device.weekly_xp = (device.weekly_xp or 0) + awarded_xp
    device.level = calculate_level(device.total_xp)

    if event_type == "PACKAGE_DOWNLOADED":
        return

    last_active = device.last_active_date

    if last_active is None or event_date >= last_active:
        if last_active is None:
            device.streak_days = 1
        elif last_active == event_date:
            pass
        elif last_active == event_date - timedelta(days=1):
            device.streak_days = (device.streak_days or 0) + 1
        else:
            device.streak_days = 1

        device.last_active_date = event_date