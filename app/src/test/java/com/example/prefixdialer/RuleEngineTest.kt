package com.example.prefixdialer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ルールモデルと評価器のテスト。
 *
 * 実発信は一切行わない純粋関数のテスト。
 */
class RuleEngineTest {

    private fun ruleSet(vararg rules: DialRule, enabled: Boolean = true) =
        RuleSet(name = "test", rules = rules.toList(), enabled = enabled)

    private fun dial(raw: String, rs: RuleSet) = RuleEngine.buildDialNumber(raw, rs)

    private val MOBILE = "09012345678"
    private val FIXED = "0312345678"
    private val VOIP = "05012345678"

    // ------------------------------------------------------------------
    // 種別ごとに prefix と先頭0の扱いを変えられること
    // （事業者によって携帯向けと固定向けで形式が違うため、1.0 の中核要件）
    // ------------------------------------------------------------------

    @Test
    fun `携帯と固定で異なるprefixと先頭0の扱いを設定できる`() {
        val rs = ruleSet(
            DialRule(
                RuleCondition.OfType(NumberCategory.MOBILE),
                RuleAction.Apply("0088", LeadingZero.STRIP),
            ),
            DialRule(
                RuleCondition.OfType(NumberCategory.FIXED_LINE),
                RuleAction.Apply("0077", LeadingZero.KEEP),
            ),
        )

        assertEquals("0088" + "9012345678", dial(MOBILE, rs))
        assertEquals("0077" + "0312345678", dial(FIXED, rs))
        assertNull("ルールの無いIP電話には付かない", dial(VOIP, rs))
    }

    @Test
    fun `先頭0の扱い3種がそれぞれ正しく適用される`() {
        fun withZero(z: LeadingZero) = ruleSet(
            DialRule(RuleCondition.OfType(NumberCategory.MOBILE), RuleAction.Apply("0063", z)),
        )

        assertEquals("0063" + "09012345678", dial(MOBILE, withZero(LeadingZero.KEEP)))
        assertEquals("0063" + "9012345678", dial(MOBILE, withZero(LeadingZero.STRIP)))
        assertEquals("0063" + "819012345678", dial(MOBILE, withZero(LeadingZero.TO_COUNTRY_CODE)))
    }

    @Test
    fun `suffixが番号の末尾に付く`() {
        val rs = ruleSet(
            DialRule(
                RuleCondition.OfType(NumberCategory.MOBILE),
                RuleAction.Apply("0063", LeadingZero.KEEP, suffix = "#"),
            ),
        )
        assertEquals("0063" + "09012345678" + "#", dial(MOBILE, rs))
    }

    // ------------------------------------------------------------------
    // 評価順序
    // ------------------------------------------------------------------

    @Test
    fun `ルールは上から評価され最初にマッチしたものが使われる`() {
        val rs = ruleSet(
            DialRule(RuleCondition.StartsWith("090"), RuleAction.Apply("1111", LeadingZero.KEEP)),
            DialRule(RuleCondition.OfType(NumberCategory.MOBILE), RuleAction.Apply("2222", LeadingZero.KEEP)),
        )
        assertEquals("上の StartsWith が勝つ", "1111" + "09012345678", dial(MOBILE, rs))
        assertEquals("080 は下の種別ルールに落ちる", "2222" + "08012345678", dial("08012345678", rs))
    }

    @Test
    fun `PassThroughで特定の番号帯だけを除外できる`() {
        val rs = ruleSet(
            DialRule(RuleCondition.StartsWith("0312"), RuleAction.PassThrough),
            DialRule(RuleCondition.OfType(NumberCategory.FIXED_LINE), RuleAction.Apply("0063", LeadingZero.KEEP)),
        )
        assertNull("除外した番号帯には付かない", dial("0312345678", rs))
        assertEquals("他の固定電話には付く", "0063" + "0612345678", dial("0612345678", rs))
    }

    @Test
    fun `マッチするルールが無ければ付けない`() {
        assertNull(dial(MOBILE, ruleSet()))
        assertNull(dial(MOBILE, Presets.custom))
    }

    // ------------------------------------------------------------------
    // スイッチ類
    // ------------------------------------------------------------------

    @Test
    fun `マスタースイッチがOFFなら全番号が素通しになる`() {
        val rs = ruleSet(
            DialRule(RuleCondition.OfType(NumberCategory.MOBILE), RuleAction.Apply("0063", LeadingZero.KEEP)),
            enabled = false,
        )
        assertNull(dial(MOBILE, rs))
        assertNull(dial(FIXED, rs))
    }

    @Test
    fun `無効化されたルールは読み飛ばされる`() {
        val rs = ruleSet(
            DialRule(RuleCondition.StartsWith("090"), RuleAction.Apply("1111", LeadingZero.KEEP), enabled = false),
            DialRule(RuleCondition.OfType(NumberCategory.MOBILE), RuleAction.Apply("2222", LeadingZero.KEEP)),
        )
        assertEquals("2222" + "09012345678", dial(MOBILE, rs))
    }

    // ------------------------------------------------------------------
    // 二重付与の防止
    // ------------------------------------------------------------------

    @Test
    fun `設定したprefixで始まる番号には二重に付けない`() {
        val rs = ruleSet(
            DialRule(RuleCondition.OfType(NumberCategory.MOBILE), RuleAction.Apply("0063", LeadingZero.KEEP)),
        )
        assertNull(dial("0063" + "09012345678", rs))
    }

