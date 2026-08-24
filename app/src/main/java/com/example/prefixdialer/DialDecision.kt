package com.example.prefixdialer

/**
 * 1 本の発信に対する判定結果。
 *
 * [RuleEngine] は「書き換え後の番号」だけでなく、**書き換えなかった理由**も返す。
 * ユーザーが「なぜプレフィックスが付かないのか」を自力で確認できるようにするため。
 * これが無いと、機能が効いていないことに気づけないか、気づいても原因が分からない。
 */
sealed class DialDecision {

    /** 番号を書き換えて発信する。 */
    data class Rewrite(
        val dialNumber: String,
        val category: NumberCategory,
    ) : DialDecision()

    /** そのまま発信する。 */
    data class Skip(val reason: SkipReason) : DialDecision()

    /** 書き換えた場合の番号。書き換えないなら null。 */
    val dialNumberOrNull: String? get() = (this as? Rewrite)?.dialNumber
}

/**
 * プレフィックスを付けなかった理由。
 *
 * 表示文言は [description] に持たせる。ユーザーが読んで次の行動が分かる粒度にする
 * （「対象外」だけでは何も分からない）。
 */
enum class SkipReason(val description: String) {

    /** 緊急通報・特番・#系ダイヤルなど、設定に関わらず保護される番号。 */
    PROTECTED_NUMBER("緊急通報・特番のため（設定に関わらず保護されます）"),

    /** マスタースイッチが OFF。 */
    MASTER_OFF("機能が停止中のため"),

    /** ローミング中で、ローミング時の停止が有効。 */
    ROAMING("ローミング中のため"),

    /** この回線ではプレフィックスを付けない設定。 */
    DISABLED_LINE("この回線では付けない設定のため"),

    /** 除外リストに登録されている番号。 */
    EXCLUDED("除外リストに登録されているため"),

    /** すでに設定中のプレフィックスが付いている。 */
    ALREADY_PREFIXED("すでにプレフィックスが付いているため"),

    /** 他社の事業者識別番号(00XY)で始まっている。 */
    CARRIER_PREFIX("他社の事業者識別番号が付いているため"),

    /** 国際発信(010)。 */
    INTERNATIONAL("国際発信のため"),

    /** 電話番号として解釈できなかった。 */
    UNPARSEABLE("番号として解釈できなかったため"),

    /** 日本の番号ではない。 */
    NOT_JAPAN("日本の番号ではないため"),

    /** 桁数などが電話番号として妥当でない。 */
    INVALID("有効な電話番号ではないため"),

    /** フリーダイヤル・ナビダイヤル・有料情報など、対象にできない種別。 */
    NOT_TARGET_TYPE("フリーダイヤル等、対象にできない種別のため"),

    /** 携帯か固定かを確定できず、種別によって設定が異なる。 */
    AMBIGUOUS_TYPE("携帯か固定か判別できず、種別で設定が異なるため"),

    /** マッチするルールが無い。 */
    NO_RULE("この種別に有効なルールが無いため"),

    /** 「付けない」ルールにマッチした。 */
    PASS_THROUGH("「付けない」ルールに一致したため"),

    /** 番号が空。 */
    EMPTY("番号が空のため"),
}
