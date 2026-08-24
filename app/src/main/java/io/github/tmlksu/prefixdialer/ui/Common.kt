package io.github.tmlksu.prefixdialer.ui

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier

/** 一覧の行をタップ可能にする。行全体を対象にしてタップ範囲を広く取る。 */
internal fun Modifier.clickableRow(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)
