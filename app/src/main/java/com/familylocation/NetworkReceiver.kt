package com.familylocation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/**
 * 监听网络变化：网络恢复后如果已开启则重排两个闹钟。
 * 解决断网后系统可能清理闹钟的问题。
 */
class NetworkReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!Prefs.isEnabled(context)) return
        if (!isNetworkAvailable(context)) return

        AlarmScheduler.schedule(context)
        Prefs.setLastStatus(context, "🌐 网络已恢复，已重排两个闹钟：${Prefs.getScheduleSummary(context)}")
        Prefs.setLastWake(context, "${Prefs.nowText()}：网络恢复，已重新安排闹钟")
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(nw) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            return cm.activeNetworkInfo?.isConnectedOrConnecting == true
        }
    }
}
