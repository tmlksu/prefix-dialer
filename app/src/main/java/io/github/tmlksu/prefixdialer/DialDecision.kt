package io.github.tmlksu.prefixdialer

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
 * ユーザーが読んで次の行動が分かる粒度に分ける（「対象外」だけでは何も分からない）。
 *
 * 表示文言はここに持たせない。この enum は Android 非依存に保ち、純 JVM の
 * ユニットテストで扱えるようにする。文言との対応づけは UI 層（`SkipReason.describe`）
 * が担当し、リソースとして多言語化する。
 */
enum class SkipReason {

    /** 緊急通報・特番・#系ダイヤルなど、設定に関わらず保護される番号。 */
    PROTECTED_NUMBER,

    /** マスタースイッチが OFF。 */
    MASTER_OFF,

    /** ローミング中で、ローミング時の停止が有効。 */
    ROAMING,

    /** この回線ではプレフィックスを付けない設定。 */
    DISABLED_LINE,

    /** 除外リストに登録されている番号。 */
    EXCLUDED,

    /** すでに設定中のプレフィックスが付いている。 */
    ALREADY_PREFIXED,

    /** 他社の事業者識別番号(00XY)で始まっている。 */
    CARRIER_PREFIX,

    /** 国際発信(010)。 */
    INTERNATIONAL,

    /** 電話番号として解釈できなかった。 */
    UNPARSEABLE,

    /** 日本の番号ではない。 */
    NOT_JAPAN,

    /** 桁数などが電話番号として妥当でない。 */
    INVALID,

    /** フリーダイヤル・ナビダイヤル・有料情報など、対象にできない種別。 */
    NOT_TARGET_TYPE,

    /** 携帯か固定かを確定できず、種別によって設定が異なる。 */
    AMBIGUOUS_TYPE,

    /** マッチするルールが無い。 */
    NO_RULE,

    /** 「付けない」ルールにマッチした。 */
    PASS_THROUGH,

    /** 番号が空。 */
    EMPTY,
}
