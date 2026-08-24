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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.prefixdialer.CallRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 発信記録。このアプリが各発信で何をしたかを見せる。
 *
 * 書き換えた発信だけでなく、**書き換えなかった発信とその理由**も出す。
 * 「効いていない」と感じたときに原因を自力で確認できるようにするのが目的。
 *
 * ここに出るのはアプリ自身の動作記録で、通話履歴を読んでいるわけではない。
 */
@Composable
fun RecordsScreen(
    records: List<CallRecord>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(4.dp))

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Text(
                "このアプリが各発信で何をしたかの記録です。端末内にのみ保存され、" +
                    "外部には送信されません。通話履歴そのものではありません。",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp),
            )
        }

        if (records.isEmpty()) {
            Text(
                "まだ記録がありません。発信すると、ここに結果が残ります。",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            Card(Modifier.fillMaxWidth()) {
                Column {
                    for ((index, record) in records.withIndex()) {
                        if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        RecordRow(record)
                    }
                }
            }

            TextButton(onClick = onClear, modifier = Modifier.align(Alignment.End)) {
                Text("記録を消去")
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RecordRow(record: CallRecord) {
    ListItem(
        overlineContent = { Text(formatTimestamp(record.timestamp)) },
        headlineContent = {
            Text(record.originalNumber, fontFamily = FontFamily.Monospace)
        },
        supportingContent = {
            Text(
                text = record.dialedNumber
                    ?.let { "→ $it" }
                    ?: (record.skipReason?.description ?: "そのまま発信"),
                fontFamily = if (record.wasRewritten) FontFamily.Monospace else FontFamily.Default,
                style = MaterialTheme.typography.bodySmall,
            )
        },
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (record.wasRewritten) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "プレフィックスあり",
                        Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Icon(
                        Icons.Filled.Remove,
                        contentDescription = "プレフィックスなし",
                        Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

private val timestampFormat = SimpleDateFormat("M/d HH:mm", Locale.getDefault())

private fun formatTimestamp(millis: Long): String = timestampFormat.format(Date(millis))
