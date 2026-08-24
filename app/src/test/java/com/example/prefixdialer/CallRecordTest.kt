package com.example.prefixdialer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 発信記録の生成と JSON 変換のテスト。 */
class CallRecordTest {

    private val now = 1_756_000_000_000L   // 固定値。時刻に依存させない

    @Test
    fun `書き換えた発信の記録`() {
        val record = CallRecord.from(
            now,
            "09012345678",
            DialDecision.Rewrite("006309012345678", NumberCategory.MOBILE),
        )
        assertEquals("09012345678", record.originalNumber)
        assertEquals("006309012345678", record.dialedNumber)
        assertNull(record.skipReason)
        assertTrue(record.wasRewritten)
    }

    @Test
    fun `書き換えなかった発信の記録には理由が残る`() {
        val record = CallRecord.from(
            now,
            "110",
            DialDecision.Skip(SkipReason.PROTECTED_NUMBER),
        )
        assertEquals("110", record.originalNumber)
        assertNull(record.dialedNumber)
        assertEquals(SkipReason.PROTECTED_NUMBER, record.skipReason)
        assertFalse(record.wasRewritten)
    }

    @Test
    fun `記録を書き出して読み戻せる`() {
        val records = listOf(
            CallRecord(now, "09012345678", dialedNumber = "006309012345678"),
            CallRecord(now - 1000, "110", skipReason = SkipReason.PROTECTED_NUMBER),
            CallRecord(now - 2000, "0120444444", skipReason = SkipReason.NOT_TARGET_TYPE),
        )
        assertEquals(records, CallRecordJson.decode(CallRecordJson.encode(records)))
    }

    @Test
    fun `空の一覧も往復できる`() {
        assertEquals(emptyList<CallRecord>(), CallRecordJson.decode(CallRecordJson.encode(emptyList())))
    }

    @Test
    fun `壊れた入力では空の一覧を返す`() {
        for (text in listOf("", "   ", "{", "not json", """{"records": "全部"}""")) {
            assertEquals("'$text'", emptyList<CallRecord>(), CallRecordJson.decode(text))
        }
        assertEquals(emptyList<CallRecord>(), CallRecordJson.decode(null))
    }

    @Test
    fun `必須項目が欠けた記録は読み飛ばされる`() {
        val text = """
            {"records": [
              {"original": "09012345678"},
              {"timestamp": 1},
              {"timestamp": 2, "original": "0312345678", "dialed": "00630312345678"}
            ]}
        """.trimIndent()
        val decoded = CallRecordJson.decode(text)
        assertEquals(1, decoded.size)
        assertEquals("0312345678", decoded.single().originalNumber)
    }

    @Test
    fun `未知の理由を持つ記録は理由なしとして読まれる`() {
        // 将来 SkipReason が増えた版で書き出したファイルを古い版で読む場合。
        // 記録自体は捨てず、理由だけ落とす。
        val text = """{"records": [{"timestamp": 1, "original": "090", "skipReason": "FUTURE"}]}"""
        val decoded = CallRecordJson.decode(text)
        assertEquals(1, decoded.size)
        assertNull(decoded.single().skipReason)
    }

    @Test
    fun `大きなタイムスタンプが精度を失わない`() {
        // Json.Num は Double なのでミリ秒エポックが丸められないことを確認する
        val record = CallRecord(now, "09012345678", dialedNumber = "006309012345678")
        assertEquals(now, CallRecordJson.decode(CallRecordJson.encode(listOf(record))).single().timestamp)
    }
}
