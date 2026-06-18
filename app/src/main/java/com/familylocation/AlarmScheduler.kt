package com.familylocation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object AlarmScheduler {
    const val REQUEST_CODE_1 = 2001
    const val REQUEST_CODE_2 = 2002
    const val ACTION_DAILY_ALARM = "com.familylocation.ACTION_DAILY_ALARM"
    const val EXTRA_TARGET_TIME = "target_time_ms"
    const val EXTRA_ALARM_INDEX = "alarm_index"

    // ──────────────── 公开方法 ────────────────

    /** 安排两个定时闹钟 */
    fun schedule(ctx: Context) {
        cancelAllInternal(ctx)

        val h1 = Prefs.getSendHour(ctx)
        val m1 = Prefs.getSendMinute(ctx)
        val t1 = computeNextTrigger(h1, m1, 0)
        setBestAlarm(ctx, t1, REQUEST_CODE_1, 1)
        Prefs.setNextTriggerAt(ctx, t1)

        val h2 = Prefs.getSend2Hour(ctx)
        val m2 = Prefs.getSend2Minute(ctx)
        if (h1 == h2 && m1 == m2) {
            Prefs.setNextTriggerAt2(ctx, t1)
            return
        }
        val t2 = computeNextTrigger(h2, m2, 0)
        setBestAlarm(ctx, t2, REQUEST_CODE_2, 2)
        Prefs.setNextTriggerAt2(ctx, t2)
    }

    /** 仅重排某个闹钟为下一天同一时间 */
    fun scheduleSpecific(ctx: Context, alarmIndex: Int) {
        val hour = if (alarmIndex == 1) Prefs.getSendHour(ctx) else Prefs.getSend2Hour(ctx)
        val minute = if (alarmIndex == 1) Prefs.getSendMinute(ctx) else Prefs.getSend2Minute(ctx)
        val requestCode = if (alarmIndex == 1) REQUEST_CODE_1 else REQUEST_CODE_2
        val triggerAt = computeNextTrigger(hour, minute, 1)

        cancelOne(ctx, requestCode)
        setBestAlarm(ctx, triggerAt, requestCode, alarmIndex)

        if (alarmIndex == 1) {
            Prefs.setNextTriggerAt(ctx, triggerAt)
        } else {
            Prefs.setNextTriggerAt2(ctx, triggerAt)
        }
    }

    /** 今日暂停：两个都推到明天 */
    fun rescheduleBothForTomorrow(ctx: Context) {
        cancelAllInternal(ctx)

        val h1 = Prefs.getSendHour(ctx)
        val m1 = Prefs.getSendMinute(ctx)
        val t1 = computeNextTrigger(h1, m1, 1)
        setBestAlarm(ctx, t1, REQUEST_CODE_1, 1)
        Prefs.setNextTriggerAt(ctx, t1)

        val h2 = Prefs.getSend2Hour(ctx)
        val m2 = Prefs.getSend2Minute(ctx)
        if (h1 == h2 && m1 == m2) {
            Prefs.setNextTriggerAt2(ctx, t1)
            return
        }
        val t2 = computeNextTrigger(h2, m2, 1)
        setBestAlarm(ctx, t2, REQUEST_CODE_2, 2)
        Prefs.setNextTriggerAt2(ctx, t2)
    }

    /** 取消所有闹钟 */
    fun cancelAll(ctx: Context) {
        cancelAllInternal(ctx)
        Prefs.setNextTriggerAt(ctx, 0L)
        Prefs.setNextTriggerAt2(ctx, 0L)
    }

    // ──────────────── 内部 ────────────────

    private fun cancelAllInternal(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(ctx, 0L, REQUEST_CODE_1, 1))
        am.cancel(pendingIntent(ctx, 0L, REQUEST_CODE_2, 2))
    }

    private fun cancelOne(ctx: Context, requestCode: Int) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val idx = if (requestCode == REQUEST_CODE_1) 1 else 2
        am.cancel(pendingIntent(ctx, 0L, requestCode, idx))
    }

    /**
     * 选择最优闹钟方式：
     *  1) AlarmClock → 国产 ROM 最高优先级，穿透省电限制
     *  2) setExactAndAllowWhileIdle → 标准精确闹钟
     *  3) setAndAllowWhileIdle → 无精确权限时的兜底
     */
    private fun setBestAlarm(ctx: Context, triggerAtMs: Long, requestCode: Int, alarmIndex: Int) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(ctx, triggerAtMs, requestCode, alarmIndex)

        // 优先 AlarmClock —— 国产 ROM（华为/荣耀/小米/OPPO/vivo）对此有特殊豁免
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val info = AlarmManager.AlarmClockInfo(triggerAtMs, clockIconIntent(ctx))
                am.setAlarmClock(info, pi)
                return
            } catch (_: SecurityException) {
                // 少数设备可能拒绝 AlarmClock，降级
            }
        }

        // 降级：精确闹钟
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
            try { am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi); return }
            catch (_: SecurityException) {}
        }

        // 最终兜底
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
    }

    /** 状态栏闹钟图标 PendingIntent（不会真正打开任何 Activity，仅用于系统感知） */
    private fun clockIconIntent(ctx: Context): PendingIntent {
        val intent = Intent(ctx, AlarmReceiver::class.java).apply {
            action = ACTION_DAILY_ALARM
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getBroadcast(
            ctx, 9999, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun pendingIntent(ctx: Context, targetMs: Long, requestCode: Int, alarmIndex: Int): PendingIntent {
        val intent = Intent(ctx, AlarmReceiver::class.java).apply {
            action = ACTION_DAILY_ALARM
            putExtra(EXTRA_ALARM_INDEX, alarmIndex)
            if (targetMs > 0L) putExtra(EXTRA_TARGET_TIME, targetMs)
        }
        return PendingIntent.getBroadcast(
            ctx, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun computeNextTrigger(hour: Int, minute: Int, addDays: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            if (addDays > 0) add(Calendar.DAY_OF_YEAR, addDays)
        }
        if (addDays == 0 && cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}
