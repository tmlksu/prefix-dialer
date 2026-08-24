package com.example.prefixdialer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 手書き JSON 実装のテスト。 */
class JsonTest {

    private fun parse(text: String) = Json.parse(text)

    @Test
    fun `基本的な値をパースできる`() {
        assertEquals(Json.Str("abc"), parse("\"abc\""))
        assertEquals(Json.Bool(true), parse("true"))
        assertEquals(Json.Bool(false), parse("false"))
        assertEquals(Json.Null, parse("null"))
        assertEquals(42, parse("42")?.asInt())
        assertEquals(-7, parse("-7")?.asInt())
    }

    @Test
    fun `オブジェクトと配列をパースできる`() {
        val v = parse("""{"a": 1, "b": [true, "x"], "c": {"d": null}}""")!!
        assertEquals(1, v.int("a"))
        assertEquals(2, v.array("b")?.size)
        assertEquals(true, v.array("b")?.get(0)?.asBool())
        assertEquals("x", v.array("b")?.get(1)?.asString())
        assertEquals(Json.Null, v["c"]?.get("d"))
    }

    @Test
    fun `空のオブジェクトと配列を扱える`() {
        assertEquals(Json.Obj(emptyMap()), parse("{}"))
        assertEquals(Json.Arr(emptyList()), parse("[]"))
        assertEquals("{}", Json.Obj(emptyMap()).encode())
        assertEquals("[]", Json.Arr(emptyList()).encode())
    }

    @Test
    fun `空白と改行を読み飛ばす`() {
        val v = parse("  {\n\t\"a\" :  1 ,\n \"b\": 2\n}  ")!!
        assertEquals(1, v.int("a"))
        assertEquals(2, v.int("b"))
    }

    @Test
    fun `エスケープを正しく解釈する`() {
        assertEquals("a\"b", parse("""  "a\"b"  """)?.asString())
        assertEquals("a\\b", parse("""  "a\\b"  """)?.asString())
        assertEquals("a\nb", parse("""  "a\nb"  """)?.asString())
        assertEquals("a\tb", parse("""  "a\tb"  """)?.asString())
        assertEquals("あ", parse("""  "あ"  """)?.asString())
    }

    @Test
    fun `書き出した文字列を読み戻せる`() {
        val original = Json.obj(
            "name" to Json.of("改行\nと\"引用符\"とタブ\t"),
            "flag" to Json.of(true),
            "count" to Json.of(3),
            "list" to Json.arr(listOf(Json.of("a"), Json.of("b"))),
            "nested" to Json.obj("k" to Json.of("v")),
        )
        assertEquals(original, parse(original.encode()))
    }

    @Test
    fun `不正な入力ではnullを返し例外を投げない`() {
        val broken = listOf(
            "", "   ", "{", "}", "[", "]", "{\"a\"}", "{\"a\":}", "{a: 1}",
            "[1, 2", "\"unterminated", "tru", "nul", "{\"a\": 1,}", "--1",
            "{}{}", "[1] [2]", "\"a\" \"b\"", "{\"a\": 1} trailing",
        )
        for (text in broken) {
            assertNull("'$text' が null を返さなかった", parse(text))
        }
    }

    @Test
    fun `深い入れ子でもスタックを壊さない`() {
        val depth = 200
        val text = "[".repeat(depth) + "]".repeat(depth)
        assertTrue(parse(text) is Json.Arr)
    }

    @Test
    fun `整数は小数点なしで出力される`() {
        assertEquals("3", Json.of(3).encode())
        assertEquals("0", Json.of(0).encode())
        assertEquals("-5", Json.of(-5).encode())
    }
}
