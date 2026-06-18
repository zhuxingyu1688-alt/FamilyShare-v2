package com.familylocation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmIndex = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_INDEX, 1)

        if (!Prefs.isEnabled(context)) {
            Prefs.setLastWake(context, "${Prefs.nowText()}：闹钟${alarmIndex}唤醒，但自动发送已关闭，未定位")
            return
        }

        val target = intent.getLongExtra(AlarmScheduler.EXTRA_TARGET_TIME, 0L)
        if (target <= 0L) {
            Prefs.setLastWake(context, "${Prefs.nowText()}：旧闹钟${alarmIndex}被唤醒，已跳过并重新安排所有，未定位")
            AlarmScheduler.schedule(context)
            return
        }

        val serviceIntent = Intent(context, LocationService::class.java).apply {
            action = LocationService.ACTION_SCHEDULED
            putExtra(AlarmScheduler.EXTRA_TARGET_TIME, target)
            putExtra(AlarmScheduler.EXTRA_ALARM_INDEX, alarmIndex)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // 只重排刚刚触发的那个闹钟，另一个不动
        AlarmScheduler.scheduleSpecific(context, alarmIndex)
    }
}
