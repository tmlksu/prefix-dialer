package io.github.tmlksu.prefixdialer

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import androidx.core.content.ContextCompat

/**
 * OS 側の状態のスナップショット。
 *
 * 画面はこの値だけを見て描画する。Android API の呼び出しは [read] に閉じ込め、
 * UI 側に散らさない。
 */
data class SystemStatus(

    /** `ROLE_CALL_REDIRECTION` を保持しているか。これが false だと機能が一切動かない。 */
    val hasRedirectionRole: Boolean = false,

    /** 端末がこのロールに対応しているか（非対応端末では設定導線を出さない）。 */
    val roleAvailable: Boolean = true,

    /** 通話履歴の読み書き権限があるか。履歴書き換え機能を有効にした場合のみ必要。 */
    val hasCallLogPermissions: Boolean = false,

    /** 連絡先の読み取り権限があるか。履歴の表示名を補完するために使う。 */
    val hasContactsPermission: Boolean = false,

    /** 通知権限があるか。履歴書き換えの短命フォアグラウンドサービスに必要。 */
    val hasNotificationPermission: Boolean = false,

    /** バッテリー最適化から除外されているか。One UI で履歴書き換えが殺されるのを防ぐ。 */
    val ignoringBatteryOptimizations: Boolean = false,
) {

    /** 履歴書き換え機能に必要な権限がすべて揃っているか。 */
    val readyForCallLogRewrite: Boolean
        get() = hasCallLogPermissions && hasNotificationPermission

    companion object {

        /** 履歴書き換え機能を有効にするときに要求する権限。 */
        fun callLogPermissions(): Array<String> = buildList {
            add(Manifest.permission.READ_CALL_LOG)
            add(Manifest.permission.WRITE_CALL_LOG)
            add(Manifest.permission.READ_CONTACTS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()

        fun read(context: Context): SystemStatus {
            val roleManager = context.getSystemService(RoleManager::class.java)
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

            return SystemStatus(
                hasRedirectionRole = roleManager?.isRoleHeld(RoleManager.ROLE_CALL_REDIRECTION) == true,
                roleAvailable = roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_REDIRECTION) == true,
                hasCallLogPermissions = context.hasAll(
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.WRITE_CALL_LOG,
                ),
                hasContactsPermission = context.hasAll(Manifest.permission.READ_CONTACTS),
                hasNotificationPermission =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.hasAll(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        true
                    },
                ignoringBatteryOptimizations =
                    powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true,
            )
        }

        private fun Context.hasAll(vararg permissions: String): Boolean = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        /**
         * 通話リダイレクトのロールを要求する Intent。既に保持している場合は null。
         *
         * このロールは端末に 1 アプリのみ。他アプリが保持している場合、
         * ユーザーがこのダイアログで承認すると自動的に奪い返せる。
         */
        fun requestRoleIntent(context: Context): Intent? {
            val roleManager = context.getSystemService(RoleManager::class.java) ?: return null
            if (!roleManager.isRoleAvailable(RoleManager.ROLE_CALL_REDIRECTION)) return null
            if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_REDIRECTION)) return null
            return roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_REDIRECTION)
        }

        /**
         * バッテリー最適化の設定へ誘導する Intent の候補。先頭から順に試す。
         *
         * `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`（その場で許可を求めるダイアログ）は
         * 使わない。あれは `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` 権限の宣言が必須で、
         * 未宣言だと**何も起きずに失敗する**（Galaxy S25 で確認）。かつこの権限は
         * Google Play が用途を限定している制限付き権限で、宣言すると審査で問題になりうる。
         *
         * そこで設定画面へ送る方式にした。ユーザーの操作は 1 手増えるが、
         * 権限を増やさずに済み、確実に目的の画面へ辿り着ける。
         *
         * 1. 電池の最適化の一覧（素の Android ではここが最短）
         * 2. アプリ情報の画面（Samsung / One UI ではバッテリー設定がこの配下にある）
         */
        fun batteryOptimizationIntents(context: Context): List<Intent> = listOf(
            Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            appDetailsIntent(context),
        )

        /** アプリ情報の画面を開く Intent。 */
        fun appDetailsIntent(context: Context): Intent =
            Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:${context.packageName}"))
    }
}
