package com.example.prefixdialer

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType

/**
 * 発信番号にプレフィックスを付けるかどうかを判定する純粋ロジック。
 *
 * Android への依存を持たないので、ローカルユニットテストでそのまま検証できる。
 * 判定は libphonenumber に委ね、+81 <-> 先頭0 の変換や特番の仕分けを任せる。
 */
object PhoneNumberPrefixer {

    /** 国内発信に付与するプレフィックス。 */
    const val PREFIX = "0063"

    private const val REGION_JP = "JP"

    private val phoneUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()

    /** プレフィックス対象とする番号種別。 */
    private val PREFIXABLE_TYPES = setOf(
        PhoneNumberType.MOBILE,
        PhoneNumberType.FIXED_LINE,
        PhoneNumberType.FIXED_LINE_OR_MOBILE,
        PhoneNumberType.VOIP, // 050 IP電話
    )

    /**
     * 発信すべき最終番号を返す。プレフィックス不要なら null（＝そのまま発信）。
     *
     * @param raw ダイヤラーから渡された生の番号文字列
     */
    fun buildDialNumber(raw: String?): String? {
        if (raw.isNullOrBlank()) return null

        // 表示用の記号を除去（先頭の + は残す）
        val cleaned = raw.trim().replace(Regex("[\\s\\-().]"), "")

        // 0) 明示的に触らないケースを先に弾く
        if (cleaned.startsWith(PREFIX)) return null              // 二重付与の防止
        if (cleaned.startsWith("010")) return null               // 国際発信（例: 010 1 …）
        if (!cleaned.startsWith("+") && cleaned.startsWith("00")) {
            return null                                          // 他社の事業者識別番号(0033/0061等)
        }

        val number = try {
            phoneUtil.parse(cleaned, REGION_JP)
        } catch (e: NumberParseException) {
            return null                                          // パース不能はそのまま発信
        }

        // 1) 日本(+81)以外は対象外（+1 などの海外番号）
        if (number.countryCode != 81) return null
        if (!phoneUtil.isValidNumber(number)) return null        // 110/119 等の短縮もここで除外

        // 2) 番号種別で除外
        //    TOLL_FREE(0120/0800), SHARED_COST(0570), PREMIUM_RATE(0990) などは付けない
        if (phoneUtil.getNumberType(number) !in PREFIXABLE_TYPES) return null

        // 3) 国内表記(先頭0付き)へ正規化してプレフィックスを付与
        val national = phoneUtil
            .format(number, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)
            .replace(Regex("[^0-9]"), "")

        return PREFIX + national
    }
}
