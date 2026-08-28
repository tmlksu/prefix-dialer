package io.github.tmlksu.prefixdialer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import android.util.Log

/**
 * 発信後、CallLog に載った「0063…」の新規エントリを検知し、
 * NUMBER と CACHED_NAME を元番号ベースに書き換える短命フォアグラウンドサービス。
 *
 * One UI のバックグラウンド制限で殺されないよう、shortService として起動する。
 * 一定時間（または全対応を消化）で自己終了する。
 */
class CallLogRewriteService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var observer: ContentObserver? = null

    private val stopRunnable = Runnable { stopSelfSafely() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat()

        if (observer == null) {
            val obs = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    tryRewriteRecent()
                    if (PendingRewrites.isEmpty()) stopSelfSafely()
                }
            }
            observer = obs
            contentResolver.registerContentObserver(
                CallLog.Calls.CONTENT_URI, true, obs,
            )
        }

        // 監視開始直後に、すでに載っている可能性もあるので一度走らせる
        tryRewriteRecent()

        // フォールバックの自己終了（shortService の上限内）
        handler.removeCallbacks(stopRunnable)
        handler.postDelayed(stopRunnable, WINDOW_MS)
        return START_NOT_STICKY
    }

    /** 直近の発信履歴を見て、対応があれば元番号に書き換える。 */
    private fun tryRewriteRecent() {
        PendingRewrites.purgeOld(System.currentTimeMillis())
        if (PendingRewrites.isEmpty()) return

        val projection = arrayOf(CallLog.Calls._ID, CallLog.Calls.NUMBER)
        try {
            contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                "${CallLog.Calls.TYPE} = ?",
                arrayOf(CallLog.Calls.OUTGOING_TYPE.toString()),
                "${CallLog.Calls.DATE} DESC LIMIT $SCAN_ROWS",
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(CallLog.Calls._ID)
                val numCol = c.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                while (c.moveToNext()) {
                    val logged = c.getString(numCol) ?: continue
                    val original = PendingRewrites.consumeFor(logged) ?: continue
                    rewriteRow(c.getLong(idCol), original)
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "call log permissions are missing; cannot rewrite", e)
        } catch (e: Exception) {
            Log.w(TAG, "call log scan failed", e)
        }
    }

    private fun rewriteRow(id: Long, original: String) {
        val values = ContentValues().apply {
            put(CallLog.Calls.NUMBER, original)
            lookupContactName(this@CallLogRewriteService, original)?.let {
                put(CallLog.Calls.CACHED_NAME, it)
            }
        }
        val updated = contentResolver.update(
            CallLog.Calls.CONTENT_URI, values,
            "${CallLog.Calls._ID} = ?", arrayOf(id.toString()),
        )
        Log.d(TAG, "rewrote row $id -> $original (updated=$updated)")
    }

    private fun stopSelfSafely() {
        handler.removeCallbacks(stopRunnable)
        observer?.let { contentResolver.unregisterContentObserver(it) }
        observer = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        observer?.let { contentResolver.unregisterContentObserver(it) }
        observer = null
        handler.removeCallbacks(stopRunnable)
        super.onDestroy()
    }

    private fun startForegroundCompat() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID, getString(R.string.notification_channel_call_log),
                    NotificationManager.IMPORTANCE_MIN,
                ),
            )
        }
        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(getString(R.string.notification_call_log_title))
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIF_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE,
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    companion object {
        private const val TAG = "CallLogRewrite"
        private const val CHANNEL_ID = "call_log_rewrite"
        private const val NOTIF_ID = 42
        private const val WINDOW_MS = 15_000L
        private const val SCAN_ROWS = 5

        /** 元番号から連絡先の表示名を引く（無ければ null）。 */
        private fun lookupContactName(context: Context, number: String): String? {
            return try {
                val uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(number),
                )
                context.contentResolver.query(
                    uri,
                    arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                    null, null, null,
                )?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
