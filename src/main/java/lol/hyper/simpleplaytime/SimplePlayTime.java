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

package lol.hyper.simpleplaytime;

import lol.hyper.simpleplaytime.api.PlayTimeExpansion;
import lol.hyper.simpleplaytime.api.MiniPlaceholdersExpansion;
import lol.hyper.simpleplaytime.command.PlayTimeCommand;
import lol.hyper.simpleplaytime.database.DatabaseManager;
import lol.hyper.simpleplaytime.database.PlayerData;
import lol.hyper.simpleplaytime.events.PlayerLeaveJoin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public final class SimplePlayTime extends JavaPlugin {

    private final Map<UUID, PlayerCounter> playerCounters = new HashMap<>();
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    public final Logger logger = this.getLogger();
    public final File configFile = new File(this.getDataFolder(), "config.yml");
    public FileConfiguration config;
    public final NamespacedKey playtimeKey = new NamespacedKey(this, "playtime");

    private DatabaseManager databaseManager;
    private PlayTimeExpansion placeholderExpansion;
    private MiniPlaceholdersExpansion miniPlaceholdersExpansion;

    @Override
    public void onEnable() {
        try {
            loadConfig();

            // 初始化数据库
            if (config.getBoolean("database.enabled", true)) {
                databaseManager = new DatabaseManager(this);
                if (!databaseManager.initialize()) {
                    logger.warning("Failed to initialize database! Running in fallback mode with PersistentDataContainer...");
                    databaseManager = null; // ？
                }
            } else {
                logger.info("Database disabled in config. Using PersistentDataContainer for storage.");
            }

            // 注册事件监听器
            Bukkit.getPluginManager().registerEvents(new PlayerLeaveJoin(this), this);

            // 注册命令
            this.getCommand("playtime").setExecutor(new PlayTimeCommand(this));

            // 注册变量扩展
            registerPlaceholderExpansions();

            // 启动自动保存任务
            startAutoSaveTask();

            new Metrics(this, 13941);

            logger.info("SimplePlayTime v" + getDescription().getVersion() + " enabled successfully!");

        } catch (Exception e) {
            logger.severe("Failed to enable SimplePlayTime: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // 保存所有玩家数据
        saveAllPlayerData();

        // 关闭数据库连接
        if (databaseManager != null) {
            databaseManager.close();
        }

        // 取消注册PlaceholderAPI扩展
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
        }
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            this.saveResource("config.yml", true);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        int CONFIG_VERSION = 3;
        if (config.getInt("config-version") != CONFIG_VERSION) {
            logger.warning("Your configuration is out of date! Some features may not work!");
        }
    }



    /**
     * Gets a message from messages.yml.
     *
     * @param path The path to the message.
     * @return Component with formatting applied.
     */
    public Component getMessage(String path) {
        String message = config.getString(path);
        if (message == null) {
            logger.warning(path + " is not a valid message!");
            return Component.text("Invalid path! " + path).color(NamedTextColor.RED);
        }
        return MiniMessage.miniMessage().deserialize(message);
    }

    // 注册变量扩展
    private void registerPlaceholderExpansions() {
        boolean hasPlaceholderAPI = false;
        boolean hasMiniPlaceholders = false;

        logger.info("开始注册变量扩展...");

        // 尝试注册 PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            logger.info("检测到 PlaceholderAPI，尝试注册扩展...");
            try {
                placeholderExpansion = new PlayTimeExpansion(this);
                placeholderExpansion.register();
                logger.info("PlaceholderAPI expansion registered!");
                hasPlaceholderAPI = true;
            } catch (Exception e) {
                logger.warning("Failed to register PlaceholderAPI expansion: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            logger.info("未检测到 PlaceholderAPI");
        }

        // 尝试注册 MiniPlaceholders
        if (Bukkit.getPluginManager().getPlugin("MiniPlaceholders") != null) {
            logger.info("检测到 MiniPlaceholders，尝试注册扩展...");
            try {
                miniPlaceholdersExpansion = new MiniPlaceholdersExpansion(this);
                var expansion = miniPlaceholdersExpansion.createExpansion();
                logger.info("MiniPlaceholders expansion created: " + expansion.getClass().getName());
                expansion.register();
                logger.info("MiniPlaceholders expansion registered successfully!");
                hasMiniPlaceholders = true;
            } catch (Exception e) {
                logger.warning("Failed to register MiniPlaceholders expansion: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            logger.info("未检测到 MiniPlaceholders");
        }

        // 如果都没有找到，给出警告
        if (!hasPlaceholderAPI && !hasMiniPlaceholders) {
            logger.warning("Neither PlaceholderAPI nor MiniPlaceholders found! Variables will not work.");
            logger.info("For Folia servers, please use MiniPlaceholders instead of PlaceholderAPI.");
        }
    }

    // 获取数据库管理器
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    // 获取玩家数据
    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.get(uuid);
    }

    // 添加玩家数据
    public void addPlayerData(UUID uuid, PlayerData playerData) {
        playerDataMap.put(uuid, playerData);
    }

    // 移除玩家数据
    public void removePlayerData(UUID uuid) {
        playerDataMap.remove(uuid);
    }

    // 获取玩家计数器
    public PlayerCounter getPlayerCounter(UUID uuid) {
        return playerCounters.get(uuid);
    }

    // 添加玩家计数器
    public void addPlayerCounter(UUID uuid, PlayerCounter counter) {
        playerCounters.put(uuid, counter);
    }

    // 移除玩家计数器
    public void removePlayerCounter(UUID uuid) {
        PlayerCounter counter = playerCounters.remove(uuid);
        if (counter != null) {
            counter.cancel();
        }
    }

    // 启动自动保存任务
    private void startAutoSaveTask() {
        int saveInterval = config.getInt("data-sync.save-interval", 300);
        if (saveInterval > 0) {
            try {
                // 尝试使用 Folia 兼容的调度方式
                if (isFolia()) {
                    // 在 Folia 上使用全局区域调度器
                    Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, (task) -> {
                        try {
                            saveAllPlayerData();
                        } catch (Exception e) {
                            logger.warning("Error during auto-save: " + e.getMessage());
                        }
                    }, saveInterval * 20L, saveInterval * 20L);
                } else {
                    // 在 Paper 上使用传统的异步调度器
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            try {
                                saveAllPlayerData();
                            } catch (Exception e) {
                                logger.warning("Error during auto-save: " + e.getMessage());
                            }
                        }
                    }.runTaskTimerAsynchronously(this, saveInterval * 20L, saveInterval * 20L);
                }
                logger.info("Auto-save task started with interval: " + saveInterval + " seconds");
            } catch (Exception e) {
                logger.warning("Failed to start auto-save task: " + e.getMessage());
                logger.info("Auto-save disabled due to scheduling error");
            }
        } else {
            logger.info("Auto-save disabled (interval = 0)");
        }
    }

    // 检测是否为 Folia 服务器
    private boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    // 保存所有玩家数据
    public void saveAllPlayerData() {
        if (databaseManager == null) return;

        for (PlayerData playerData : playerDataMap.values()) {
            // 检查是否跨天
            LocalDate today = LocalDate.now();
            if (!playerData.getLastLoginDate().equals(today)) {
                // 计算昨天的实际在线时间（包含会话时间）
                long yesterdayPlaytime = playerData.getDailyPlaytime() + playerData.getCurrentSessionDuration();

                // 保存昨天的数据
                databaseManager.updateDailyPlaytime(playerData.getUuid(), playerData.getLastLoginDate(), yesterdayPlaytime);

                // 更新总时间并重置每日时间
                playerData.setTotalPlaytime(playerData.getTotalPlaytimeWithSession());
                playerData.resetDailyPlaytime();
                playerData.setSessionStartTime(System.currentTimeMillis()); // 重置会话开始时间
            }

            // 保存当前数据到数据库（包含实时会话时间）
            long currentTotalPlaytime = playerData.getTotalPlaytimeWithSession();
            long currentDailyPlaytime = playerData.getDailyPlaytimeWithSession();

            databaseManager.updateTotalPlaytime(playerData.getUuid(), playerData.getPlayerName(), currentTotalPlaytime);
            databaseManager.updateDailyPlaytime(playerData.getUuid(), today, currentDailyPlaytime);
        }
    }
}
