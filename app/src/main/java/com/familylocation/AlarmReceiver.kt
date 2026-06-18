package com.familylocation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // ── 关键：获取临时 WakeLock，防止系统在启动服务前休眠 ──
        val wl = try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "FamilyShare:alarm_receiver"
            ).apply {
                setReferenceCounted(false)
                acquire(30_000L) // 30 秒足够启动服务 + 创建通知
            }
        } catch (_: Exception) { null }

        // ── goAsync 延长 BroadcastReceiver 生命周期 ──
        val pending = goAsync()

        try {
            val alarmIndex = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_INDEX, 1)

            if (!Prefs.isEnabled(context)) {
                Prefs.setLastWake(context, "${Prefs.nowText()}：闹钟${alarmIndex}唤醒，但自动发送已关闭")
                return
            }

            val target = intent.getLongExtra(AlarmScheduler.EXTRA_TARGET_TIME, 0L)
            if (target <= 0L) {
                Prefs.setLastWake(context, "${Prefs.nowText()}：旧闹钟${alarmIndex}被唤醒，已跳过并重排")
                AlarmScheduler.schedule(context)
                return
            }

            val serviceIntent = Intent(context, LocationService::class.java).apply {
                action = LocationService.ACTION_SCHEDULED
                putExtra(AlarmScheduler.EXTRA_TARGET_TIME, target)
                putExtra(AlarmScheduler.EXTRA_ALARM_INDEX, alarmIndex)
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                // Android 12+ 后台启动前台服务限制 → 尝试普通启动
                Prefs.setLastWake(context, "${Prefs.nowText()}：闹钟${alarmIndex}前台服务启动受限(${e.message})，尝试普通启动")
                try {
                    context.startService(serviceIntent)
                } catch (e2: Exception) {
                    Prefs.setLastWake(context, "${Prefs.nowText()}：闹钟${alarmIndex}启动彻底失败：${e2.message}")
                }
            }

            // 只重排刚触发的那个闹钟
            AlarmScheduler.scheduleSpecific(context, alarmIndex)

        } finally {
            // 释放 WakeLock
            try { if (wl?.isHeld == true) wl.release() } catch (_: Exception) {}
            // 通知系统广播处理完成
            pending.finish()
        }
    }
}
