package com.example.prefixdialer

import android.content.Intent
import android.net.Uri
import android.telecom.CallRedirectionService
import android.telecom.PhoneAccountHandle
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * 発信直前に呼ばれ、設定に従って番号を書き換えてから発信し直す。
 *
 * ## 応答は速く
 *
 * このコールバックの応答が遅れると発信そのものが遅れる。設定は [SettingsStore] が
 * メモリに保持しているので毎回の読み出しは I/O にならない。ここで重い処理をしない。
 *
 * ## 例外を外に出さない
 *
 * このサービスが落ちると発信そのものが失敗しうる。判定に失敗した場合でも
 * 必ず [placeCallUnmodified] を呼び、元の番号で発信させる。
 */
class PrefixRedirectionService : CallRedirectionService() {

    private val settingsStore: SettingsStore by lazy { SettingsStore(this) }

    override fun onPlaceCall(
        handle: Uri,
        initialPhoneAccount: PhoneAccountHandle,
        allowInteractiveResponse: Boolean,
    ) {
        val original = handle.schemeSpecificPart

        val outcome = runCatching {
            val settings = settingsStore.load()
            val dial = RuleEngine.buildDialNumber(
                raw = original,
                settings = settings,
                phoneAccountId = initialPhoneAccount.id,
                isRoaming = PhoneAccounts.isRoaming(this),
            )
            dial to settings
        }.getOrElse { error ->
            // 判定できなかったときは書き換えない。発信を失敗させるより素通しが安全。
            Log.e(TAG, "failed to evaluate rules; placing call unmodified", error)
            null to null
        }

        val (dial, settings) = outcome
        if (dial == null || settings == null) {
            placeCallUnmodified()
            return
        }

        Log.d(TAG, "redirect: $original -> $dial")

        // 履歴の書き換えはオプトイン。無効なら監視サービスも起動しない
        // （通話履歴の権限を持っていない可能性があるため）。
        if (settings.callLogRewriteEnabled) {
            PendingRewrites.add(dial, original, System.currentTimeMillis())
            runCatching {
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, CallLogRewriteService::class.java),
                )
            }.onFailure { Log.w(TAG, "could not start rewrite service", it) }
        }

        redirectCall(Uri.fromParts("tel", dial, null), initialPhoneAccount, false)
    }

    companion object {
        private const val TAG = "PrefixRedirection"
    }
}
