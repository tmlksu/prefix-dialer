package com.example.prefixdialer

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.prefixdialer.ui.AdvancedScreen
import com.example.prefixdialer.ui.ExclusionsScreen
import com.example.prefixdialer.ui.HomeScreen
import com.example.prefixdialer.ui.PrefixDialerTheme
import com.example.prefixdialer.ui.RecordsScreen
import com.example.prefixdialer.ui.RulesScreen

/** 画面。数が少ないのでナビゲーションライブラリは入れず、状態で切り替える。 */
private enum class Screen(val title: String) {
    HOME("Prefix Dialer"),
    RULES("書き換えルール"),
    EXCLUSIONS("除外する番号"),
    RECORDS("発信記録"),
    ADVANCED("詳細設定"),
}

/**
 * 設定画面のホスト。
 *
 * 状態は 2 つだけ。永続化された [Settings] と、OS 側の [SystemStatus]。
 * 後者は権限やロールの取得後・画面復帰時に読み直す（他アプリにロールを
 * 奪われた場合など、アプリの外で変わりうるため）。
 */
class MainActivity : ComponentActivity() {

    private val settingsStore: SettingsStore by lazy { SettingsStore(this) }
    private val callRecordStore: CallRecordStore by lazy { CallRecordStore(this) }

    private var records by mutableStateOf(emptyList<CallRecord>())

    private var settings by mutableStateOf(Settings())
    private var status by mutableStateOf(SystemStatus())
    private var lines by mutableStateOf(emptyList<PhoneAccounts.Line>())
    private var hasPhoneStatePermission by mutableStateOf(false)

    private val roleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { refreshSystemState() }

    private val systemSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { refreshSystemState() }

    /**
     * 通話履歴まわりの権限。許可されて初めて履歴書き換えを有効にする。
     * 拒否された場合は設定を変えない（「有効なのに動かない」状態を作らないため）。
     */
    private val callLogPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        refreshSystemState()
        val essential = listOf(
            android.Manifest.permission.READ_CALL_LOG,
            android.Manifest.permission.WRITE_CALL_LOG,
        )
        if (essential.all { granted[it] == true }) {
            updateSettings { it.copy(callLogRewriteEnabled = true) }
        } else {
            toast("通話履歴の権限が無いため、履歴の書き換えは有効にできません")
        }
    }

    private val phoneStatePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refreshSystemState() }

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        val ok = runCatching {
            contentResolver.openOutputStream(uri)?.use {
                it.write(settingsStore.exportJson().toByteArray())
            } ?: error("could not open $uri")
        }.isSuccess
        toast(if (ok) "設定を書き出しました" else "書き出しに失敗しました")
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        val text = runCatching {
            contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
        }.getOrNull()

        if (text == null) {
            toast("ファイルを読めませんでした")
            return@registerForActivityResult
        }
        if (settingsStore.importJson(text)) {
            settings = settingsStore.load()
            toast("設定を読み込みました")
        } else {
            toast("設定の形式が正しくありません")
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settings = settingsStore.load()

        setContent {
            var screen by remember { mutableStateOf(Screen.HOME) }

            PrefixDialerTheme {
                // 設定画面から戻るときはホームへ。端末の戻るボタンでいきなり閉じさせない。
                BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(screen.title) },
                            navigationIcon = {
                                if (screen != Screen.HOME) {
                                    IconButton(onClick = { screen = Screen.HOME }) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "戻る",
                                        )
                                    }
                                }
                            },
                        )
                    },
                ) { innerPadding ->
                    val contentModifier = Modifier.padding(innerPadding)
                    when (screen) {
                        Screen.HOME -> HomeScreen(
                            settings = settings,
                            status = status,
                            onMasterSwitchChange = { enabled ->
                                updateSettings {
                                    it.copy(ruleSet = it.ruleSet.copy(enabled = enabled))
                                }
                            },
                            onRequestRole = ::requestRedirectionRole,
                            onOpenRules = { screen = Screen.RULES },
                            onOpenExclusions = { screen = Screen.EXCLUSIONS },
                            onOpenRecords = { screen = Screen.RECORDS },
                            onOpenAdvanced = { screen = Screen.ADVANCED },
                            modifier = contentModifier,
                        )

                        Screen.RULES -> RulesScreen(
                            ruleSet = settings.ruleSet,
                            onRuleSetChange = { ruleSet ->
                                updateSettings { it.copy(ruleSet = ruleSet.renamedIfEdited()) }
                            },
                            modifier = contentModifier,
                        )

                        Screen.EXCLUSIONS -> ExclusionsScreen(
                            excludedNumbers = settings.excludedNumbers,
                            onChange = { numbers ->
                                updateSettings { it.copy(excludedNumbers = numbers) }
                            },
                            modifier = contentModifier,
                        )

                        Screen.RECORDS -> RecordsScreen(
                            records = records,
                            onClear = {
                                callRecordStore.clear()
                                records = callRecordStore.list()
                            },
                            modifier = contentModifier,
                        )

                        Screen.ADVANCED -> AdvancedScreen(
                            settings = settings,
                            status = status,
                            lines = lines,
                            hasPhoneStatePermission = hasPhoneStatePermission,
                            onSettingsChange = { updated -> updateSettings { updated } },
                            onEnableCallLogRewrite = {
                                callLogPermissionLauncher.launch(SystemStatus.callLogPermissions())
                            },
                            onRequestBatteryExemption = {
                                systemSettingsLauncher.launch(
                                    SystemStatus.ignoreBatteryOptimizationsIntent(this),
                                )
                            },
                            onRequestPhoneStatePermission = {
                                phoneStatePermissionLauncher.launch(PhoneAccounts.PERMISSION)
                            },
                            onExport = { exportLauncher.launch(EXPORT_FILE_NAME) },
                            onImport = {
                                importLauncher.launch(arrayOf("application/json", "text/plain"))
                            },
                            modifier = contentModifier,
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // ロールは他アプリに奪われることがあり、権限も設定アプリ側で取り消せる。
        // アプリの外で変わりうる状態なので、復帰のたびに読み直す。
        refreshSystemState()
        settings = settingsStore.load()
        records = callRecordStore.list()
    }

    private fun refreshSystemState() {
        status = SystemStatus.read(this)
        hasPhoneStatePermission = PhoneAccounts.hasPermission(this)
        lines = PhoneAccounts.list(this)
    }

    private fun requestRedirectionRole() {
        val intent: Intent? = SystemStatus.requestRoleIntent(this)
        if (intent == null) {
            refreshSystemState()
            return
        }
        roleLauncher.launch(intent)
    }

    private fun updateSettings(transform: (Settings) -> Settings) {
        settings = settingsStore.update(transform)
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * プリセットから変更されていれば「カスタム」に改名する。
     *
     * プリセット名のまま中身だけ違う状態になると、ユーザーが自分の設定内容を
     * 誤解する。名前と中身を一致させる。
     */
    private fun RuleSet.renamedIfEdited(): RuleSet {
        val matchesPreset = Presets.all.any { it.name == name && it.rules == rules }
        return if (matchesPreset || name == Presets.custom.name) this else copy(name = "カスタム")
    }

    companion object {
        private const val EXPORT_FILE_NAME = "prefix-dialer-settings.json"
    }
}
