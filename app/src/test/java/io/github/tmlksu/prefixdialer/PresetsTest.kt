package io.github.tmlksu.prefixdialer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * プリセットの期待値。
 *
 * プリセットの値を変更したらここも必ず更新すること。
 * 事業者識別番号や先頭 0 の扱いの誤りは、そのままユーザーの課金事故になる。
 */
class PresetsTest {

    @Test
    fun `G-Callは全種別に0063を先頭0を残して付ける`() {
        val rs = Presets.gCall
        assertEquals("0063" + "09012345678", RuleEngine.buildDialNumber("09012345678", rs))
        assertEquals("0063" + "0312345678", RuleEngine.buildDialNumber("0312345678", rs))
        assertEquals("0063" + "05012345678", RuleEngine.buildDialNumber("05012345678", rs))
    }

    @Test
    fun `楽天でんわは全種別に003768を先頭0を残して付ける`() {
        val rs = Presets.rakutenDenwa
        assertEquals("003768" + "09012345678", RuleEngine.buildDialNumber("09012345678", rs))
        assertEquals("003768" + "0312345678", RuleEngine.buildDialNumber("0312345678", rs))
        assertEquals("003768" + "05012345678", RuleEngine.buildDialNumber("05012345678", rs))
    }

    @Test
    fun `楽天でんわのプレフィックスが付いた番号には二重に付けない`() {
        // 003768 は 00XY 形式なので、他社プレフィックスの検出でも二重付与が防がれる
        assertNull(RuleEngine.buildDialNumber("00376809012345678", Presets.rakutenDenwa))
        assertNull(RuleEngine.buildDialNumber("00376809012345678", Presets.gCall))
    }

    @Test
    fun `プリセットの名前が重複していない`() {
        assertEquals(Presets.all.size, Presets.all.map { it.name }.toSet().size)
    }

    @Test
    fun `カスタムは初期状態では1件も書き換えない`() {
        for (n in listOf("09012345678", "0312345678", "05012345678")) {
            assertNull(RuleEngine.buildDialNumber(n, Presets.custom))
        }
    }

    @Test
    fun `全プリセットで緊急通報と特番が保護される`() {
        for (preset in Presets.all) {
            val leaked = ProtectedNumbers.DOCUMENTED_SPECIAL_NUMBERS.keys
                .mapNotNull { n -> RuleEngine.buildDialNumber(n, preset)?.let { n to it } }
            assertTrue(
                "${preset.name}: " + leaked.joinToString { "${it.first} -> ${it.second}" },
                leaked.isEmpty(),
            )
        }
    }

    @Test
    fun `全プリセットで3桁番号が書き換えられない`() {
        for (preset in Presets.all) {
            val leaked = (0..999).map { it.toString().padStart(3, '0') }
                .mapNotNull { n -> RuleEngine.buildDialNumber(n, preset)?.let { n to it } }
            assertTrue(
                "${preset.name}: " + leaked.joinToString { "${it.first} -> ${it.second}" },
                leaked.isEmpty(),
            )
        }
    }

    @Test
    fun `全プリセットでフリーダイヤル等が除外される`() {
        for (preset in Presets.all) {
            for (n in listOf("0120444444", "08001234567", "0570000123", "0990123456")) {
                assertNull("${preset.name}: $n", RuleEngine.buildDialNumber(n, preset))
            }
        }
    }
}
