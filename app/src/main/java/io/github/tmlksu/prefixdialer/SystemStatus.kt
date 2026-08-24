package io.github.tmlksu.prefixdialer

import android.Manifest
import android.app.Activity
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

    /**
     * 基本機能（プレフィックス付与）が動作する状態か。
     *
     * ロールだけで足りる。通話履歴の権限は基本機能には不要。
     */
    val readyForPrefixing: Boolean get() = hasRedirectionRole

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

        /** バッテリー最適化からの除外を要求する Intent。 */
        @Suppress("BatteryLife") // 履歴書き換えを有効にした場合のみ案内する
        fun ignoreBatteryOptimizationsIntent(context: Context): Intent =
            Intent(AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:${context.packageName}"))

        /** アプリの設定画面を開く Intent（権限を「今後表示しない」にされた場合の逃げ道）。 */
        fun appSettingsIntent(context: Context): Intent =
            Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:${context.packageName}"))

        /**
         * 権限ダイアログが二度と出ない状態か（「今後表示しない」を選ばれた）。
         * この場合はアプリ設定画面へ誘導するしかない。
         */
        fun isPermanentlyDenied(activity: Activity, permission: String): Boolean =
            ContextCompat.checkSelfPermission(activity, permission) !=
                PackageManager.PERMISSION_GRANTED &&
                !activity.shouldShowRequestPermissionRationale(permission)
    }
}
