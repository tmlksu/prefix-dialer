package io.github.tmlksu.prefixdialer.ui

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.tmlksu.prefixdialer.NumberCategory
import io.github.tmlksu.prefixdialer.R
import io.github.tmlksu.prefixdialer.RuleAction
import io.github.tmlksu.prefixdialer.RuleCondition
import io.github.tmlksu.prefixdialer.Settings
import io.github.tmlksu.prefixdialer.SystemStatus

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
                title = stringResource(R.string.warn_unsupported_title),
                body = stringResource(R.string.warn_unsupported_body),
            )
        } else if (!status.hasRedirectionRole) {
            WarningCard(
                title = stringResource(R.string.warn_no_role_title),
                body = stringResource(R.string.warn_no_role_body),
                actionLabel = stringResource(R.string.warn_no_role_action),
                onAction = onRequestRole,
            )
        } else if (!settings.ruleSet.enabled) {
            InfoCard(
                icon = Icons.Filled.Block,
                title = stringResource(R.string.info_disabled_title),
                body = stringResource(R.string.info_disabled_body),
            )
        } else if (settings.ruleSet.rules.none { it.enabled && it.action is RuleAction.Apply }) {
            WarningCard(
                title = stringResource(R.string.warn_no_rules_title),
                body = stringResource(R.string.warn_no_rules_body),
                actionLabel = stringResource(R.string.warn_no_rules_action),
                onAction = onOpenRules,
            )
        }

        // --- マスタースイッチ ----------------------------------------------

        Card(Modifier.fillMaxWidth()) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.master_switch_title)) },
                supportingContent = {
                    Text(
                        stringResource(
                            if (settings.ruleSet.enabled) R.string.master_switch_on
                            else R.string.master_switch_off,
                        ),
                    )
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
                    overlineContent = { Text(stringResource(R.string.current_settings)) },
                    headlineContent = { Text(settings.ruleSet.displayName()) },
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
                    title = stringResource(R.string.screen_rules),
                    subtitle = stringResource(R.string.nav_rules_subtitle),
                    onClick = onOpenRules,
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                NavigationRow(
                    title = stringResource(R.string.screen_exclusions),
                    subtitle = if (settings.excludedNumbers.isEmpty()) {
                        stringResource(R.string.nav_exclusions_none)
                    } else {
                        stringResource(R.string.nav_exclusions_count, settings.excludedNumbers.size)
                    },
                    onClick = onOpenExclusions,
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                NavigationRow(
                    title = stringResource(R.string.screen_records),
                    subtitle = stringResource(R.string.nav_records_subtitle),
                    onClick = onOpenRecords,
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                NavigationRow(
                    title = stringResource(R.string.screen_advanced),
                    subtitle = stringResource(R.string.nav_advanced_subtitle),
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
        !settings.ruleSet.enabled -> stringResource(R.string.summary_paused)
        action is RuleAction.Apply ->
            action.prefix + action.leadingZero.apply(sample) + action.suffix
        else -> stringResource(R.string.summary_unchanged)
    }

    ListItem(
        headlineContent = { Text(stringResource(category.labelRes)) },
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


