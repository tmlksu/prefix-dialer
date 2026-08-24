package com.example.prefixdialer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.prefixdialer.DialRule
import com.example.prefixdialer.LeadingZero
import com.example.prefixdialer.NumberCategory
import com.example.prefixdialer.Presets
import com.example.prefixdialer.RuleAction
import com.example.prefixdialer.RuleCondition
import com.example.prefixdialer.RuleSet

/**
 * 書き換えルールの編集画面。
 *
 * ## なぜ番号種別ごとに行が分かれているのか
 *
 * 事業者によっては携帯向けと固定向けでプレフィックスも先頭 0 の扱いも異なる。
 * そのため「プレフィックスを 1 つ入力する」形式では表現できない。
 *
 * ## 入力した結果を必ず見せる
 *
 * 先頭 0 の扱いは言葉で説明しても伝わりにくく、間違えると発信が失敗するか
 * 意図しない料金になる。各行にサンプル番号への適用結果を常時表示する。
 */
@Composable
fun RulesScreen(
    ruleSet: RuleSet,
    onRuleSetChange: (RuleSet) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(4.dp))

        // --- プリセット ------------------------------------------------------

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("プリセット", style = MaterialTheme.typography.titleMedium)
                Text(
                    "選ぶと以下の設定がまとめて置き換わります。" +
                        "収録しているのは動作を確認できた事業者のみです。",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (preset in Presets.all) {
                        FilterChip(
                            selected = ruleSet.name == preset.name,
                            onClick = { onRuleSetChange(preset.copy(enabled = ruleSet.enabled)) },
                            label = { Text(preset.name) },
                        )
                    }
                }
            }
        }

        // --- 番号種別ごとのルール --------------------------------------------

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 8.dp)) {
                for ((index, category) in NumberCategory.entries.withIndex()) {
                    if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    CategoryRuleEditor(
                        category = category,
                        ruleSet = ruleSet,
                        onRuleSetChange = onRuleSetChange,
                    )
                }
            }
        }

        // --- 触れない番号の説明 ----------------------------------------------

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("設定に関わらず書き換えない番号", style = MaterialTheme.typography.titleMedium)
                Text(
                    "以下はどの設定でもそのまま発信されます。",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    """
                    ・緊急通報と 3 桁の特番（110 / 119 / 118 / 117 / 171 など）
                    ・# や * を含む番号（#7119 / #8000 / #9110 など）
                    ・フリーダイヤル 0120 / 0800、ナビダイヤル 0570、有料情報 0990
                    ・国際発信 010、他社の事業者識別番号 00XY
                    ・海外の番号
                    ・発信者番号の通知/非通知プレフィックス 184 / 186 で始まる番号
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CategoryRuleEditor(
    category: NumberCategory,
    ruleSet: RuleSet,
    onRuleSetChange: (RuleSet) -> Unit,
) {
    val condition = RuleCondition.OfType(category)
    val rule = ruleSet.rules.firstOrNull { it.condition == condition }
    val apply = rule?.action as? RuleAction.Apply
    val enabled = rule?.enabled == true && apply != null

    fun update(transform: (RuleSet) -> RuleSet) = onRuleSetChange(transform(ruleSet))

    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ListItem(
            headlineContent = { Text(category.label()) },
            supportingContent = { Text(if (enabled) "プレフィックスを付ける" else "そのまま発信") },
            trailingContent = {
                Switch(
                    checked = enabled,
                    onCheckedChange = { checked ->
                        update { it.withRuleEnabled(category, checked) }
                    },
                )
            },
        )

        if (enabled && apply != null) {
            OutlinedTextField(
                value = apply.prefix,
                onValueChange = { input ->
                    val digits = input.filter(Char::isDigit)
                    update { it.withAction(category, apply.copy(prefix = digits)) }
                },
                label = { Text("プレフィックス") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = apply.prefix.isEmpty(),
                supportingText = if (apply.prefix.isEmpty()) {
                    { Text("入力するまでプレフィックスは付きません") }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Text("元番号の先頭 0 の扱い", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (option in LeadingZero.entries) {
                    FilterChip(
                        selected = apply.leadingZero == option,
                        onClick = {
                            update { it.withAction(category, apply.copy(leadingZero = option)) }
                        },
                        label = { Text(option.label()) },
                    )
                }
            }

            // 設定の結果を必ず具体例で見せる。先頭 0 の扱いは言葉では伝わりにくい。
            val sample = category.sampleNumber()
            AssistChip(
                onClick = {},
                enabled = false,
                label = {
                    Text(
                        text = "$sample → " +
                            apply.prefix + apply.leadingZero.apply(sample) + apply.suffix,
                        fontFamily = FontFamily.Monospace,
                    )
                },
            )
        }
    }
}

private fun LeadingZero.label(): String = when (this) {
    LeadingZero.KEEP -> "残す"
    LeadingZero.STRIP -> "取る"
    LeadingZero.TO_COUNTRY_CODE -> "81 にする"
}

/**
 * 指定の番号種別のルールを有効/無効にする。
 *
 * 無効化はルールを削除せず `enabled = false` にする。プレフィックスの入力内容を
 * 保ったまま一時的に切れるようにするため。
 */
private fun RuleSet.withRuleEnabled(category: NumberCategory, enabled: Boolean): RuleSet {
    val condition = RuleCondition.OfType(category)
    val existing = rules.firstOrNull { it.condition == condition }

    return when {
        existing != null -> copy(
            rules = rules.map { if (it.condition == condition) it.copy(enabled = enabled) else it },
        )
        // まだルールが無い種別を有効化した場合は、他の種別の設定を引き継いで作る。
        // 多くの事業者は全種別で同じ形式なので、そのほうが入力の手間が少ない。
        enabled -> {
            val template = rules.firstNotNullOfOrNull { it.action as? RuleAction.Apply }
                ?: RuleAction.Apply(prefix = "", leadingZero = LeadingZero.KEEP)
            copy(rules = rules + DialRule(condition, template, enabled = true))
        }
        else -> this
    }
}

private fun RuleSet.withAction(category: NumberCategory, action: RuleAction): RuleSet {
    val condition = RuleCondition.OfType(category)
    return copy(
        rules = rules.map { if (it.condition == condition) it.copy(action = action) else it },
    )
}
