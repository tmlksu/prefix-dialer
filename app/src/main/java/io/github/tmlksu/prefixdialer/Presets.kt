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
 * 4. **出典と検証状況を KDoc に書き残す。** 実機未確認のものはその旨を明記する
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
     * 楽天でんわ。
     *
     * 全種別とも `003768` + 先頭 0 を残した国内表記。
     *
     * 出典:
     *  - 公式「楽天でんわの仕組み」<https://denwa.rakuten.co.jp/about.html>
     *    — 相手の番号に「0037-68-」を付けて発信する、と記載
     *  - ITmedia Mobile の実機記事（2016-03-17）
     *    <https://www.itmedia.co.jp/mobile/articles/1603/17/news008.html>
     *    — 「090-8＊＊＊-＊＊＊＊」をダイヤルすると「0037-68-」が頭に付いた状態で
     *    発信される、という具体例。ここから先頭 0 を残す形式であることを確認した
     *
     * **実機での発信・明細確認は未実施**（DECISIONS.md H-06）。
     * 公式資料と実機記事から形式は確定しているが、課金に直結するため
     * 実際に使う前に 1 回は自分の回線で確かめること。
     *
     * なお楽天でんわは家族間や同一キャリア間の無料通話が有料になる。
     * その相手は「除外する番号」に登録して使うことを想定している。
     */
    val rakutenDenwa = RuleSet(
        name = "楽天でんわ",
        rules = listOf(
            DialRule(
                condition = RuleCondition.OfType(NumberCategory.MOBILE),
                action = RuleAction.Apply("003768", LeadingZero.KEEP),
            ),
            DialRule(
                condition = RuleCondition.OfType(NumberCategory.FIXED_LINE),
                action = RuleAction.Apply("003768", LeadingZero.KEEP),
            ),
            DialRule(
                condition = RuleCondition.OfType(NumberCategory.VOIP),
                action = RuleAction.Apply("003768", LeadingZero.KEEP),
            ),
        ),
    )

    /**
     * 組み込みプリセットの識別子。
     *
     * [RuleSet.name] は設定として永続化され、エクスポートしたファイルにも入る。
     * そのためロケールに依存しない固定文字列にする。画面に出す名前は UI 層が
     * `RuleSet.displayName()` で解決する（端末の言語に追従させるため）。
     */
    const val CUSTOM_ID = "Custom"

    /**
     * 空のテンプレート。ユーザーが自分の契約に合わせて設定するための出発点。
     *
     * ルールが空なのでマッチせず、この状態では 1 件もプレフィックスが付かない
     * （＝設定し忘れても勝手に課金が変わることはない）。
     */
    val custom = RuleSet(name = CUSTOM_ID, rules = emptyList())

    /** UI のプリセット選択に並べる一覧。 */
    val all: List<RuleSet> = listOf(gCall, rakutenDenwa, custom)

    /**
     * 初期状態のルールセット。
     *
     * 現在の MVP の挙動（全種別に `0063`）をそのまま引き継ぐ。
     * 設定 UI が入ったら、初回起動時にユーザーへ選ばせる形に置き換える。
     */
    val default: RuleSet = gCall
}
