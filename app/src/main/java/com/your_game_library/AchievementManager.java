package com.your_game_library;

import java.util.ArrayList;
import java.util.List;

public class AchievementManager {

    public static List<Achievement> calculateAchievements(List<com.your_game_library.Game> allGames) {
        List<Achievement> achievements = initAchievements();

        float totalHours = 0f; // ВИПРАВЛЕНО НА float
        int completedCount = 0;
        float maxHoursInOneGame = 0f; // ВИПРАВЛЕНО НА float

        // Збираємо загальну глобальну статистику
        for (com.your_game_library.Game g : allGames) {
            if (g.getTime() != null && g.getTime() > 0) {
                totalHours += g.getTime();
                if (g.getTime() > maxHoursInOneGame) {
                    maxHoursInOneGame = g.getTime();
                }
            }
            if ("completed".equalsIgnoreCase(g.getCategory())) {
                completedCount++;
            }
        }

        // --- ЛОГІКА РОЗБЛОКУВАННЯ ---
        for (Achievement a : achievements) {

            // 1. ПЕРЕВІРКА ГЛОБАЛЬНИХ АЧІВОК
            if (a.targetGameName == null || a.targetGameName.isEmpty()) {
                switch (a.id) {
                    case "novice_gamer":
                        a.isUnlocked = totalHours >= 10f;
                        a.progressText = String.format(java.util.Locale.getDefault(), "%.0f / 10h", Math.min(totalHours, 10f));
                        break;
                    case "hardcore_gamer":
                        a.isUnlocked = totalHours >= 100f;
                        a.progressText = String.format(java.util.Locale.getDefault(), "%.0f / 100h", Math.min(totalHours, 100f));
                        break;
                    case "no_life":
                        a.isUnlocked = totalHours >= 1000f;
                        a.progressText = String.format(java.util.Locale.getDefault(), "%.0f / 1000h", Math.min(totalHours, 1000f));
                        break;
                    case "game_finisher":
                        a.isUnlocked = completedCount >= 1;
                        a.progressText = Math.min(completedCount, 1) + " / 1 game";
                        break;
                    case "completionist":
                        a.isUnlocked = completedCount >= 50;
                        a.progressText = Math.min(completedCount, 50) + " / 50 games";
                        break;
                    case "one_game_wonder":
                        a.isUnlocked = maxHoursInOneGame >= 200f;
                        a.progressText = String.format(java.util.Locale.getDefault(), "%.0f / 200h (in one game)", Math.min(maxHoursInOneGame, 200f));
                        break;
                }
            }
            // 2. ПЕРЕВІРКА СПЕЦИФІЧНИХ ІГРОВИХ АЧІВОК
            else {
                // Шукаємо, чи є ця конкретна гра у базі даних гравця
                com.your_game_library.Game targetGame = null;
                for (com.your_game_library.Game g : allGames) {
                    if (g.getName() != null && g.getName().equalsIgnoreCase(a.targetGameName)) {
                        targetGame = g;
                        break;
                    }
                }

                if (targetGame != null) {
                    // ГРА Є В БІБЛІОТЕЦІ! Перевіряємо умови:
                    float gameTime = (targetGame.getTime() != null) ? targetGame.getTime() : 0f;
                    boolean isCompleted = "completed".equalsIgnoreCase(targetGame.getCategory());
                    String compType = (targetGame.getType() != null) ? targetGame.getType() : "";

                    switch (a.id) {
                        case "witcher_master": // Приклад: Пройти Відьмака на 100%
                            a.isUnlocked = isCompleted && compType.equals("100% Completion");
                            a.progressText = a.isUnlocked ? "Done" : "Needs 100% run";
                            break;

                        case "skyrim_long": // Приклад: Провести в Скайрімі 300 годин
                            a.isUnlocked = gameTime >= 300f;
                            a.progressText = String.format(java.util.Locale.getDefault(), "%.0f / 300h", Math.min(gameTime, 300f));
                            break;

                        case "ds3":
                            a.isUnlocked = gameTime >= 200f;
                            a.progressText = String.format(java.util.Locale.getDefault(), "%.0f / 200h", Math.min(gameTime, 200f));
                            break;
                    }
                } else {
                    // ГРИ НЕМАЄ В БІБЛІОТЕЦІ
                    a.isUnlocked = false;
                    a.progressText = "Game not owned";
                }
            }
        }

        return achievements;
    }

    // --- РЕЄСТР УСІХ АЧІВОК ---
    private static List<Achievement> initAchievements() {
        List<Achievement> list = new ArrayList<>();

        // 1. Глобальні ачівки (як було раніше)
        list.add(new Achievement("novice_gamer", "Novice Gamer", "Play your first 10 hours.", R.drawable.ic_igdb));
        list.add(new Achievement("hardcore_gamer", "Hardcore", "Spend 100 hours gaming.", R.drawable.ic_ps));
        list.add(new Achievement("no_life", "No Life", "Reach 1000 hours of total playtime.", R.drawable.ic_steam));
        list.add(new Achievement("game_finisher", "First Blood", "Complete your first game.", R.drawable.ic_collections));
        list.add(new Achievement("completionist", "Completionist", "Complete 50 games.", R.drawable.ic_safe));
        list.add(new Achievement("one_game_wonder", "Dedication", "Spend 200+ hours in a single game.", R.drawable.ic_xbox));

        // 2. Специфічні ігрові ачівки (ОСТАННІЙ АРГУМЕНТ — НАЗВА ГРИ!)
        list.add(new Achievement("ds3", "Незвичайна людина", "200 годин в ДС3, Влад, ти не звичайна людина.", R.drawable.ds3, "Dark Souls III", false));

        return list;
    }
}