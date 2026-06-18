package com.familylocation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Prefs.isEnabled(context)) {
            // 1. 恢复两个闹钟
            AlarmScheduler.schedule(context)

            // 2. 启动 WorkManager 定期自愈（国产 ROM 双保险）
            AlarmHealWorker.ensureScheduled(context)

            val (t1, t2) = Prefs.getBothNextTriggers(context)
            val text = "${Prefs.nowText()}：手机重启后已恢复双定时 + WorkManager 自愈，定时① $t1，定时② $t2"
            Prefs.setLastBootRestore(context, text)
            Prefs.setLastWake(context, text)
            Prefs.setLastStatus(context, "✅ 开机恢复成功：定时① $t1  /  定时② $t2  /  WorkManager 自愈已启动")
        }
    }
}
