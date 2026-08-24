package com.example.prefixdialer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.prefixdialer.NumberCategory
import com.example.prefixdialer.RuleAction
import com.example.prefixdialer.RuleCondition
import com.example.prefixdialer.Settings
import com.example.prefixdialer.SystemStatus

/**
 * ホーム画面。現在の状態と、機能が効かなくなる原因を最優先で見せる。
 *
 * この画面の一番大事な仕事は「**効いていないことに気づかせる**」こと。
 * プレフィックスが黙って付かなくなると、ユーザーは気づかないまま通常料金を
 * 払い続けることになる。そのため異常状態は画面最上部に警告として出す。
 */
@Composable
fun HomeScreen(
    settings: Settings,
    status: SystemStatus,
    onMasterSwitchChange: (Boolean) -> Unit,
    onRequestRole: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenExclusions: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenAdvanced: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(4.dp))

        // --- 機能が効かない状態を最優先で警告する --------------------------

        if (!status.roleAvailable) {
            WarningCard(
                title = "この端末では利用できません",
                body = "通話リダイレクト機能に対応していない端末です。" +
                    "Android 10 以降かつ、メーカーがこの機能を無効化していない必要があります。",
            )
        } else if (!status.hasRedirectionRole) {
            WarningCard(
                title = "通話リダイレクトが無効です",
                body = "プレフィックスは付きません。他の通話系アプリにこの役割を" +
                    "取られた可能性があります（端末に 1 アプリしか設定できません）。",
                actionLabel = "有効にする",
                onAction = onRequestRole,
            )
        } else if (!settings.ruleSet.enabled) {
            InfoCard(
                icon = Icons.Filled.Block,
                title = "停止中",
                body = "マスタースイッチが OFF です。すべての発信がそのまま行われます。",
            )
        } else if (settings.ruleSet.rules.none { it.enabled && it.action is RuleAction.Apply }) {
            WarningCard(
                title = "ルールが設定されていません",
                body = "有効なルールが 1 件もないため、プレフィックスは付きません。",
                actionLabel = "設定する",
                onAction = onOpenRules,
            )
        }

        // --- マスタースイッチ ----------------------------------------------

        Card(Modifier.fillMaxWidth()) {
            ListItem(
                headlineContent = { Text("プレフィックスを付ける") },
                supportingContent = {
                    Text(if (settings.ruleSet.enabled) "有効" else "停止中")
                },
                trailingContent = {
                    Switch(
                        checked = settings.ruleSet.enabled,
                        onCheckedChange = onMasterSwitchChange,
                        enabled = status.hasRedirectionRole,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            )
        }

        // --- 現在のルールの要約 --------------------------------------------

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 8.dp)) {
                ListItem(
                    overlineContent = { Text("現在の設定") },
                    headlineContent = { Text(settings.ruleSet.name) },
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                for (category in NumberCategory.entries) {
                    RuleSummaryRow(settings, category)
                }
            }
        }

        // --- 各設定へ --------------------------------------------------------

        Card(Modifier.fillMaxWidth()) {
            Column {
                NavigationRow(
                    title = "書き換えルール",
                    subtitle = "プレフィックスと番号種別ごとの設定",
                    onClick = onOpenRules,
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                NavigationRow(
                    title = "除外する番号",
                    subtitle = if (settings.excludedNumbers.isEmpty()) {
                        "なし"
                    } else {
                        "${settings.excludedNumbers.size} 件"
                    },
                    onClick = onOpenExclusions,
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                NavigationRow(
                    title = "発信記録",
                    subtitle = "各発信で何をしたかの記録",
                    onClick = onOpenRecords,
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                NavigationRow(
                    title = "詳細設定",
                    subtitle = "通話履歴・SIM・ローミング・バックアップ",
                    onClick = onOpenAdvanced,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/** 1 つの番号種別について、いま何が起きるかを 1 行で示す。 */
@Composable
private fun RuleSummaryRow(settings: Settings, category: NumberCategory) {
    val sample = category.sampleNumber()
    val action = settings.ruleSet.rules
        .firstOrNull {
            it.enabled && it.condition == RuleCondition.OfType(category)
        }?.action

    val description = when {
        !settings.ruleSet.enabled -> "停止中"
        action is RuleAction.Apply ->
            action.prefix + action.leadingZero.apply(sample) + action.suffix
        else -> "そのまま発信"
    }

    ListItem(
        headlineContent = { Text(category.label()) },
        supportingContent = {
            Text(
                text = "$sample → $description",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
            )
        },
    )
}

@Composable
private fun NavigationRow(title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
        modifier = Modifier.clickableRow(onClick),
    )
}

@Composable
private fun WarningCard(
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null, Modifier.size(20.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Text(body, style = MaterialTheme.typography.bodyMedium)
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction, modifier = Modifier.align(Alignment.End)) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = null, Modifier.size(20.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

internal fun NumberCategory.label(): String = when (this) {
    NumberCategory.MOBILE -> "携帯電話"
    NumberCategory.FIXED_LINE -> "固定電話"
    NumberCategory.VOIP -> "IP電話 (050)"
}

/** ルールの効き方を具体的に見せるためのサンプル番号（実在しない番号帯を使う）。 */
internal fun NumberCategory.sampleNumber(): String = when (this) {
    NumberCategory.MOBILE -> "09012345678"
    NumberCategory.FIXED_LINE -> "0312345678"
    NumberCategory.VOIP -> "05012345678"
}
