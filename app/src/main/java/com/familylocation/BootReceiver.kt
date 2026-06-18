package com.familylocation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Prefs.isEnabled(context)) {
            AlarmScheduler.schedule(context)
            val (t1, t2) = Prefs.getBothNextTriggers(context)
            val text = "${Prefs.nowText()}：手机重启后已恢复双定时，定时① $t1，定时② $t2；未访问定位"
            Prefs.setLastBootRestore(context, text)
            Prefs.setLastWake(context, text)
            Prefs.setLastStatus(context, "✅ 开机恢复成功：定时① $t1  /  定时② $t2")
        }
    }
}
