package io.github.tmlksu.prefixdialer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 設定の JSON 相互変換のテスト。
 *
 * 手で編集されたファイルを読むことを想定しているため、壊れた入力に対する挙動を
 * 特に厚く見る。方針は「読めなかったらプレフィックスを付けない側に倒す」。
 */
class SettingsJsonTest {

    private val richSettings = Settings(
        ruleSet = RuleSet(
            name = "テスト事業者",
            rules = listOf(
                DialRule(RuleCondition.StartsWith("0312"), RuleAction.PassThrough),
                DialRule(
                    RuleCondition.OfType(NumberCategory.MOBILE),
                    RuleAction.Apply("0088", LeadingZero.STRIP, suffix = "#"),
                ),
                DialRule(
                    RuleCondition.OfType(NumberCategory.FIXED_LINE),
                    RuleAction.Apply("0077", LeadingZero.TO_COUNTRY_CODE),
                    enabled = false,
                ),
            ),
            enabled = true,
        ),
        callLogRewriteEnabled = true,
        disableWhileRoaming = false,
        disabledPhoneAccountIds = setOf("sim1", "sim2"),
        excludedNumbers = setOf("0312345678", "09099998888"),
        callRecordLimit = 50,
    )

    // ------------------------------------------------------------------
    // 往復
    // ------------------------------------------------------------------

    @Test
    fun `設定を書き出して読み戻すと一致する`() {
        assertEquals(richSettings, SettingsJson.decode(SettingsJson.encode(richSettings)))
    }

    @Test
    fun `既定の設定も往復できる`() {
        val defaults = Settings()
        assertEquals(defaults, SettingsJson.decode(SettingsJson.encode(defaults)))
    }

    @Test
    fun `全プリセットが往復できる`() {
        for (preset in Presets.all) {
            val settings = Settings(ruleSet = preset)
            assertEquals(preset.name, settings, SettingsJson.decode(SettingsJson.encode(settings)))
        }
    }

    @Test
    fun `書き出した内容は有効なJSONで人間が読める`() {
        val text = SettingsJson.encode(richSettings)
        assertTrue("パースできない", Json.parse(text) != null)
        assertTrue("整形されていない", text.contains("\n"))
        assertEquals(SettingsJson.VERSION, Json.parse(text)?.int("version"))
    }

    // ------------------------------------------------------------------
    // 壊れた入力・欠けたキー
    // ------------------------------------------------------------------

    @Test
    fun `パースできない入力ではfallbackを返す`() {
        val fallback = Settings(callRecordLimit = 7)
        for (text in listOf("", "   ", "{", "not json", "[1,2,3", "\u0000")) {
            assertEquals("'$text'", fallback, SettingsJson.decode(text, fallback))
        }
        assertEquals(fallback, SettingsJson.decode(null, fallback))
    }

    @Test
    fun `欠けたキーは既定値で補われる`() {
        val decoded = SettingsJson.decode("""{"version": 1}""")
        assertEquals(Settings(), decoded)
    }

    @Test
    fun `未知のキーは無視される`() {
        val text = """
            {
              "version": 1,
              "未来の設定": {"a": [1, 2, 3]},
              "callRecordLimit": 25
            }
        """.trimIndent()
        assertEquals(25, SettingsJson.decode(text).callRecordLimit)
    }

    @Test
    fun `型が違う値は既定値で補われる`() {
        val text = """
            {
              "callLogRewriteEnabled": "yes",
              "disableWhileRoaming": 1,
              "callRecordLimit": "たくさん",
              "excludedNumbers": "0312345678"
            }
        """.trimIndent()
        val decoded = SettingsJson.decode(text)
        assertEquals(Settings().callLogRewriteEnabled, decoded.callLogRewriteEnabled)
        assertEquals(Settings().disableWhileRoaming, decoded.disableWhileRoaming)
        assertEquals(Settings().callRecordLimit, decoded.callRecordLimit)
        assertEquals(Settings().excludedNumbers, decoded.excludedNumbers)
    }

    // ------------------------------------------------------------------
    // 壊れたルールは「付けない側」に倒す
    // ------------------------------------------------------------------

