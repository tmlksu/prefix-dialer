package com.example.prefixdialer

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.telecom.CallRedirectionService
import android.telecom.PhoneAccountHandle
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * 発信直前に呼ばれ、対象番号なら 0063 プレフィックスを付けて発信し直す。
 * 両SIM共通ロジックなので PhoneAccountHandle は判定に使わない。
 */
class PrefixRedirectionService : CallRedirectionService() {

    override fun onPlaceCall(
        handle: Uri,
        initialPhoneAccount: PhoneAccountHandle,
        allowInteractiveResponse: Boolean,
    ) {
        val original = handle.schemeSpecificPart
        val dial = PhoneNumberPrefixer.buildDialNumber(original)

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
