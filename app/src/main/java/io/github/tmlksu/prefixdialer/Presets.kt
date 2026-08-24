package io.github.tmlksu.prefixdialer

/**
 * 事業者プリセット。
 *
 * ## 収録の方針
 *
 * **裏取りできたものだけを収録する。** 事業者識別番号や先頭 0 の扱いを間違えると、
 * 発信が失敗するだけでなく、意図しない料金体系で課金される。推測で追加しない。
 *
 * 未収録の事業者は [custom] を起点にユーザーが自分で設定する。
 * プリセットは「よく使う設定の出発点」であって、網羅を目指すものではない。
 *
 * ## プリセットを追加するときは
 *
 * 1. 事業者の公式資料でダイヤル方法を確認する（携帯向けと固定向けで
 *    プレフィックスや先頭 0 の扱いが違う事業者があるため、両方を個別に確認すること）
 * 2. 実機で発信し、実際に接続されること・明細で意図した料金になることを確認する
 * 3. [PresetsTest] に期待値を追加する
 */
object Presets {

    /**
     * G-Call。
     *
     * 出典: 実機（Samsung Galaxy S25 / Android 15）での発信・接続確認済み（2026-08-24）。
     * 携帯・固定・IP電話のいずれも `0063` + 先頭 0 を残した国内表記。
     */
    val gCall = RuleSet(
        name = "G-Call",
        rules = listOf(
            DialRule(
                condition = RuleCondition.OfType(NumberCategory.MOBILE),
                action = RuleAction.Apply("0063", LeadingZero.KEEP),
            ),
            DialRule(
                condition = RuleCondition.OfType(NumberCategory.FIXED_LINE),
                action = RuleAction.Apply("0063", LeadingZero.KEEP),
            ),
            DialRule(
                condition = RuleCondition.OfType(NumberCategory.VOIP),
                action = RuleAction.Apply("0063", LeadingZero.KEEP),
            ),
        ),
    )

    /**
     * 空のテンプレート。ユーザーが自分の契約に合わせて設定するための出発点。
     *
     * ルールが空なのでマッチせず、この状態では 1 件もプレフィックスが付かない
     * （＝設定し忘れても勝手に課金が変わることはない）。
     */
    val custom = RuleSet(name = "カスタム", rules = emptyList())

    /** UI のプリセット選択に並べる一覧。 */
    val all: List<RuleSet> = listOf(gCall, custom)

    /**
     * 初期状態のルールセット。
     *
     * 現在の MVP の挙動（全種別に `0063`）をそのまま引き継ぐ。
     * 設定 UI が入ったら、初回起動時にユーザーへ選ばせる形に置き換える。
     */
    val default: RuleSet = gCall
}
