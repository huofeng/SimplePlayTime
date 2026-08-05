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

import java.time.LocalDate;
import java.util.UUID;

public class PlayerData {
    
    private final UUID uuid;
    private String playerName;
    private long totalPlaytime;
    private long dailyPlaytime;
    private LocalDate lastLoginDate;
    private long sessionStartTime;
    public PlayerData(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.totalPlaytime = 0L;
        this.dailyPlaytime = 0L;
        this.lastLoginDate = LocalDate.now();
        this.sessionStartTime = System.currentTimeMillis();
    }

    public PlayerData(UUID uuid, String playerName, long totalPlaytime, long dailyPlaytime, LocalDate lastLoginDate) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.totalPlaytime = totalPlaytime;
        this.dailyPlaytime = dailyPlaytime;
        this.lastLoginDate = lastLoginDate;
        this.sessionStartTime = System.currentTimeMillis();
    }

    // Getters
    public UUID getUuid() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getTotalPlaytime() {
        return totalPlaytime;
    }

    public long getDailyPlaytime() {
        return dailyPlaytime;
    }

    public LocalDate getLastLoginDate() {
        return lastLoginDate;
    }

    public long getSessionStartTime() {
        return sessionStartTime;
    }



    // Setters
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void setTotalPlaytime(long totalPlaytime) {
        this.totalPlaytime = totalPlaytime;
    }

    public void setDailyPlaytime(long dailyPlaytime) {
        this.dailyPlaytime = dailyPlaytime;
    }

    public void setLastLoginDate(LocalDate lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    public void setSessionStartTime(long sessionStartTime) {
        this.sessionStartTime = sessionStartTime;
    }



    // 添加总在线时间
    public void addTotalPlaytime(long seconds) {
        this.totalPlaytime += seconds;
    }

    // 添加每日在线时间
    public void addDailyPlaytime(long seconds) {
        this.dailyPlaytime += seconds;
    }



    // 重置每日时间（跨天时调用）
    public void resetDailyPlaytime() {
        this.dailyPlaytime = 0L;
        this.lastLoginDate = LocalDate.now();
    }

    // 获取当前会话总时长（秒）
    public long getCurrentSessionDuration() {
        return (System.currentTimeMillis() - sessionStartTime) / 1000;
    }

    // 获取总在线时间（包含当前会话）
    public long getTotalPlaytimeWithSession() {
        return totalPlaytime + getCurrentSessionDuration();
    }

    // 获取每日在线时间（包含当前会话）
    public long getDailyPlaytimeWithSession() {
        return dailyPlaytime + getCurrentSessionDuration();
    }

    // 格式化时间显示
    public static String formatTime(long totalSeconds) {
        if (totalSeconds <= 0) {
            return "0秒";
        }

        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("天");
        }
        if (hours > 0) {
            sb.append(hours).append("小时");
        }
        if (minutes > 0) {
            sb.append(minutes).append("分钟");
        }
        if (seconds > 0) {
            sb.append(seconds).append("秒");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "PlayerData{" +
                "uuid=" + uuid +
                ", playerName='" + playerName + '\'' +
                ", totalPlaytime=" + totalPlaytime +
                ", dailyPlaytime=" + dailyPlaytime +
                ", lastLoginDate=" + lastLoginDate +
                ", sessionStartTime=" + sessionStartTime +
                '}';
    }
}
