package com.familylocation

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * 定期自愈 Worker：每隔一段时间检查两个闹钟是否还在，
 * 如果丢失（被国产 ROM 清理）则自动重排。
 *
 * 触发条件：设备不在打盹、网络可用、电池非低电量。
 */
class AlarmHealWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        if (!Prefs.isEnabled(applicationContext)) return Result.success()

        val t1 = Prefs.getNextTriggerAt(applicationContext)
        val t2 = Prefs.getNextTriggerAt2(applicationContext)
        val now = System.currentTimeMillis()
        val needsReschedule = t1 <= 0L || t2 <= 0L || t1 < now - 5 * 60_000L || t2 < now - 5 * 60_000L

        if (needsReschedule) {
            AlarmScheduler.schedule(applicationContext)
            val (n1, n2) = Prefs.getBothNextTriggers(applicationContext)
            Prefs.setLastWake(applicationContext, "${Prefs.nowText()}：WorkManager 自愈：检测到闹钟丢失，已重排 → 定时① $n1 / 定时② $n2")
            Prefs.setLastStatus(applicationContext, "🩺 WorkManager 自愈：闹钟已恢复 → 定时① $n1  / 定时② $n2")
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "family_alarm_heal"
        private const val INTERVAL_MINUTES = 15L

        /** 启动定期自愈（如果未启动） */
        fun ensureScheduled(ctx: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<AlarmHealWorker>(
                INTERVAL_MINUTES, TimeUnit.MINUTES
            ).setConstraints(constraints)
                .setInitialDelay(INTERVAL_MINUTES, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        /** 取消定期自愈（关闭自动发送时调用） */
        fun cancel(ctx: Context) {
            WorkManager.getInstance(ctx).cancelUniqueWork(WORK_NAME)
        }
    }
}