    @Test
    fun `prefixが読めないルールは捨てられる`() {
        val text = """
            {
              "ruleSet": {
                "name": "壊れた設定",
                "enabled": true,
                "rules": [
                  {"condition": {"type": "ofType", "category": "MOBILE"},
                   "action": {"type": "apply", "leadingZero": "KEEP"}},
                  {"condition": {"type": "ofType", "category": "FIXED_LINE"},
                   "action": {"type": "apply", "prefix": "abc", "leadingZero": "KEEP"}}
                ]
              }
            }
        """.trimIndent()
        val decoded = SettingsJson.decode(text)
        assertTrue("壊れたルールが残っている", decoded.ruleSet.rules.isEmpty())
        assertNull(RuleEngine.buildDialNumber("09012345678", decoded.ruleSet))
        assertNull(RuleEngine.buildDialNumber("0312345678", decoded.ruleSet))
    }

    @Test
    fun `未知の条件や動作を持つルールは捨てられる`() {
        val text = """
            {
              "ruleSet": {
                "name": "未来の設定",
                "rules": [
                  {"condition": {"type": "regex", "pattern": ".*"},
                   "action": {"type": "apply", "prefix": "0063", "leadingZero": "KEEP"}},
                  {"condition": {"type": "ofType", "category": "MOBILE"},
                   "action": {"type": "transform", "script": "evil"}},
                  {"condition": {"type": "ofType", "category": "UNKNOWN_CATEGORY"},
                   "action": {"type": "apply", "prefix": "0063", "leadingZero": "KEEP"}},
                  {"condition": {"type": "ofType", "category": "MOBILE"},
                   "action": {"type": "apply", "prefix": "0063", "leadingZero": "SIDEWAYS"}}
                ]
              }
            }
        """.trimIndent()
        val decoded = SettingsJson.decode(text)
        assertTrue("未知のルールが残っている", decoded.ruleSet.rules.isEmpty())
        assertNull(RuleEngine.buildDialNumber("09012345678", decoded.ruleSet))
    }

    @Test
    fun `ルール配列自体が壊れていてもルールなしになる`() {
        val text = """{"ruleSet": {"name": "壊れた設定", "rules": "全部"}}"""
        val decoded = SettingsJson.decode(text)
        assertTrue(decoded.ruleSet.rules.isEmpty())
        assertNull(RuleEngine.buildDialNumber("09012345678", decoded.ruleSet))
    }

    @Test
    fun `読み込んだ設定でも緊急通報は保護される`() {
        // 手で編集して特番を狙うようなルールを書かれても、安全層は貫通しない
        val text = """
            {
              "ruleSet": {
                "name": "悪意ある設定",
                "enabled": true,
                "rules": [
                  {"condition": {"type": "startsWith", "digits": "1"},
                   "action": {"type": "apply", "prefix": "0063", "leadingZero": "KEEP"}},
                  {"condition": {"type": "startsWith", "digits": "11"},
                   "action": {"type": "apply", "prefix": "0063", "leadingZero": "STRIP"}}
                ]
              }
            }
        """.trimIndent()
        val ruleSet = SettingsJson.decode(text).ruleSet
        val leaked = ProtectedNumbers.DOCUMENTED_SPECIAL_NUMBERS.keys
            .mapNotNull { n -> RuleEngine.buildDialNumber(n, ruleSet)?.let { n to it } }
        assertTrue(leaked.joinToString { "${it.first} -> ${it.second}" }, leaked.isEmpty())
    }

    // ------------------------------------------------------------------
    // 正規化
    // ------------------------------------------------------------------

    @Test
    fun `除外番号は数字のみに正規化して読み込まれる`() {
        val text = """{"excludedNumbers": ["03-1234-5678", "090 9999 8888", "(06)1234-5678"]}"""
        assertEquals(
            setOf("0312345678", "09099998888", "0612345678"),
            SettingsJson.decode(text).excludedNumbers,
        )
    }

    @Test
    fun `負の保持件数は0に丸められる`() {
        assertEquals(0, SettingsJson.decode("""{"callRecordLimit": -10}""").callRecordLimit)
    }
}
