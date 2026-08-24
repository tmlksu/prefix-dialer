package io.github.tmlksu.prefixdialer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 判定理由のテスト。
 *
 * 理由が正しくないと、発信記録が「なぜ付かないのか」の答えにならない。
 * 「対象外」で丸めず、原因ごとに違う理由が返ることを確認する。
 */
class DialDecisionTest {

    private val base = Settings(ruleSet = Presets.gCall)

    private fun reason(
        raw: String,
        settings: Settings = base,
        phoneAccountId: String? = null,
        isRoaming: Boolean = false,
    ): SkipReason? =
        (RuleEngine.evaluate(raw, settings, phoneAccountId, isRoaming) as? DialDecision.Skip)?.reason

    @Test
    fun `書き換えた場合は番号と種別を返す`() {
        val decision = RuleEngine.evaluate("09012345678", base)
        assertEquals(
            DialDecision.Rewrite("006309012345678", NumberCategory.MOBILE),
            decision,
        )
        assertEquals("006309012345678", decision.dialNumberOrNull)
    }

    @Test
    fun `固定電話とIP電話の種別も返る`() {
        assertEquals(
            NumberCategory.FIXED_LINE,
            (RuleEngine.evaluate("0312345678", base) as DialDecision.Rewrite).category,
        )
        assertEquals(
            NumberCategory.VOIP,
            (RuleEngine.evaluate("05012345678", base) as DialDecision.Rewrite).category,
        )
    }

    @Test
    fun `緊急通報と特番はPROTECTED_NUMBER`() {
        for (n in listOf("110", "119", "118", "117", "#7119", "184090123456789")) {
            assertEquals(n, SkipReason.PROTECTED_NUMBER, reason(n))
        }
    }

    @Test
    fun `マスタースイッチOFFはMASTER_OFF`() {
        val off = base.copy(ruleSet = Presets.gCall.copy(enabled = false))
        assertEquals(SkipReason.MASTER_OFF, reason("09012345678", off))
    }

    @Test
    fun `ローミング中はROAMING`() {
        assertEquals(SkipReason.ROAMING, reason("09012345678", isRoaming = true))
    }

    @Test
    fun `無効化した回線はDISABLED_LINE`() {
        val settings = base.copy(disabledPhoneAccountIds = setOf("sim2"))
        assertEquals(SkipReason.DISABLED_LINE, reason("09012345678", settings, "sim2"))
    }

    @Test
    fun `除外リストの番号はEXCLUDED`() {
        val settings = base.copy(excludedNumbers = setOf("09012345678"))
        assertEquals(SkipReason.EXCLUDED, reason("09012345678", settings))
        assertEquals(SkipReason.EXCLUDED, reason("090-1234-5678", settings))
        assertEquals(SkipReason.EXCLUDED, reason("+819012345678", settings))
    }

    @Test
    fun `二重付与はALREADY_PREFIXED`() {
        assertEquals(SkipReason.ALREADY_PREFIXED, reason("006309012345678"))
    }

    @Test
    fun `他社の事業者識別番号はCARRIER_PREFIX`() {
        assertEquals(SkipReason.CARRIER_PREFIX, reason("003309012345678"))
    }

    @Test
    fun `国際発信はINTERNATIONAL`() {
        assertEquals(SkipReason.INTERNATIONAL, reason("010112125551234"))
    }

    @Test
    fun `海外の番号はNOT_JAPAN`() {
        assertEquals(SkipReason.NOT_JAPAN, reason("+12125551234"))
    }

    @Test
    fun `フリーダイヤル等はNOT_TARGET_TYPE`() {
        for (n in listOf("0120444444", "08001234567", "0570000123", "0990123456")) {
            assertEquals(n, SkipReason.NOT_TARGET_TYPE, reason(n))
        }
    }

    @Test
    fun `ルールが無い種別はNO_RULE`() {
        val mobileOnly = RuleSet(
            "mobile only",
            listOf(
                DialRule(
                    RuleCondition.OfType(NumberCategory.MOBILE),
                    RuleAction.Apply("0063", LeadingZero.KEEP),
                ),
            ),
        )
        assertEquals(SkipReason.NO_RULE, reason("0312345678", base.copy(ruleSet = mobileOnly)))
    }

    @Test
    fun `付けないルールにマッチした場合はPASS_THROUGH`() {
        val ruleSet = RuleSet(
            "test",
            listOf(
                DialRule(RuleCondition.StartsWith("090"), RuleAction.PassThrough),
                DialRule(
                    RuleCondition.OfType(NumberCategory.MOBILE),
                    RuleAction.Apply("0063", LeadingZero.KEEP),
                ),
            ),
        )
        assertEquals(SkipReason.PASS_THROUGH, reason("09012345678", base.copy(ruleSet = ruleSet)))
    }

    @Test
    fun `プレフィックスが未入力のルールはNO_RULE扱い`() {
        // 設定 UI で入力途中の状態。空文字を前置しても何も起きないので、
        // 「付いた」と誤解させずスキップ扱いにする。
        val ruleSet = RuleSet(
            "入力途中",
            listOf(
                DialRule(
                    RuleCondition.OfType(NumberCategory.MOBILE),
                    RuleAction.Apply("", LeadingZero.KEEP),
                ),
            ),
        )
        assertEquals(SkipReason.NO_RULE, reason("09012345678", base.copy(ruleSet = ruleSet)))
    }

    @Test
    fun `空の番号はEMPTY`() {
        assertEquals(SkipReason.EMPTY, reason(""))
        assertEquals(SkipReason.EMPTY, reason("   "))
    }

    @Test
    fun `理由の名前が重複していない`() {
        // 文言は UI 層のリソースに持たせているのでここでは検証しない
        // （SkipReason は Android 非依存に保つ）。
        // 表示文言の付け忘れは ui/Labels.kt の when が網羅性チェックで捕まえる。
        assertEquals(SkipReason.entries.size, SkipReason.entries.map { it.name }.toSet().size)
    }
}
