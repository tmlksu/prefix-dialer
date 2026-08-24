package io.github.tmlksu.prefixdialer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.tmlksu.prefixdialer.PhoneAccounts
import io.github.tmlksu.prefixdialer.Settings
import io.github.tmlksu.prefixdialer.SystemStatus

/**
 * 詳細設定。通話履歴・SIM・ローミング・バックアップ。
 *
 * 機微な権限を要する機能はここに集め、**有効化した瞬間に初めて権限を要求する**。
 * インストール直後に通話履歴の権限を求めない（`DECISIONS.md` D-14）。
 */
@Composable
fun AdvancedScreen(
    settings: Settings,
    status: SystemStatus,
    lines: List<PhoneAccounts.Line>,
    hasPhoneStatePermission: Boolean,
    onSettingsChange: (Settings) -> Unit,
    onEnableCallLogRewrite: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onRequestPhoneStatePermission: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(4.dp))

        // --- 通話履歴の書き換え ----------------------------------------------

        SectionCard(
            title = "通話履歴の書き換え",
            description = "発信後に、通話履歴の番号をプレフィックスなしの元番号へ戻します。" +
                "電話帳の名前が表示されるようになります。",
        ) {
            ListItem(
                headlineContent = { Text("履歴を元の番号に戻す") },
                supportingContent = {
                    Text(
                        when {
                            !settings.callLogRewriteEnabled -> "無効"
                            !status.readyForCallLogRewrite -> "権限が不足しています"
                            !status.ignoringBatteryOptimizations -> "バッテリー最適化の解除を推奨"
                            else -> "有効"
                        },
                    )
                },
                trailingContent = {
                    Switch(
                        checked = settings.callLogRewriteEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                // 有効化のタイミングで初めて通話履歴の権限を要求する
                                onEnableCallLogRewrite()
                            } else {
                                onSettingsChange(settings.copy(callLogRewriteEnabled = false))
                            }
                        },
                    )
                },
            )

            if (settings.callLogRewriteEnabled) {
                if (!status.readyForCallLogRewrite) {
                    NoteText(
                        "通話履歴の読み書き権限が必要です。許可するまでこの機能は動作しません。",
                        isWarning = true,
                    )
                    TextButton(onClick = onEnableCallLogRewrite) { Text("権限を許可する") }
                } else if (!status.ignoringBatteryOptimizations) {
                    NoteText(
                        "バッテリー最適化が有効だと、書き換え処理が途中で止められることがあります" +
                            "（Samsung / One UI では特に起きやすい）。",
                        isWarning = true,
                    )
                    TextButton(onClick = onRequestBatteryExemption) {
                        Text("バッテリー最適化を解除する")
                    }
                }
            } else {
                NoteText(
                    "この機能を使わない場合、通話履歴の権限は一切要求されません。" +
                        "履歴にはプレフィックス付きの番号が残ります。",
                )
            }
        }

        // --- ローミング ------------------------------------------------------

        SectionCard(
            title = "ローミング",
            description = "海外のネットワークに接続しているときの動作。",
        ) {
            ListItem(
                headlineContent = { Text("ローミング中は停止する") },
                supportingContent = {
                    Text(
                        "国内向けのプレフィックスは海外では機能せず、" +
                            "意図しない料金が発生する可能性があります。",
                    )
                },
                trailingContent = {
                    Switch(
                        checked = settings.disableWhileRoaming,
                        onCheckedChange = {
                            onSettingsChange(settings.copy(disableWhileRoaming = it))
                        },
                    )
                },
            )
        }

        // --- SIM / 回線ごとの設定 --------------------------------------------

        SectionCard(
            title = "回線ごとの設定",
            description = "契約していない回線でプレフィックスを使うと、割引が効かず" +
                "通常より高い料金になることがあります。",
        ) {
            when {
                !hasPhoneStatePermission -> {
                    NoteText(
                        "回線の一覧を表示するには電話状態の権限が必要です。" +
                            "この権限はプレフィックスの動作そのものには不要で、" +
                            "回線名を表示するためだけに使います。",
                    )
                    TextButton(onClick = onRequestPhoneStatePermission) {
                        Text("回線を表示する")
                    }
                }
                lines.isEmpty() -> NoteText("利用できる回線が見つかりませんでした。")
                else -> {
                    for ((index, line) in lines.withIndex()) {
                        if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        val enabled = line.id !in settings.disabledPhoneAccountIds
                        ListItem(
                            headlineContent = { Text(line.label) },
                            supportingContent = { Text(if (enabled) "有効" else "この回線では付けない") },
                            trailingContent = {
                                Switch(
                                    checked = enabled,
                                    onCheckedChange = { checked ->
                                        val updated = if (checked) {
                                            settings.disabledPhoneAccountIds - line.id
                                        } else {
                                            settings.disabledPhoneAccountIds + line.id
                                        }
                                        onSettingsChange(
                                            settings.copy(disabledPhoneAccountIds = updated),
                                        )
                                    },
                                )
                            },
                        )
                    }
                }
            }
        }

        // --- バックアップ ----------------------------------------------------

        SectionCard(
            title = "設定のバックアップ",
            description = "設定を JSON ファイルとして書き出し、別の端末で読み込めます。" +
                "回線ごとの設定は端末固有なので含まれません。",
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onExport) { Text("書き出す") }
                OutlinedButton(onClick = onImport) { Text("読み込む") }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionCard(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
            content()
        }
    }
}

@Composable
private fun NoteText(text: String, isWarning: Boolean = false) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isWarning) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (isWarning) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp),
        )
    }
}
