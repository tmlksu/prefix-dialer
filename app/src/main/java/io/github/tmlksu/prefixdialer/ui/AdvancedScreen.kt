package io.github.tmlksu.prefixdialer.ui

import android.content.ActivityNotFoundException
import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.tmlksu.prefixdialer.AppLinks
import io.github.tmlksu.prefixdialer.BuildConfig
import io.github.tmlksu.prefixdialer.CallLogRewrite
import io.github.tmlksu.prefixdialer.PhoneAccounts
import io.github.tmlksu.prefixdialer.R
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
    val context = LocalContext.current

    /**
     * リンクをブラウザで開く。INTERNET 権限は要らない（開くのは別アプリ）。
     *
     * ブラウザを持たない端末では何も起きない。ここで落としたり警告を出したりする
     * ほどのことではないので、例外だけ握る。
     */
    fun openLink(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: ActivityNotFoundException) {
            // 開けるアプリが無い。何もしない。
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(4.dp))

        // --- 通話履歴の書き換え ----------------------------------------------
        // Play 版はこの機能を持たないので、操作できる項目は出さない。
        // 「あるのに使えない」状態を作らないため。
        //
        // ただし黙って消すと、github 版から移った人には「設定が消えた」としか
        // 見えない。無いことと、その理由だけを置く（PLAY-RELEASE.md §9）。

        if (!CallLogRewrite.AVAILABLE) SectionCard(
            title = stringResource(R.string.calllog_title),
            description = stringResource(R.string.calllog_unavailable),
        ) {}

        if (CallLogRewrite.AVAILABLE) SectionCard(
            title = stringResource(R.string.calllog_title),
            description = stringResource(R.string.calllog_description),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.calllog_switch)) },
                supportingContent = {
                    Text(
                        stringResource(
                            when {
                                !settings.callLogRewriteEnabled -> R.string.calllog_state_off
                                !status.readyForCallLogRewrite -> R.string.calllog_state_missing_permission
                                !status.ignoringBatteryOptimizations -> R.string.calllog_state_battery
                                else -> R.string.calllog_state_on
                            },
                        ),
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
                    NoteText(stringResource(R.string.calllog_note_permission), isWarning = true)
                    TextButton(onClick = onEnableCallLogRewrite) {
                        Text(stringResource(R.string.calllog_note_permission_action))
                    }
                } else if (!status.ignoringBatteryOptimizations) {
                    NoteText(stringResource(R.string.calllog_note_battery), isWarning = true)
                    TextButton(onClick = onRequestBatteryExemption) {
                        Text(stringResource(R.string.calllog_note_battery_action))
                    }
                }
            } else {
                NoteText(stringResource(R.string.calllog_note_off))
            }
        }

        // --- ローミング ------------------------------------------------------

        SectionCard(
            title = stringResource(R.string.roaming_title),
            description = stringResource(R.string.roaming_description),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.roaming_switch)) },
                supportingContent = {
                    Text(stringResource(R.string.roaming_switch_description))
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
            title = stringResource(R.string.lines_title),
            description = stringResource(R.string.lines_description),
        ) {
            when {
                !hasPhoneStatePermission -> {
                    NoteText(stringResource(R.string.lines_permission_note))
                    TextButton(onClick = onRequestPhoneStatePermission) {
                        Text(stringResource(R.string.lines_permission_action))
                    }
                }
                lines.isEmpty() -> NoteText(stringResource(R.string.lines_empty))
                else -> {
                    for ((index, line) in lines.withIndex()) {
                        if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        val enabled = line.id !in settings.disabledPhoneAccountIds
                        ListItem(
                            headlineContent = { Text(line.label) },
                            supportingContent = {
                                Text(
                                    stringResource(
                                        if (enabled) R.string.lines_enabled
                                        else R.string.lines_disabled,
                                    ),
                                )
                            },
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
            title = stringResource(R.string.backup_title),
            description = stringResource(R.string.backup_description),
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onExport) { Text(stringResource(R.string.backup_export)) }
                OutlinedButton(onClick = onImport) { Text(stringResource(R.string.backup_import)) }
            }
        }

        // --- このアプリについて ----------------------------------------------
        // プライバシーポリシーへの導線は Google Play の明文要件で、Console の
        // フィールドと**アプリ内**の両方に要る（PLAY-RELEASE.md §5）。

        SectionCard(
            title = stringResource(R.string.about_title),
            description = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.about_privacy_policy)) },
                supportingContent = { Text(AppLinks.PRIVACY_POLICY) },
                modifier = Modifier.clickableRow { openLink(AppLinks.PRIVACY_POLICY) },
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            ListItem(
                headlineContent = { Text(stringResource(R.string.about_source_code)) },
                supportingContent = { Text(AppLinks.SOURCE_CODE) },
                modifier = Modifier.clickableRow { openLink(AppLinks.SOURCE_CODE) },
            )
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
