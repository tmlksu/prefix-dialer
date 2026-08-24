package com.example.prefixdialer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * 発信に使える回線（SIM）の一覧と、ローミング状態の取得。
 *
 * ## 権限について
 *
 * 回線の一覧と表示名の取得には `READ_PHONE_STATE` が要る。しかし
 * **プレフィックスを付けるかどうかの判定にはこの権限は不要**である。
 * `CallRedirectionService.onPlaceCall` が `PhoneAccountHandle` を直接渡してくるため、
 * その `id` を [Settings.disabledPhoneAccountIds] と突き合わせるだけで済む。
 *
 * つまりこの権限が要るのは「設定画面で回線名を人間に見せるとき」だけ。
 * 権限が無ければ [available] が false になり、設定画面はその旨を表示する。
 */
object PhoneAccounts {

    private const val TAG = "PhoneAccounts"

    /** 設定画面で回線を一覧するのに必要な権限。 */
    const val PERMISSION = Manifest.permission.READ_PHONE_STATE

    /** 1 本の回線。 */
    data class Line(
        /** `PhoneAccountHandle.id`。設定の保存キーになる。 */
        val id: String,
        /** ユーザーに見せる名前（キャリア名など）。取得できなければ id を使う。 */
        val label: String,
    )

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED

    /**
     * 発信に使える回線の一覧。権限が無い、あるいは取得に失敗した場合は空リスト。
     *
     * 空リストと「回線が 1 本もない」は区別できないが、設定画面では
     * [hasPermission] と併せて表示を出し分けるので問題にならない。
     */
    fun list(context: Context): List<Line> {
        if (!hasPermission(context)) return emptyList()
        val telecom = context.getSystemService(TelecomManager::class.java) ?: return emptyList()

        return try {
            telecom.callCapablePhoneAccounts.mapNotNull { handle ->
                val id = handle.id?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                val label = runCatching {
                    telecom.getPhoneAccount(handle)?.label?.toString()
                }.getOrNull()?.takeIf { it.isNotBlank() } ?: id
                Line(id, label)
            }
        } catch (e: SecurityException) {
            // 端末やメーカーによっては権限があっても拒否されることがある。
            Log.w(TAG, "could not list phone accounts", e)
            emptyList()
        }
    }

    /**
     * ローミング中かどうか。判定できない場合は false（＝通常どおり動作する）。
     *
     * ローミング判定を誤って true にすると国内でもプレフィックスが付かなくなり、
     * ユーザーは気づかないまま割引を失う。取得できないときは通常動作に倒す。
     */
    fun isRoaming(context: Context): Boolean = try {
        context.getSystemService(TelephonyManager::class.java)?.isNetworkRoaming == true
    } catch (e: SecurityException) {
        Log.w(TAG, "could not read roaming state", e)
        false
    }
}