    @Test
    fun `他社の事業者識別番号で始まる番号には付けない`() {
        val rs = ruleSet(
            DialRule(RuleCondition.OfType(NumberCategory.MOBILE), RuleAction.Apply("0063", LeadingZero.KEEP)),
            DialRule(RuleCondition.OfType(NumberCategory.FIXED_LINE), RuleAction.Apply("0063", LeadingZero.KEEP)),
        )
        // 事業者識別番号は総務省の割当上すべて 00XY 形式
        for (n in listOf("003309012345678", "006109012345678", "00376809012345678")) {
            assertNull("$n に付いてしまった", dial(n, rs))
        }
    }

    // ------------------------------------------------------------------
    // ルールで覆せない除外（安全層）
    // ------------------------------------------------------------------

    @Test
    fun `フリーダイヤル等はStartsWithルールを書いても対象にできない`() {
        val rs = ruleSet(
            DialRule(RuleCondition.StartsWith("0120"), RuleAction.Apply("0063", LeadingZero.KEEP)),
            DialRule(RuleCondition.StartsWith("0800"), RuleAction.Apply("0063", LeadingZero.KEEP)),
            DialRule(RuleCondition.StartsWith("0570"), RuleAction.Apply("0063", LeadingZero.KEEP)),
            DialRule(RuleCondition.StartsWith("0990"), RuleAction.Apply("0063", LeadingZero.KEEP)),
        )
        for (n in listOf("0120444444", "08001234567", "0570000123", "0990123456")) {
            assertNull("$n が書き換えられた", dial(n, rs))
        }
    }

    /**
     * 攻撃的なルールセットでも緊急通報が書き換えられないこと。
     *
     * ユーザーがどんな設定をしても [ProtectedNumbers] は迂回できない、という不変条件。
     */
    @Test
    fun `どんなルールを書いても緊急通報と特番は書き換えられない`() {
        val hostile = ruleSet(
            DialRule(RuleCondition.StartsWith("1"), RuleAction.Apply("0063", LeadingZero.KEEP)),
            DialRule(RuleCondition.StartsWith("11"), RuleAction.Apply("0063", LeadingZero.STRIP)),
            DialRule(RuleCondition.StartsWith("110"), RuleAction.Apply("0063", LeadingZero.KEEP)),
            DialRule(RuleCondition.StartsWith("0"), RuleAction.Apply("0063", LeadingZero.KEEP)),
            DialRule(RuleCondition.OfType(NumberCategory.MOBILE), RuleAction.Apply("0063", LeadingZero.KEEP)),
            DialRule(RuleCondition.OfType(NumberCategory.FIXED_LINE), RuleAction.Apply("0063", LeadingZero.KEEP)),
            DialRule(RuleCondition.OfType(NumberCategory.VOIP), RuleAction.Apply("0063", LeadingZero.KEEP)),
        )

        val leaked = ProtectedNumbers.DOCUMENTED_SPECIAL_NUMBERS.keys
            .mapNotNull { n -> dial(n, hostile)?.let { n to it } }
        assertTrue(
            "特番が書き換えられた: " + leaked.joinToString { "${it.first} -> ${it.second}" },
            leaked.isEmpty(),
        )

        val leaked3 = (0..999).map { it.toString().padStart(3, '0') }
            .mapNotNull { n -> dial(n, hostile)?.let { n to it } }
        assertTrue(
            "3桁番号が書き換えられた: " + leaked3.joinToString { "${it.first} -> ${it.second}" },
            leaked3.isEmpty(),
        )
    }

    // ------------------------------------------------------------------
    // 海外番号
    // ------------------------------------------------------------------

    @Test
    fun `海外の番号には付けない`() {
        val rs = ruleSet(
            DialRule(RuleCondition.OfType(NumberCategory.MOBILE), RuleAction.Apply("0063", LeadingZero.KEEP)),
            DialRule(RuleCondition.OfType(NumberCategory.FIXED_LINE), RuleAction.Apply("0063", LeadingZero.KEEP)),
        )
        for (n in listOf("+12125551234", "+442071234567", "+8613800138000")) {
            assertNull("$n が書き換えられた", dial(n, rs))
        }
    }

    @Test
    fun `国際表記の日本番号は国内表記に正規化して付ける`() {
        val rs = ruleSet(
            DialRule(RuleCondition.OfType(NumberCategory.MOBILE), RuleAction.Apply("0063", LeadingZero.KEEP)),
        )
        assertEquals("0063" + "09012345678", dial("+819012345678", rs))
    }

    // ------------------------------------------------------------------
    // RuleSet のユーティリティ
    // ------------------------------------------------------------------

    @Test
    fun `prefixes は使用中のprefixをすべて返す`() {
        val rs = ruleSet(
            DialRule(RuleCondition.OfType(NumberCategory.MOBILE), RuleAction.Apply("0088", LeadingZero.STRIP)),
            DialRule(RuleCondition.OfType(NumberCategory.FIXED_LINE), RuleAction.Apply("0077", LeadingZero.KEEP)),
            DialRule(RuleCondition.OfType(NumberCategory.VOIP), RuleAction.PassThrough),
        )
        assertEquals(setOf("0088", "0077"), rs.prefixes)
    }
}
