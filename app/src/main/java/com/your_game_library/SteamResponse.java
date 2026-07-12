package com.your_game_library;

import java.util.List;

public class SteamResponse {
    public ResponseBody response;

    public static class ResponseBody {
        public int game_count;
        public List<SteamGame> games;
    }

    public static class SteamGame {
        public int appid;
        public String name;
        public int playtime_forever; // Час у хвилинах
        public long rtime_last_played; // Час останнього запуску (Unix timestamp у секундах)
        public String img_icon_url;
    }
}