package com.example.prefixdialer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 緊急通報・特番の保護に関する不変条件テスト。
 *
 * ## このテストの扱い
 *
 * ここが赤くなったら、それは「テストが古い」のではなく **安全層が壊れている**。
 * 期待値を書き換えて通すのではなく、実装を直すこと。
 *
 * ## 実機での確認について
 *
 * このテストはすべて純粋関数に対するローカル実行であり、発信は一切行わない。
 * 実機で挙動を確かめたい場合も、緊急通報番号（110 / 118 / 119 / 112）や
 * 相談ダイヤル（#7119 / #8000 / #9110 / #8103）には **絶対に発信しないこと**。
 * 実害なく確認できるのは 117（時報）程度で、これも必要最小限に留める。
 */
class ProtectedNumbersTest {

    // ------------------------------------------------------------------
    // 中核の不変条件: 文書化した特番は 1 つ残らず保護されること
    // ------------------------------------------------------------------

    @Test
    fun `文書化された特番はすべて保護される`() {
        val leaked = ProtectedNumbers.DOCUMENTED_SPECIAL_NUMBERS
            .filterKeys { !ProtectedNumbers.isProtected(it) }

        assertTrue(
            "保護されていない特番がある: " +
                leaked.entries.joinToString { "${it.key}(${it.value})" },
            leaked.isEmpty(),
        )
    }

    @Test
    fun `文書化された特番はプレフィックスが付かない`() {
        val rewritten = ProtectedNumbers.DOCUMENTED_SPECIAL_NUMBERS.keys
            .mapNotNull { num -> PhoneNumberPrefixer.buildDialNumber(num)?.let { num to it } }

        assertTrue(
            "特番が書き換えられた: " + rewritten.joinToString { "${it.first} -> ${it.second}" },
            rewritten.isEmpty(),
        )
    }

    /**
     * 3 桁番号は 000〜999 を総当たりで確認する。
     * 1XY 帯は総務省が特番用に予約しているが、取りこぼしを防ぐため全域を見る。
     */
    @Test
    fun `3桁番号は000から999まで一つも書き換えられない`() {
        val rewritten = (0..999)
            .map { it.toString().padStart(3, '0') }
            .mapNotNull { num -> PhoneNumberPrefixer.buildDialNumber(num)?.let { num to it } }

        assertTrue(
            "3桁番号が書き換えられた: " + rewritten.joinToString { "${it.first} -> ${it.second}" },
            rewritten.isEmpty(),
        )
    }

    /** 1 桁・2 桁も同様に触らない。 */
    @Test
    fun `1桁と2桁の番号は書き換えられない`() {
        val rewritten = (0..99)
            .flatMap { listOf(it.toString(), it.toString().padStart(2, '0')) }
            .distinct()
            .mapNotNull { num -> PhoneNumberPrefixer.buildDialNumber(num)?.let { num to it } }

        assertTrue(
            "短い番号が書き換えられた: " + rewritten.joinToString { "${it.first} -> ${it.second}" },
            rewritten.isEmpty(),
        )
    }

    // ------------------------------------------------------------------
    // 記号を含む番号
    // ------------------------------------------------------------------

    @Test
    fun `シャープで始まる番号はすべて保護される`() {
        // #7119 等の既知の窓口に加え、将来追加される # 番号も一括で守れていること
        for (n in listOf("#7119", "#8000", "#9110", "#8103", "#8891", "#0000", "#9999", "#31#09012345678")) {
            assertTrue("$n が保護されていない", ProtectedNumbers.isProtected(n))
            assertNull("$n が書き換えられた", PhoneNumberPrefixer.buildDialNumber(n))
        }
    }

    @Test
    fun `アスタリスクを含む番号は保護される`() {
        for (n in listOf("*99", "*67", "*310912345678")) {
            assertTrue("$n が保護されていない", ProtectedNumbers.isProtected(n))
            assertNull("$n が書き換えられた", PhoneNumberPrefixer.buildDialNumber(n))
        }
    }

    @Test
    fun `ポーズ文字を含む番号は素通しする`() {
        // 代表番号 + 内線。プレフィックス付与で DTMF の送出が崩れないよう 1.0 では触らない
        for (n in listOf("0312345678,,,123", "0312345678;123", "0312345678p123", "0312345678w9")) {
            assertTrue("$n が保護されていない", ProtectedNumbers.isProtected(n))
            assertNull("$n が書き換えられた", PhoneNumberPrefixer.buildDialNumber(n))
        }
    }

    // ------------------------------------------------------------------
    // 発信者番号通知プレフィックス
    // ------------------------------------------------------------------

    @Test
    fun `184と186で始まる番号は素通しする`() {
        for (n in listOf("18409012345678", "18609012345678", "1840312345678", "184", "186")) {
            assertTrue("$n が保護されていない", ProtectedNumbers.isProtected(n))
            assertNull("$n が書き換えられた", PhoneNumberPrefixer.buildDialNumber(n))
        }
    }

    // ------------------------------------------------------------------
    // 国際表記で保護をすり抜けられないこと
    // ------------------------------------------------------------------

    @Test
    fun `国番号付きで書かれた特番も保護される`() {
        for (n in listOf("+81110", "+81119", "+81118", "+81117")) {
            assertTrue("$n が保護されていない", ProtectedNumbers.isProtected(n))
            assertNull("$n が書き換えられた", PhoneNumberPrefixer.buildDialNumber(n))
        }
    }

    @Test
    fun `区切り記号で書かれた特番も保護される`() {
        for (n in listOf("1-1-0", "1 1 9", "(118)", " 110 ")) {
            assertTrue("$n が保護されていない", ProtectedNumbers.isProtected(n))
            assertNull("$n が書き換えられた", PhoneNumberPrefixer.buildDialNumber(n))
        }
    }

    // ------------------------------------------------------------------
    // 過剰ブロックしていないこと（ガードが機能を殺していない確認）
    // ------------------------------------------------------------------

    @Test
    fun `通常の番号はガードに引っかからない`() {
        for (n in listOf("09012345678", "08012345678", "07012345678", "0312345678", "0612345678", "05012345678")) {
            assertFalse("$n が誤って保護されている", ProtectedNumbers.isProtected(n))
        }
    }

    @Test
    fun `ガード導入後も通常の番号にはプレフィックスが付く`() {
        assertEquals("0063" + "09012345678", PhoneNumberPrefixer.buildDialNumber("09012345678"))
        assertEquals("0063" + "0312345678", PhoneNumberPrefixer.buildDialNumber("0312345678"))
        assertEquals("0063" + "05012345678", PhoneNumberPrefixer.buildDialNumber("05012345678"))
    }

    // ------------------------------------------------------------------
    // 入力の異常系
    // ------------------------------------------------------------------

    @Test
    fun `空やnullは保護扱い`() {
        assertTrue(ProtectedNumbers.isProtected(null))
        assertTrue(ProtectedNumbers.isProtected(""))
        assertTrue(ProtectedNumbers.isProtected("   "))
        assertTrue(ProtectedNumbers.isProtected("abc"))
    }
}
