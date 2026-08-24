package io.github.tmlksu.prefixdialer.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.tmlksu.prefixdialer.LeadingZero
import io.github.tmlksu.prefixdialer.NumberCategory
import io.github.tmlksu.prefixdialer.Presets
import io.github.tmlksu.prefixdialer.R
import io.github.tmlksu.prefixdialer.RuleSet
import io.github.tmlksu.prefixdialer.SkipReason

/**
 * ドメインの型と表示文言の対応づけ。
 *
 * この対応を UI 層に置くことで、[NumberCategory] / [SkipReason] / [LeadingZero] は
 * Android に依存せず純 JVM のユニットテストで扱える状態を保てる。
 *
 * `when` に `else` を書かないのは意図的で、enum に値を足したときにコンパイルエラーで
 * 気づけるようにするため（文言の付け忘れを防ぐ）。
 */

@get:StringRes
val NumberCategory.labelRes: Int
    get() = when (this) {
        NumberCategory.MOBILE -> R.string.category_mobile
        NumberCategory.FIXED_LINE -> R.string.category_fixed_line
        NumberCategory.VOIP -> R.string.category_voip
    }

@get:StringRes
val LeadingZero.labelRes: Int
    get() = when (this) {
        LeadingZero.KEEP -> R.string.leading_zero_keep
        LeadingZero.STRIP -> R.string.leading_zero_strip
        LeadingZero.TO_COUNTRY_CODE -> R.string.leading_zero_country_code
    }

@get:StringRes
val SkipReason.labelRes: Int
    get() = when (this) {
        SkipReason.PROTECTED_NUMBER -> R.string.skip_protected_number
        SkipReason.MASTER_OFF -> R.string.skip_master_off
        SkipReason.ROAMING -> R.string.skip_roaming
        SkipReason.DISABLED_LINE -> R.string.skip_disabled_line
        SkipReason.EXCLUDED -> R.string.skip_excluded
        SkipReason.ALREADY_PREFIXED -> R.string.skip_already_prefixed
        SkipReason.CARRIER_PREFIX -> R.string.skip_carrier_prefix
        SkipReason.INTERNATIONAL -> R.string.skip_international
        SkipReason.UNPARSEABLE -> R.string.skip_unparseable
        SkipReason.NOT_JAPAN -> R.string.skip_not_japan
        SkipReason.INVALID -> R.string.skip_invalid
        SkipReason.NOT_TARGET_TYPE -> R.string.skip_not_target_type
        SkipReason.AMBIGUOUS_TYPE -> R.string.skip_ambiguous_type
        SkipReason.NO_RULE -> R.string.skip_no_rule
        SkipReason.PASS_THROUGH -> R.string.skip_pass_through
        SkipReason.EMPTY -> R.string.skip_empty
    }

/**
 * ルールの効き方を具体例で見せるためのサンプル番号。
 *
 * 先頭 0 の扱いは言葉だけでは伝わりにくく、間違えると発信が失敗するか
 * 意図しない料金になる。設定画面では必ず適用結果を並べて表示する。
 */
internal fun NumberCategory.sampleNumber(): String = when (this) {
    NumberCategory.MOBILE -> "09012345678"
    NumberCategory.FIXED_LINE -> "0312345678"
    NumberCategory.VOIP -> "05012345678"
}

/**
 * 画面に出すルールセット名。
 *
 * 組み込みプリセットの名前は永続化される固定文字列なので、表示時に端末の言語へ
 * 置き換える。ユーザーが自分で付けた名前はそのまま出す。
 */
@Composable
fun RuleSet.displayName(): String = when (name) {
    Presets.CUSTOM_ID -> stringResource(R.string.preset_custom)
    else -> name
}
