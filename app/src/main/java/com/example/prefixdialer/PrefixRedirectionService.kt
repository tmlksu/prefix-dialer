package com.example.prefixdialer

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.telecom.CallRedirectionService
import android.telecom.PhoneAccountHandle
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * 発信直前に呼ばれ、設定に従って番号を書き換えてから発信し直す。
 *
 * このコールバックの応答が遅れると発信そのものが遅れるため、重い処理はしない。
 * 設定は [SettingsStore] がメモリに保持しているので、毎回の読み出しは I/O にならない。
 */
class PrefixRedirectionService : CallRedirectionService() {

    private val settingsStore: SettingsStore by lazy { SettingsStore(this) }

    override fun onPlaceCall(
        handle: Uri,
        initialPhoneAccount: PhoneAccountHandle,
        allowInteractiveResponse: Boolean,
    ) {
        val original = handle.schemeSpecificPart
        val dial = RuleEngine.buildDialNumber(original, settingsStore.load())

        if (dial == null) {
            placeCallUnmodified()
            return
        }

        Log.d(TAG, "redirect: $original -> $dial")

        // 履歴を元番号へ戻すための対応を控え、監視サービスを起動しておく
        PendingRewrites.add(dial, original, System.currentTimeMillis())
        runCatching {
            ContextCompat.startForegroundService(
                this,
                Intent(this, CallLogRewriteService::class.java),
            )
        }.onFailure { Log.w(TAG, "could not start rewrite service", it) }

        redirectCall(Uri.fromParts("tel", dial, null), initialPhoneAccount, false)
    }

    companion object {
        private const val TAG = "PrefixRedirection"
    }
}
