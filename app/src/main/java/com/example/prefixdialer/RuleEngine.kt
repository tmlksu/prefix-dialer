package com.example.prefixdialer

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType
import com.google.i18n.phonenumbers.Phonenumber

/**
 * [RuleSet] を 1 本の発信番号に適用する評価器。
 *
 * Android に依存しない純粋ロジックなので、ローカルユニットテストでそのまま検証できる。
 *
 * ## 評価の順序
 *
 * 1. [ProtectedNumbers] — 緊急通報・特番。**ユーザー設定より上位で、ルールでは覆せない**
 * 2. マスタースイッチ
 * 3. 二重付与・他社プレフィックス・国際発信の除外
 * 4. 日本の有効な番号かどうか
 * 5. 種別による除外（フリーダイヤル等。これもルールでは覆せない）
 * 6. ルールの評価（上から順に、最初にマッチしたものを適用）
 *
 * 1 と 5 がルールより上位にあるのが重要な点。プレフィックスやルールを UI から
 * 自由に編集できるようになっても、この 2 つはバイパスされない。
 */
object RuleEngine {

    private const val REGION_JP = "JP"
    private const val JP_COUNTRY_CODE = 81

    private val phoneUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()

    /**
     * 実際に発信すべき番号を返す。書き換え不要なら null（＝そのまま発信）。
     *
     * @param raw ダイヤラーから渡された生の番号文字列
     * @param ruleSet 適用するルール
     */
    fun buildDialNumber(raw: String?, ruleSet: RuleSet): String? {
        if (raw.isNullOrBlank()) return null

        // 1) 安全層: 緊急通報・特番・#系ダイヤルは何があっても書き換えない。
        //    ルールより上位に置くこと。詳細は ProtectedNumbers の KDoc を参照。
        if (ProtectedNumbers.isProtected(raw)) return null

        // 2) マスタースイッチ
        if (!ruleSet.enabled) return null

        // 表示用の記号を除去（先頭の + は残す）
        val cleaned = raw.trim().replace(Regex("[\\s\\-().]"), "")

        // 3) 触ってはいけない、あるいは触っても意味がないケース
        if (ruleSet.prefixes.any { cleaned.startsWith(it) }) return null   // 二重付与の防止
        if (cleaned.startsWith("010")) return null                         // 国際発信 (010 …)
        if (!cleaned.startsWith("+") && cleaned.startsWith("00")) {
            // 他社の事業者識別番号 (0033/0061/0063 …)。総務省の割当上すべて 00XY 形式なので、
            // ユーザーがどのプレフィックスを設定していてもこの 1 行で二重付与を防げる。
            return null
        }

        val number = try {
            phoneUtil.parse(cleaned, REGION_JP)
        } catch (e: NumberParseException) {
            return null                                                    // パース不能はそのまま発信
        }

        // 4) 日本の有効な番号だけを対象にする
        if (number.countryCode != JP_COUNTRY_CODE) return null             // +1 などの海外番号
        if (!phoneUtil.isValidNumber(number)) return null                  // 桁数不正など

        val national = number.toNationalDigits()

        // 5) 種別で対象外にする。フリーダイヤル(0120/0800)・ナビダイヤル(0570)・
        //    有料情報(0990) などは NumberCategory に存在しないためここで null になる。
        //    ルールで対象にすることもできない（通話が壊れる / 課金が変わるため）。
        val category = number.resolveCategory(national, ruleSet) ?: return null

        // 6) ルールを上から評価
        return when (val action = ruleSet.actionFor(category, national)) {
            null -> null                                                   // マッチなし = 付けない
            is RuleAction.PassThrough -> null
            is RuleAction.Apply -> action.prefix + action.leadingZero.apply(national) + action.suffix
        }
    }

    /** 国内表記(先頭0付き・数字のみ)へ正規化する。 */
    private fun Phonenumber.PhoneNumber.toNationalDigits(): String =
        phoneUtil.format(this, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)
            .replace(Regex("[^0-9]"), "")

    /**
     * libphonenumber の種別を [NumberCategory] に対応づける。対象外なら null。
     *
     * `FIXED_LINE_OR_MOBILE` は携帯か固定かを確定できない。事業者によっては携帯向けと
     * 固定向けでプレフィックスも先頭0の扱いも異なるため、**取り違えると課金事故になる**。
     * そこで両方の解釈で同じ結果になる場合だけ適用し、食い違う場合は素通しする。
     */
    private fun Phonenumber.PhoneNumber.resolveCategory(
        national: String,
        ruleSet: RuleSet,
    ): NumberCategory? = when (phoneUtil.getNumberType(this)) {
        PhoneNumberType.MOBILE -> NumberCategory.MOBILE
        PhoneNumberType.FIXED_LINE -> NumberCategory.FIXED_LINE
        PhoneNumberType.VOIP -> NumberCategory.VOIP
        PhoneNumberType.FIXED_LINE_OR_MOBILE -> {
            val asMobile = ruleSet.actionFor(NumberCategory.MOBILE, national)
            val asFixed = ruleSet.actionFor(NumberCategory.FIXED_LINE, national)
            if (asMobile == asFixed) NumberCategory.MOBILE else null
        }
        else -> null
    }
}
