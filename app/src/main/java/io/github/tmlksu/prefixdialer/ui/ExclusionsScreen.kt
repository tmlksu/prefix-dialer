package io.github.tmlksu.prefixdialer.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.tmlksu.prefixdialer.Settings

/**
 * 個別に除外する番号の一覧。
 *
 * 「この番号だけは絶対にプレフィックスを付けない」を表現する。
 * 会社の内線や、特定の相手だけ通常回線で掛けたい場合に使う。
 * ルールより優先される。
 */
@Composable
fun ExclusionsScreen(
    excludedNumbers: Set<String>,
    onChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var input by remember { mutableStateOf("") }
    val normalized = Settings.normalizeNumber(input)
    val alreadyPresent = normalized.isNotEmpty() && normalized in excludedNumbers

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
                "ここに登録した番号には、ルールの設定に関わらずプレフィックスが付きません。" +
                    "ハイフンや括弧は無視して照合します。",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("番号を追加") },
                    placeholder = { Text("03-1234-5678") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = alreadyPresent,
                    supportingText = when {
                        alreadyPresent -> {
                            { Text("すでに登録されています") }
                        }
                        normalized.isNotEmpty() -> {
                            { Text("登録される形式: $normalized") }
                        }
                        else -> null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        onChange(excludedNumbers + normalized)
                        input = ""
                    },
                    enabled = normalized.isNotEmpty() && !alreadyPresent,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("追加")
                }
            }
        }

        if (excludedNumbers.isEmpty()) {
            Text(
                "除外している番号はありません。",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            Card(Modifier.fillMaxWidth()) {
                Column {
                    for ((index, number) in excludedNumbers.sorted().withIndex()) {
                        if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        ListItem(
                            headlineContent = {
                                Text(number, fontFamily = FontFamily.Monospace)
                            },
                            trailingContent = {
                                IconButton(onClick = { onChange(excludedNumbers - number) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "$number を削除")
                                }
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
