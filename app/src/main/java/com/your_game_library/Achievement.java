package com.your_game_library;

public class Achievement {
    public String id;
    public String title;
    public String description;
    public int iconResId;
    public boolean isUnlocked;
    public String progressText;
    public String targetGameName;

    // ДОДАНО: Прапорець, який каже, чи треба перефарбовувати цю іконку
    public boolean isVectorIcon;
    // Метод генерує унікальний ключ для збереження статусу "показано"
    public String getPrefsKey() {
        return "achiev_shown_" + id;
    }
    // Конструктор для глобальних ачівок (з векторними іконками)
    public Achievement(String id, String title, String description, int iconResId) {
        this(id, title, description, iconResId, null, true);
    }

    // Конструктор для ігрових ачівок (з векторними іконками)
    public Achievement(String id, String title, String description, int iconResId, String targetGameName) {
        this(id, title, description, iconResId, targetGameName, true);
    }

    // ГОЛОВНИЙ КОНСТРУКТОР (дозволяє вказати, чи це векторна іконка)
    public Achievement(String id, String title, String description, int iconResId, String targetGameName, boolean isVectorIcon) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.iconResId = iconResId;
        this.targetGameName = targetGameName;
        this.isVectorIcon = isVectorIcon;
        this.isUnlocked = false;
        this.progressText = "";
    }
}