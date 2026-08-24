package com.example.prefixdialer

/**
 * 発信番号の書き換え判定の入り口。
 *
 * 実体は [RuleEngine] + [RuleSet] に移した。ここは呼び出し側のための薄い窓口で、
 * 「現在有効なルールセット」を解決して [RuleEngine] に委譲する役目だけを持つ。
 *
 * 設定 UI と永続化が入ったら [activeRuleSet] を保存済みの値を返すよう差し替える。
 * それまでは [Presets.default]（= MVP と同じ全種別 `0063`）を返す。
 */
object PhoneNumberPrefixer {

    /**
     * 現在有効なルールセット。
     *
     * TODO(1.0): 設定画面の実装後、SharedPreferences 等から読み出した値に差し替える。
     */
    var activeRuleSet: RuleSet = Presets.default

    /** 表示用の代表プレフィックス。複数種別で異なる場合は先頭のものを返す。 */
    val PREFIX: String
        get() = activeRuleSet.prefixes.firstOrNull() ?: ""

    /**
     * 発信すべき最終番号を返す。プレフィックス不要なら null（＝そのまま発信）。
     *
     * @param raw ダイヤラーから渡された生の番号文字列
     */
    fun buildDialNumber(raw: String?): String? =
        RuleEngine.buildDialNumber(raw, activeRuleSet)
}
