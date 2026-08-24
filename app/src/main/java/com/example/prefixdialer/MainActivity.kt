package com.example.prefixdialer

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * セットアップ画面。以下を順に有効化してもらう:
 *  1. 通話リダイレクトのロール (ROLE_CALL_REDIRECTION)
 *  2. 通話履歴・連絡先・通知のランタイム権限
 *  3. バッテリー最適化からの除外（One UI 対策）
 */
class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView

    private val roleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { refreshStatus() }

    private val permsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { refreshStatus() }

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { refreshStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        val title = TextView(this).apply {
            text = "Prefix Dialer"
            textSize = 24f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        root.addView(title)
        root.addView(spacer())

        status = TextView(this).apply { textSize = 15f }
        root.addView(status)

        root.addView(spacer())
        root.addView(button("1. 通話リダイレクトを有効化") { requestRedirectionRole() })
        root.addView(button("2. 権限を許可（通話履歴・連絡先・通知）") { requestPermissions() })
        root.addView(button("3. バッテリー最適化を解除") { requestIgnoreBattery() })
        root.addView(spacer())
        root.addView(button("状態を更新") { refreshStatus() })

        setContentView(ScrollView(this).apply { addView(root) })

        // Android 15 のエッジツーエッジ対策: システムバー分の余白を確保して見切れを防ぐ
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(48, 48 + bars.top, 48, 48 + bars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun requestRedirectionRole() {
        val rm = getSystemService(RoleManager::class.java) ?: return
        if (rm.isRoleAvailable(RoleManager.ROLE_CALL_REDIRECTION) &&
            !rm.isRoleHeld(RoleManager.ROLE_CALL_REDIRECTION)
        ) {
            roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_REDIRECTION))
        } else {
            refreshStatus()
        }
    }

    private fun requestPermissions() {
        val perms = buildList {
            add(Manifest.permission.READ_CALL_LOG)
            add(Manifest.permission.WRITE_CALL_LOG)
            add(Manifest.permission.READ_CONTACTS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
        permsLauncher.launch(perms)
    }

    private fun requestIgnoreBattery() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        settingsLauncher.launch(intent)
    }

    private fun refreshStatus() {
        val rm = getSystemService(RoleManager::class.java)
        val hasRole = rm?.isRoleHeld(RoleManager.ROLE_CALL_REDIRECTION) == true
        val hasPerms = listOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
        ).all {
            ContextCompat.checkSelfPermission(this, it) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryOk = pm.isIgnoringBatteryOptimizations(packageName)

        status.text = buildString {
            appendLine("プレフィックス: ${PhoneNumberPrefixer.PREFIX}")
            appendLine()
            appendLine("${mark(hasRole)} 通話リダイレクト")
            appendLine("${mark(hasPerms)} 通話履歴・連絡先の権限")
            appendLine("${mark(batteryOk)} バッテリー最適化の解除")
            appendLine()
            if (hasRole && hasPerms && batteryOk) {
                append("準備完了。国内の携帯・固定電話への発信に自動でプレフィックスが付きます。")
            } else {
                append("上のボタンで未完了の項目を有効化してください。")
            }
        }
    }

    private fun mark(ok: Boolean) = if (ok) "✅" else "⬜"

    private fun spacer() = TextView(this).apply { height = 48 }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        gravity = Gravity.START or Gravity.CENTER_VERTICAL
        setOnClickListener { onClick() }
    }
}
