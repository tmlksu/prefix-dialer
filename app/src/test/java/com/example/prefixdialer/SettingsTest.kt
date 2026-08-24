package com.example.prefixdialer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 設定を考慮した発信判定のテスト。 */
class SettingsTest {

    private val base = Settings(ruleSet = Presets.gCall)
    private val MOBILE = "09012345678"

    private fun dial(
        raw: String,
        settings: Settings = base,
        subscriptionId: Int? = null,
        isRoaming: Boolean = false,
    ) = RuleEngine.buildDialNumber(raw, settings, subscriptionId, isRoaming)

    @Test
    fun `既定の設定では通常の番号にプレフィックスが付く`() {
        assertEquals("0063$MOBILE", dial(MOBILE))
    }

    // ------------------------------------------------------------------
    // 既定値（DECISIONS.md D-12 / D-14）
    // ------------------------------------------------------------------

    @Test
    fun `履歴書き換えは既定でOFF`() {
        assertFalse(Settings().callLogRewriteEnabled)
    }

    @Test
    fun `ローミング中の停止は既定でON`() {
        assertTrue(Settings().disableWhileRoaming)
    }

    @Test
    fun `既定では全SIMで有効`() {
        assertTrue(Settings().disabledSubscriptionIds.isEmpty())
        assertTrue(Settings().isEnabledForSubscription(1))
        assertTrue(Settings().isEnabledForSubscription(99))
    }

    // ------------------------------------------------------------------
    // ローミング
    // ------------------------------------------------------------------

    @Test
    fun `ローミング中はプレフィックスが付かない`() {
        assertNull(dial(MOBILE, isRoaming = true))
    }

    @Test
    fun `ローミング停止を解除すればローミング中でも付く`() {
        val settings = base.copy(disableWhileRoaming = false)
        assertEquals("0063$MOBILE", dial(MOBILE, settings, isRoaming = true))
    }

    // ------------------------------------------------------------------
    // SIM ごとの制御
    // ------------------------------------------------------------------

    @Test
    fun `無効化したSIMからの発信には付かない`() {
        val settings = base.copy(disabledSubscriptionIds = setOf(2))
        assertNull(dial(MOBILE, settings, subscriptionId = 2))
        assertEquals("0063$MOBILE", dial(MOBILE, settings, subscriptionId = 1))
    }

    @Test
    fun `SIMが不明な場合は適用する`() {
        // 端末やロールの制約で購読 ID が取れないことがある。現行の挙動を維持する。
        val settings = base.copy(disabledSubscriptionIds = setOf(2))
        assertEquals("0063$MOBILE", dial(MOBILE, settings, subscriptionId = null))
    }

    @Test
    fun `マスタースイッチOFFはSIM指定より優先される`() {
        val settings = base.copy(ruleSet = Presets.gCall.copy(enabled = false))
        assertNull(dial(MOBILE, settings, subscriptionId = 1))
    }

    // ------------------------------------------------------------------
    // 個別除外
    // ------------------------------------------------------------------

    @Test
    fun `除外した番号には付かない`() {
        val settings = base.copy(excludedNumbers = setOf(MOBILE))
        assertNull(dial(MOBILE, settings))
        assertEquals("006308012345678", dial("08012345678", settings))
    }

    @Test
    fun `除外は表記ゆれを吸収する`() {
        val settings = base.copy(excludedNumbers = setOf("0312345678"))
        for (raw in listOf("0312345678", "03-1234-5678", "03 1234 5678", "(03)1234-5678", "+81312345678")) {
            assertNull("$raw が除外されなかった", dial(raw, settings))
        }
    }

    @Test
    fun `除外はルールより優先される`() {
        val settings = base.copy(
            ruleSet = RuleSet(
                "test",
                listOf(DialRule(RuleCondition.StartsWith("090"), RuleAction.Apply("0063", LeadingZero.KEEP))),
            ),
            excludedNumbers = setOf(MOBILE),
        )
        assertNull(dial(MOBILE, settings))
    }

    @Test
    fun `isExcluded は数字のみに正規化して照合する`() {
        val settings = Settings(excludedNumbers = setOf("0312345678"))
        assertTrue(settings.isExcluded("03-1234-5678"))
        assertTrue(settings.isExcluded("0312345678"))
        assertFalse(settings.isExcluded("0312345679"))
        assertFalse(settings.isExcluded(null))
        assertFalse(settings.isExcluded(""))
    }

    // ------------------------------------------------------------------
    // 安全層は設定に関わらず貫通しない
    // ------------------------------------------------------------------

    @Test
    fun `どの設定でも緊急通報と特番は保護される`() {
        val variants = listOf(
            base,
            base.copy(disableWhileRoaming = false),
            base.copy(callLogRewriteEnabled = true),
            base.copy(excludedNumbers = emptySet()),
            Settings(ruleSet = Presets.gCall.copy(enabled = true)),
        )
        for (settings in variants) {
            for (roaming in listOf(false, true)) {
                val leaked = ProtectedNumbers.DOCUMENTED_SPECIAL_NUMBERS.keys
                    .mapNotNull { n -> dial(n, settings, isRoaming = roaming)?.let { n to it } }
                assertTrue(leaked.joinToString { "${it.first} -> ${it.second}" }, leaked.isEmpty())
            }
        }
    }
}
