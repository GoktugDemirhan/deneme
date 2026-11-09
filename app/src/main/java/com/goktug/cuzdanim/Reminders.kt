package com.goktug.cuzdanim

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import org.json.JSONArray
import java.util.Calendar

/** Yapılacaklar hatırlatıcılarını telefonun alarm sistemine kurar. */
object Reminders {
    const val CHANNEL_ID = "hatirlatici"
    const val ACTION_REMIND = "com.goktug.cuzdanim.REMIND"
    private const val PREFS = "reminders"
    private const val KEY = "list"

    private fun load(ctx: Context): JSONArray =
        runCatching { JSONArray(ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]")) }
            .getOrDefault(JSONArray())

    /** Eski alarmları iptal eder, verilen listeyi (null ise kayıtlı listeyi) yeniden kurar. */
    fun reschedule(ctx: Context, newJson: String?) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val old = load(ctx)
        for (i in 0 until old.length()) {
            val id = old.optJSONObject(i)?.optString("id") ?: continue
            am.cancel(pendingIntent(ctx, id, null, null))
        }
        if (newJson != null) {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, newJson).apply()
        }
        val list = load(ctx)
        val now = System.currentTimeMillis()
        for (i in 0 until list.length()) {
            val o = list.optJSONObject(i) ?: continue
            val time = nextTime(o.optLong("time", 0L), o.optString("repeat", ""), now)
            if (time <= now) continue
            val pi = pendingIntent(ctx, o.optString("id"), o.optString("title"), o.optString("note"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi)
            }
        }
    }

    private val FIXED = mapOf("15m" to 900_000L, "30m" to 1_800_000L, "1h" to 3_600_000L, "3h" to 10_800_000L)

    /** k. tekrarın zamanı (k = 0 ilk hatırlatma). Aylıkta ayın son gününe sabitlenir. */
    private fun occAt(base: Long, repeat: String, k: Int): Long {
        FIXED[repeat]?.let { return base + it * k }
        val c = Calendar.getInstance().apply { timeInMillis = base }
        when (repeat) {
            "1d" -> c.add(Calendar.DAY_OF_YEAR, k)
            "1w" -> c.add(Calendar.DAY_OF_YEAR, 7 * k)
            "1M" -> {
                val day = c.get(Calendar.DAY_OF_MONTH)
                c.set(Calendar.DAY_OF_MONTH, 1)
                c.add(Calendar.MONTH, k)
                c.set(Calendar.DAY_OF_MONTH, minOf(day, c.getActualMaximum(Calendar.DAY_OF_MONTH)))
            }
            else -> return base
        }
        return c.timeInMillis
    }

    /** Tekrarlı hatırlatıcıda now'dan sonraki ilk zaman; tekrarsızda kendisi. */
    fun nextTime(base: Long, repeat: String, now: Long): Long {
        if (base <= 0L) return 0L
        val approx = FIXED[repeat] ?: when (repeat) {
            "1d" -> 86_400_000L
            "1w" -> 604_800_000L
            "1M" -> 2_592_000_000L
            else -> return base
        }
        if (base > now) return base
        var k = maxOf(0L, (now - base) / approx - 1).toInt()
        var guard = 0
        while (occAt(base, repeat, k) <= now && guard < 10_000) { k++; guard++ }
        return occAt(base, repeat, k)
    }

    private fun pendingIntent(ctx: Context, id: String, title: String?, note: String?): PendingIntent {
        val intent = Intent(ctx, ReminderReceiver::class.java)
            .setAction(ACTION_REMIND)
            .setData(Uri.parse("cuzdanim://hatirlatici/" + Uri.encode(id)))
        if (title != null) intent.putExtra("id", id).putExtra("title", title).putExtra("note", note ?: "")
        return PendingIntent.getBroadcast(
            ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(CHANNEL_ID, "Hatırlatıcılar", NotificationManager.IMPORTANCE_HIGH)
                ch.description = "Yapılacaklar listesi hatırlatıcıları"
                ch.enableVibration(true)
                nm.createNotificationChannel(ch)
            }
        }
    }
}
