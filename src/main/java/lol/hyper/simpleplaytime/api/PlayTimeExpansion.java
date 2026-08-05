/*
 * This file is part of SimplePlayTime.
 *
 * SimplePlayTime is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SimplePlayTime is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SimplePlayTime.  If not, see <https://www.gnu.org/licenses/>.
 */

package lol.hyper.simpleplaytime.api;

import lol.hyper.simpleplaytime.SimplePlayTime;
import lol.hyper.simpleplaytime.database.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayTimeExpansion extends PlaceholderExpansion {

    private final SimplePlayTime plugin;

    public PlayTimeExpansion(SimplePlayTime plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "simpleplaytime";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return null;
        }

        PlayerData playerData = plugin.getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return "0";
        }

        switch (params.toLowerCase()) {
            // 总在线秒数
            case "total_seconds":
                return String.valueOf(playerData.getTotalPlaytimeWithSession());

            // 当日在线秒数
            case "daily_seconds":
                return String.valueOf(playerData.getDailyPlaytimeWithSession());

            // 总在线分钟数
            case "total_minutes":
                return String.valueOf(playerData.getTotalPlaytimeWithSession() / 60);

            // 当日在线分钟数
            case "daily_minutes":
                return String.valueOf(playerData.getDailyPlaytimeWithSession() / 60);

            // 总在线小时数
            case "total_hours":
                return String.valueOf(playerData.getTotalPlaytimeWithSession() / 3600);

            // 当日在线小时数
            case "daily_hours":
                return String.valueOf(playerData.getDailyPlaytimeWithSession() / 3600);

            // 总在线时间(格式化)
            case "total_formatted":
                return PlayerData.formatTime(playerData.getTotalPlaytimeWithSession());

            // 当日在线时间(格式化)
            case "daily_formatted":
                return PlayerData.formatTime(playerData.getDailyPlaytimeWithSession());

            default:
                return null;
        }
    }
}
