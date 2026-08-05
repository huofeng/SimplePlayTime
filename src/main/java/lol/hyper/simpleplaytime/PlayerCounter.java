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

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lol.hyper.simpleplaytime.database.PlayerData;

import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Consumer;

public class PlayerCounter implements Consumer<ScheduledTask> {

    private final UUID player;
    private final SimplePlayTime simplePlayTime;
    private ScheduledTask task;

    public PlayerCounter(UUID player, SimplePlayTime simplePlayTime) {
        this.player = player;
        this.simplePlayTime = simplePlayTime;
    }

    @Override
    public void accept(ScheduledTask task) {
        this.task = task;

        PlayerData playerData = simplePlayTime.getPlayerData(player);
        if (playerData == null) {
            return;
        }

        // 检查是否跨天
        LocalDate today = LocalDate.now();
        if (!playerData.getLastLoginDate().equals(today)) {
            // 计算昨天的实际在线时间（包含会话时间）
            long yesterdayPlaytime = playerData.getDailyPlaytime() + playerData.getCurrentSessionDuration();

            // 保存昨天的数据到数据库
            if (simplePlayTime.getDatabaseManager() != null) {
                simplePlayTime.getDatabaseManager().updateDailyPlaytime(
                    playerData.getUuid(),
                    playerData.getLastLoginDate(),
                    yesterdayPlaytime
                );
            }

            // 更新总时间并重置每日时间
            playerData.setTotalPlaytime(playerData.getTotalPlaytimeWithSession());
            playerData.resetDailyPlaytime();
            playerData.setSessionStartTime(System.currentTimeMillis()); // 重置会话开始时间
        }
    }

    public void cancel() {
        if (task != null) {
            this.task.cancel();
        }
    }
}
