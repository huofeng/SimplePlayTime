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

package lol.hyper.simpleplaytime.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lol.hyper.simpleplaytime.SimplePlayTime;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final SimplePlayTime plugin;
    private HikariDataSource dataSource;
    private String tablePrefix;

    public DatabaseManager(SimplePlayTime plugin) {
        this.plugin = plugin;
        this.tablePrefix = plugin.getConfig().getString("database.table-prefix", "spt_");
    }

    public boolean initialize() {
        try {
            plugin.getLogger().info("Initializing database...");
            setupDataSource();
            plugin.getLogger().info("Database connection established.");
            createTables();
            plugin.getLogger().info("Database tables created/verified.");

            // 测试连接
            try (Connection conn = getConnection()) {
                plugin.getLogger().info("Database connection test successful.");
            }

            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database: " + e.getMessage(), e);
            return false;
        }
    }

    private void setupDataSource() throws Exception {
        HikariConfig config = new HikariConfig();
        String dbType = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();

        plugin.getLogger().info("Setting up " + dbType.toUpperCase() + " database...");

        if (dbType.equals("mysql")) {
            String host = plugin.getConfig().getString("database.host", "localhost");
            int port = plugin.getConfig().getInt("database.port", 3306);
            String database = plugin.getConfig().getString("database.database", "minecraft");
            String username = plugin.getConfig().getString("database.username", "root");
            String password = plugin.getConfig().getString("database.password", "password");

            // 首先尝试创建数据库（如果不存在）
            try {
                createDatabaseIfNotExists(host, port, database, username, password);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to create database '" + database + "': " + e.getMessage());
                plugin.getLogger().warning("Please ensure the database exists or the user has CREATE privileges.");
            }

            String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC&autoReconnect=true&allowPublicKeyRetrieval=true";
            plugin.getLogger().info("MySQL JDBC URL: " + jdbcUrl.replace(password, "***"));

            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            // SQLite
            String fileName = plugin.getConfig().getString("database.file", "playtime.db");
            File dbFile = new File(plugin.getDataFolder(), fileName);

            // 确保数据文件夹存在
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            String jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            plugin.getLogger().info("SQLite database file: " + dbFile.getAbsolutePath());

            config.setJdbcUrl(jdbcUrl);
            config.setDriverClassName("org.sqlite.JDBC");
        }

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        this.dataSource = new HikariDataSource(config);
    }

    private void createDatabaseIfNotExists(String host, int port, String database, String username, String password) throws Exception {
        // 连接到MySQL服务器（不指定数据库）
        String serverUrl = "jdbc:mysql://" + host + ":" + port + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

        HikariConfig tempConfig = new HikariConfig();
        tempConfig.setJdbcUrl(serverUrl);
        tempConfig.setUsername(username);
        tempConfig.setPassword(password);
        tempConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        tempConfig.setMaximumPoolSize(1);
        tempConfig.setConnectionTimeout(10000);

        try (HikariDataSource tempDataSource = new HikariDataSource(tempConfig);
             Connection conn = tempDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("CREATE DATABASE IF NOT EXISTS `" + database + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")) {

            plugin.getLogger().info("Checking if database '" + database + "' exists...");
            stmt.executeUpdate();
            plugin.getLogger().info("Database '" + database + "' is ready.");
        }
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection()) {
            // 创建玩家基础信息表
            String createPlayerTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "player_playtime (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "player_name VARCHAR(16), " +
                    "total_playtime BIGINT DEFAULT 0, " +
                    "last_login_date DATE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";

            // 创建每日在线时间表
            String createDailyTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "daily_playtime (" +
                    "id INTEGER PRIMARY KEY " + (isMySQL() ? "AUTO_INCREMENT" : "AUTOINCREMENT") + ", " +
                    "uuid VARCHAR(36), " +
                    "date DATE, " +
                    "daily_playtime BIGINT DEFAULT 0, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE(uuid, date)" +
                    ")";

            try (PreparedStatement stmt1 = conn.prepareStatement(createPlayerTable);
                 PreparedStatement stmt2 = conn.prepareStatement(createDailyTable)) {
                stmt1.executeUpdate();
                stmt2.executeUpdate();
            }
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private boolean isMySQL() {
        return plugin.getConfig().getString("database.type", "sqlite").equalsIgnoreCase("mysql");
    }

    // 获取玩家总在线时间
    public long getTotalPlaytime(UUID uuid) {
        String sql = "SELECT total_playtime FROM " + tablePrefix + "player_playtime WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("total_playtime");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting total playtime for " + uuid, e);
        }
        return 0L;
    }

    // 获取玩家每日在线时间
    public long getDailyPlaytime(UUID uuid, LocalDate date) {
        String sql = "SELECT daily_playtime FROM " + tablePrefix + "daily_playtime WHERE uuid = ? AND date = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, date.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("daily_playtime");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting daily playtime for " + uuid + " on " + date, e);
        }
        return 0L;
    }

    // 更新玩家总在线时间
    public void updateTotalPlaytime(UUID uuid, String playerName, long totalPlaytime) {
        String sql = "INSERT INTO " + tablePrefix + "player_playtime (uuid, player_name, total_playtime, last_login_date, updated_at) " +
                "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                "ON " + (isMySQL() ? "DUPLICATE KEY UPDATE" : "CONFLICT(uuid) DO UPDATE SET") +
                " player_name = ?, total_playtime = ?, last_login_date = ?, updated_at = CURRENT_TIMESTAMP";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            LocalDate today = LocalDate.now();
            stmt.setString(1, uuid.toString());
            stmt.setString(2, playerName);
            stmt.setLong(3, totalPlaytime);
            stmt.setString(4, today.toString());
            stmt.setString(5, playerName);
            stmt.setLong(6, totalPlaytime);
            stmt.setString(7, today.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error updating total playtime for " + uuid, e);
        }
    }

    // 更新玩家每日在线时间
    public void updateDailyPlaytime(UUID uuid, LocalDate date, long dailyPlaytime) {
        String sql = "INSERT INTO " + tablePrefix + "daily_playtime (uuid, date, daily_playtime, updated_at) " +
                "VALUES (?, ?, ?, CURRENT_TIMESTAMP) " +
                "ON " + (isMySQL() ? "DUPLICATE KEY UPDATE" : "CONFLICT(uuid, date) DO UPDATE SET") +
                " daily_playtime = ?, updated_at = CURRENT_TIMESTAMP";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, date.toString());
            stmt.setLong(3, dailyPlaytime);
            stmt.setLong(4, dailyPlaytime);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error updating daily playtime for " + uuid + " on " + date, e);
        }
    }

    // 获取表前缀
    public String getTablePrefix() {
        return tablePrefix;
    }
}
