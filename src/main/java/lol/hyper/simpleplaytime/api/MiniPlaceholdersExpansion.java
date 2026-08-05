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

import io.github.miniplaceholders.api.Expansion;
import lol.hyper.simpleplaytime.SimplePlayTime;
import lol.hyper.simpleplaytime.database.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import org.bukkit.entity.Player;

public class MiniPlaceholdersExpansion {

    private final SimplePlayTime plugin;

    public MiniPlaceholdersExpansion(SimplePlayTime plugin) {
        this.plugin = plugin;
    }

    public Expansion createExpansion() {
        return Expansion.builder("simpleplaytime")
                .filter(Player.class)
                .audiencePlaceholder("total_seconds", (audience, ctx, queue) -> {
                    Player player = (Player) audience;
                    plugin.getLogger().info("MiniPlaceholders: total_seconds called for player " + player.getName());
                    PlayerData playerData = plugin.getPlayerData(player.getUniqueId());
                    if (playerData == null) {
                        plugin.getLogger().warning("MiniPlaceholders: No player data found for " + player.getName());
                        return Tag.selfClosingInserting(Component.text("0"));
                    }
                    long value = playerData.getTotalPlaytimeWithSession();
                    plugin.getLogger().info("MiniPlaceholders: total_seconds returning " + value);
                    return Tag.selfClosingInserting(Component.text(String.valueOf(value)));
                })
                .audiencePlaceholder("daily_seconds", (audience, ctx, queue) -> {
                    Player player = (Player) audience;
                    PlayerData playerData = plugin.getPlayerData(player.getUniqueId());
                    if (playerData == null) {
                        return Tag.selfClosingInserting(Component.text("0"));
                    }
                    return Tag.selfClosingInserting(Component.text(String.valueOf(playerData.getDailyPlaytimeWithSession())));
                })
                .audiencePlaceholder("total_minutes", (audience, ctx, queue) -> {
                    Player player = (Player) audience;
                    PlayerData playerData = plugin.getPlayerData(player.getUniqueId());
                    if (playerData == null) {
                        return Tag.selfClosingInserting(Component.text("0"));
                    }
                    return Tag.selfClosingInserting(Component.text(String.valueOf(playerData.getTotalPlaytimeWithSession() / 60)));
                })
                .audiencePlaceholder("daily_minutes", (audience, ctx, queue) -> {
                    Player player = (Player) audience;
                    PlayerData playerData = plugin.getPlayerData(player.getUniqueId());
                    if (playerData == null) {
                        return Tag.selfClosingInserting(Component.text("0"));
                    }
                    return Tag.selfClosingInserting(Component.text(String.valueOf(playerData.getDailyPlaytimeWithSession() / 60)));
                })
                .audiencePlaceholder("total_hours", (audience, ctx, queue) -> {
                    Player player = (Player) audience;
                    PlayerData playerData = plugin.getPlayerData(player.getUniqueId());
                    if (playerData == null) {
                        return Tag.selfClosingInserting(Component.text("0"));
                    }
                    return Tag.selfClosingInserting(Component.text(String.valueOf(playerData.getTotalPlaytimeWithSession() / 3600)));
                })
                .audiencePlaceholder("daily_hours", (audience, ctx, queue) -> {
                    Player player = (Player) audience;
                    PlayerData playerData = plugin.getPlayerData(player.getUniqueId());
                    if (playerData == null) {
                        return Tag.selfClosingInserting(Component.text("0"));
                    }
                    return Tag.selfClosingInserting(Component.text(String.valueOf(playerData.getDailyPlaytimeWithSession() / 3600)));
                })
                .audiencePlaceholder("total_formatted", (audience, ctx, queue) -> {
                    Player player = (Player) audience;
                    PlayerData playerData = plugin.getPlayerData(player.getUniqueId());
                    if (playerData == null) {
                        return Tag.selfClosingInserting(Component.text("0"));
                    }
                    return Tag.selfClosingInserting(Component.text(PlayerData.formatTime(playerData.getTotalPlaytimeWithSession())));
                })
                .audiencePlaceholder("daily_formatted", (audience, ctx, queue) -> {
                    Player player = (Player) audience;
                    PlayerData playerData = plugin.getPlayerData(player.getUniqueId());
                    if (playerData == null) {
                        return Tag.selfClosingInserting(Component.text("0"));
                    }
                    return Tag.selfClosingInserting(Component.text(PlayerData.formatTime(playerData.getDailyPlaytimeWithSession())));
                })
                // 添加全局变量测试
                .globalPlaceholder("test", (ctx, queue) -> {
                    plugin.getLogger().info("MiniPlaceholders: global test placeholder called");
                    return Tag.selfClosingInserting(Component.text("TEST_WORKING"));
                })
                .build();
    }
}
