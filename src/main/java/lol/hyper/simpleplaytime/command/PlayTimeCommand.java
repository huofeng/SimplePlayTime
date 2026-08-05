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

package lol.hyper.simpleplaytime.command;

import lol.hyper.simpleplaytime.SimplePlayTime;
import lol.hyper.simpleplaytime.database.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlayTimeCommand implements CommandExecutor {

    private final SimplePlayTime simplePlayTime;

    public PlayTimeCommand(SimplePlayTime simplePlayTime) {
        this.simplePlayTime = simplePlayTime;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            sender.sendMessage(simplePlayTime.getMessage("messages.players-only"));
            return true;
        }

        if (!sender.hasPermission("simpleplaytime.command")) {
            sender.sendMessage(Component.text("You do not have permission for this command.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("simpleplaytime.reload")) {
                simplePlayTime.loadConfig();
                sender.sendMessage(Component.text("Configuration reloaded!").color(NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("You do not have permission for this command.").color(NamedTextColor.RED));
            }
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("debug")) {
            if (sender.hasPermission("simpleplaytime.reload")) {
                Player player = (Player) sender;
                PlayerData playerData = simplePlayTime.getPlayerData(player.getUniqueId());
                if (playerData != null) {
                    sender.sendMessage(Component.text("=== 调试信息 ===").color(NamedTextColor.YELLOW));
                    sender.sendMessage(Component.text("玩家UUID: " + player.getUniqueId()).color(NamedTextColor.GRAY));
                    sender.sendMessage(Component.text("总秒数: " + playerData.getTotalPlaytimeWithSession()).color(NamedTextColor.GREEN));
                    sender.sendMessage(Component.text("今日秒数: " + playerData.getDailyPlaytimeWithSession()).color(NamedTextColor.GREEN));
                    sender.sendMessage(Component.text("总格式化: " + PlayerData.formatTime(playerData.getTotalPlaytimeWithSession())).color(NamedTextColor.GREEN));
                    sender.sendMessage(Component.text("今日格式化: " + PlayerData.formatTime(playerData.getDailyPlaytimeWithSession())).color(NamedTextColor.GREEN));

                    // 测试 MiniPlaceholders 变量
                    sender.sendMessage(Component.text("=== 变量测试 ===").color(NamedTextColor.YELLOW));

                    // 使用 MiniPlaceholders 的 TagResolver
                    try {
                        var audienceResolver = io.github.miniplaceholders.api.MiniPlaceholders.getAudiencePlaceholders(player);

                        sender.sendMessage(Component.text("数值变量:").color(NamedTextColor.GRAY));
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("总秒数: <simpleplaytime_total_seconds>", audienceResolver));
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("今日秒数: <simpleplaytime_daily_seconds>", audienceResolver));
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("总分钟数: <simpleplaytime_total_minutes>", audienceResolver));
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("今日分钟数: <simpleplaytime_daily_minutes>", audienceResolver));
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("总小时数: <simpleplaytime_total_hours>", audienceResolver));
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("今日小时数: <simpleplaytime_daily_hours>", audienceResolver));

                        sender.sendMessage(Component.text("格式化变量:").color(NamedTextColor.GRAY));
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("总时间: <simpleplaytime_total_formatted>", audienceResolver));
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("今日时间: <simpleplaytime_daily_formatted>", audienceResolver));

                    } catch (Exception e) {
                        sender.sendMessage(Component.text("变量解析失败: " + e.getMessage()).color(NamedTextColor.RED));
                        e.printStackTrace();
                    }
                } else {
                    sender.sendMessage(Component.text("未找到玩家数据！请重新进入服务器。").color(NamedTextColor.RED));
                }
            } else {
                sender.sendMessage(Component.text("You do not have permission for this command.").color(NamedTextColor.RED));
            }
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("test")) {
            if (sender.hasPermission("simpleplaytime.reload")) {
                Player player = (Player) sender;
                PlayerData playerData = simplePlayTime.getPlayerData(player.getUniqueId());
                if (playerData != null) {
                    sender.sendMessage(Component.text("=== 变量测试 ===").color(NamedTextColor.YELLOW));
                    sender.sendMessage(Component.text("总秒数: " + playerData.getTotalPlaytimeWithSession()).color(NamedTextColor.GREEN));
                    sender.sendMessage(Component.text("今日秒数: " + playerData.getDailyPlaytimeWithSession()).color(NamedTextColor.GREEN));
                    sender.sendMessage(Component.text("总格式化: " + PlayerData.formatTime(playerData.getTotalPlaytimeWithSession())).color(NamedTextColor.GREEN));
                    sender.sendMessage(Component.text("今日格式化: " + PlayerData.formatTime(playerData.getDailyPlaytimeWithSession())).color(NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("未找到玩家数据！").color(NamedTextColor.RED));
                }
            } else {
                sender.sendMessage(Component.text("You do not have permission for this command.").color(NamedTextColor.RED));
            }
            return true;
        }

        Player player = (Player) sender;
        PlayerData playerData = simplePlayTime.getPlayerData(player.getUniqueId());

        if (playerData == null) {
            sender.sendMessage(Component.text("Player data not found! Please rejoin the server.").color(NamedTextColor.RED));
            return true;
        }

        // 获取总在线时间和每日在线时间（包含当前会话）
        long totalPlaytime = playerData.getTotalPlaytimeWithSession();
        long dailyPlaytime = playerData.getDailyPlaytimeWithSession();

        // 格式化时间
        String totalFormatted = PlayerData.formatTime(totalPlaytime);
        String dailyFormatted = PlayerData.formatTime(dailyPlaytime);

        // 发送消息
        String message = simplePlayTime.getConfig().getString("messages.playtime-command",
            "<green>Total: %total_formatted% | Today: %daily_formatted%</green>");
        message = message.replace("%total_formatted%", totalFormatted);
        message = message.replace("%daily_formatted%", dailyFormatted);

        sender.sendMessage(MiniMessage.miniMessage().deserialize(message));
        return true;
    }

}
