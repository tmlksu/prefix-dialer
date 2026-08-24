package com.example.prefixdialer

/**
 * 番号書き換えルールのデータモデル。
 *
 * 「機能の ON/OFF」「プレフィックスの設定」「番号種別ごとの書き換え」「プリセット」は
 * すべてこのモデル上で表現される。UI も永続化もエクスポートもここを唯一の真実とする。
 *
 * Android に依存しないので、ローカルユニットテストでそのまま検証できる。
 */

/**
 * プレフィックスを付ける際の、国内番号の先頭 `0` の扱い。
 *
 * 事業者によって要求形式が異なるため、**ルールごとに**持つ必要がある。
 * 同じ事業者でも携帯向けと固定向けで扱いが違う場合があるので、
 * 事業者単位でもプリセット単位でもなく、ルール単位で保持する。
 */
enum class LeadingZero {
    /** 先頭 0 を残す。例: `0063` + `09012345678` */
    KEEP,

    /** 先頭 0 を落とす。例: `prefix` + `9012345678` */
    STRIP,

    /** 先頭 0 を国番号 81 に置き換える。例: `prefix` + `819012345678` */
    TO_COUNTRY_CODE,
    ;

    /**
     * 国内表記(先頭0付き)の番号に、この扱いを適用する。
     *
     * @param national 先頭 0 付きの国内表記（数字のみ）
     */
    fun apply(national: String): String = when (this) {
        KEEP -> national
        STRIP -> national.removePrefix("0")
        TO_COUNTRY_CODE -> "81" + national.removePrefix("0")
    }
}

/**
 * ルールの対象にできる番号種別。
 *
 * libphonenumber の `PhoneNumberType` のうち、プレフィックス付与が意味を持つものだけを扱う。
 * フリーダイヤル(`0120`/`0800`)・ナビダイヤル(`0570`)・有料情報(`0990`)は
 * プレフィックスを付けると通話が壊れるか課金が変わるため、**ここには存在しない**
 * （＝ユーザーがルールで対象にすることもできない）。
 */
enum class NumberCategory {
    /** 携帯電話 (`090` / `080` / `070`) */
    MOBILE,

    /** 固定電話 (`03` / `06` などの 0ABJ 番号) */
    FIXED_LINE,

    /** IP電話 (`050`) */
    VOIP,
}

/** ルールの適用条件。 */
sealed class RuleCondition {

    /** 番号種別で判定する。 */
    data class OfType(val category: NumberCategory) : RuleCondition()

    /**
     * 国内表記の先頭一致で判定する。例: `"090"`, `"0312"`。
     *
     * [OfType] より細かい粒度が要るとき（特定の番号帯だけ別扱いしたい等）に使う。
     * 上に置けば種別ルールより優先される。
     */
    data class StartsWith(val digits: String) : RuleCondition()
}

/** ルールがマッチしたときの動作。 */
sealed class RuleAction {

    /**
     * プレフィックスを付けて発信する。
     *
     * @param prefix 事業者識別番号など、番号の前に付ける文字列（例 `"0063"`）
     * @param leadingZero 元番号の先頭 0 の扱い
     * @param suffix 番号の後ろに付ける文字列。国際中継の `#` 終端など特殊用途向けで、
     *   1.0 では UI に露出しない（プリセットの互換性のためモデルにのみ保持する）
     */
    data class Apply(
        val prefix: String,
        val leadingZero: LeadingZero = LeadingZero.KEEP,
        val suffix: String = "",
    ) : RuleAction()

    /**
     * 何もせずそのまま発信する。
     *
     * 「この番号帯には絶対に付けない」を明示するために使う。
     * 上位に置くことで、下の広いルールから特定の番号帯だけを抜くことができる。
     */
    data object PassThrough : RuleAction()
}

/**
 * 1 本のルール。
 *
 * @param enabled false のルールは評価時に読み飛ばされる（削除せず一時的に無効化できる）
 */
data class DialRule(
    val condition: RuleCondition,
    val action: RuleAction,
    val enabled: Boolean = true,
)

/**
 * ルールの集合。プリセット 1 件、あるいはユーザーの現在の設定に相当する。
 *
 * [rules] は **上から順に評価し、最初にマッチしたものを適用する**。
 * マッチするルールが無ければプレフィックスを付けない（安全側）。
 *
 * @param enabled マスタースイッチ。false なら全番号が素通しになる
 */
data class RuleSet(
    val name: String,
    val rules: List<DialRule>,
    val enabled: Boolean = true,
) {

    /** このルールセットが使用しているプレフィックスの一覧（二重付与の検出に使う）。 */
    val prefixes: Set<String>
        get() = rules.mapNotNull { (it.action as? RuleAction.Apply)?.prefix }
            .filter { it.isNotEmpty() }
            .toSet()

    /** [condition] にマッチする最初の有効なルールの動作を返す。無ければ null。 */
    fun actionFor(category: NumberCategory, national: String): RuleAction? =
        rules.firstOrNull { it.enabled && it.condition.matches(category, national) }?.action

    private fun RuleCondition.matches(category: NumberCategory, national: String): Boolean =
        when (this) {
            is RuleCondition.OfType -> this.category == category
            is RuleCondition.StartsWith -> digits.isNotEmpty() && national.startsWith(digits)
        }
}
