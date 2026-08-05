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

package lol.hyper.simpleplaytime.events;

import lol.hyper.simpleplaytime.PlayerCounter;
import lol.hyper.simpleplaytime.SimplePlayTime;
import lol.hyper.simpleplaytime.database.DatabaseManager;
import lol.hyper.simpleplaytime.database.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.LocalDate;
import java.util.UUID;

public class PlayerLeaveJoin implements Listener {

    private final SimplePlayTime simplePlayTime;

    public PlayerLeaveJoin(SimplePlayTime simplePlayTime) {
        this.simplePlayTime = simplePlayTime;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();

        // database load
        PlayerData playerData = loadPlayerData(uuid, playerName);
        simplePlayTime.addPlayerData(uuid, playerData);

        // start playtime-开始计时
        PlayerCounter counter = new PlayerCounter(uuid, simplePlayTime);
        player.getScheduler().runAtFixedRate(simplePlayTime, counter, null, 1, 20);
        simplePlayTime.addPlayerCounter(uuid, counter);

        // 发送欢迎消息
        if (playerData.getTotalPlaytime() == 0) {
            String startMessage = simplePlayTime.getConfig().getString("messages.playtime-start", "");
            if (!startMessage.isEmpty()) {
                player.sendMessage(simplePlayTime.getMessage("messages.playtime-start"));
            }
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        PlayerData playerData = simplePlayTime.getPlayerData(uuid);
        if (playerData == null) {
            simplePlayTime.logger.warning("No player data found for " + player.getName() + " on quit!");
            return;
        }

        // 保存玩家数据到数据库
        savePlayerData(playerData);

        // 停止计时器并清理数据
        simplePlayTime.removePlayerCounter(uuid);
        simplePlayTime.removePlayerData(uuid);
    }

    // 从数据库或PersistentDataContainer加载玩家数据
    private PlayerData loadPlayerData(UUID uuid, String playerName) {
        DatabaseManager dbManager = simplePlayTime.getDatabaseManager();

        if (dbManager != null) {
            // 从数据库加载
            long totalPlaytime = dbManager.getTotalPlaytime(uuid);
            LocalDate today = LocalDate.now();
            long dailyPlaytime = dbManager.getDailyPlaytime(uuid, today);
            return new PlayerData(uuid, playerName, totalPlaytime, dailyPlaytime, today);
        } else {
            // 从PersistentDataContainer加载（后备模式）
            return new PlayerData(uuid, playerName);
        }
    }

    // 保存玩家数据到数据库或PersistentDataContainer
    private void savePlayerData(PlayerData playerData) {
        DatabaseManager dbManager = simplePlayTime.getDatabaseManager();

        // 计算最终的在线时间（包含当前会话）
        long finalTotalPlaytime = playerData.getTotalPlaytimeWithSession();
        long finalDailyPlaytime = playerData.getDailyPlaytimeWithSession();

        if (dbManager != null) {
            // 保存到数据库
            LocalDate today = LocalDate.now();
            dbManager.updateTotalPlaytime(playerData.getUuid(), playerData.getPlayerName(), finalTotalPlaytime);
            dbManager.updateDailyPlaytime(playerData.getUuid(), today, finalDailyPlaytime);
        } else {
            // 保存到PersistentDataContainer（后备模式）
            // 注意：PersistentDataContainer只能保存总时间，不支持每日时间
            simplePlayTime.logger.info("Saving playtime for " + playerData.getPlayerName() + " to PersistentDataContainer: " + finalTotalPlaytime + " seconds");
        }
    }


}
