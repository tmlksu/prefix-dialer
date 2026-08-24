package com.example.prefixdialer

/**
 * [Settings] と JSON の相互変換。
 *
 * 用途は 2 つ。どちらも同じ形式を使う。
 *  - 設定の永続化（[SettingsStore] が文字列として保存する）
 *  - エクスポート/インポート（ユーザーがファイルとして持ち出す）
 *
 * ## 読み込み側の方針: 壊れた入力で落とさない
 *
 * 手で編集されたファイルを読むことを前提にする。未知のキーは無視し、欠けたキーや
 * 型の違う値は既定値で補う。**プレフィックスやルールが読めなかった場合は、
 * 適当に解釈せず「ルールなし」に倒す**（＝プレフィックスが付かない）。
 * 壊れた設定で意図しない番号へ発信するより、機能が効かないほうが安全なため。
 *
 * ## バージョン
 *
 * [VERSION] を上げるのは互換性を壊す変更のときだけ。キーの追加は上げない
 * （古いファイルは既定値で補われ、新しいファイルの未知キーは無視されるため）。
 */
object SettingsJson {

    const val VERSION = 1

    private const val KEY_VERSION = "version"
    private const val KEY_RULE_SET = "ruleSet"
    private const val KEY_NAME = "name"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_RULES = "rules"
    private const val KEY_CONDITION = "condition"
    private const val KEY_ACTION = "action"
    private const val KEY_TYPE = "type"
    private const val KEY_CATEGORY = "category"
    private const val KEY_DIGITS = "digits"
    private const val KEY_PREFIX = "prefix"
    private const val KEY_LEADING_ZERO = "leadingZero"
    private const val KEY_SUFFIX = "suffix"
    private const val KEY_CALL_LOG_REWRITE = "callLogRewriteEnabled"
    private const val KEY_DISABLE_ROAMING = "disableWhileRoaming"
    private const val KEY_DISABLED_ACCOUNTS = "disabledPhoneAccountIds"
    private const val KEY_EXCLUDED = "excludedNumbers"
    private const val KEY_RECORD_LIMIT = "callRecordLimit"

    private const val TYPE_OF_TYPE = "ofType"
    private const val TYPE_STARTS_WITH = "startsWith"
    private const val TYPE_APPLY = "apply"
    private const val TYPE_PASS_THROUGH = "passThrough"

    // ------------------------------------------------------------------
    // 書き出し
    // ------------------------------------------------------------------

    fun encode(settings: Settings): String = toJson(settings).encode()

    fun toJson(settings: Settings): Json = Json.obj(
        KEY_VERSION to Json.of(VERSION),
        KEY_RULE_SET to settings.ruleSet.toJson(),
        KEY_CALL_LOG_REWRITE to Json.of(settings.callLogRewriteEnabled),
        KEY_DISABLE_ROAMING to Json.of(settings.disableWhileRoaming),
        KEY_DISABLED_ACCOUNTS to Json.arr(settings.disabledPhoneAccountIds.sorted().map { Json.of(it) }),
        KEY_EXCLUDED to Json.arr(settings.excludedNumbers.sorted().map { Json.of(it) }),
        KEY_RECORD_LIMIT to Json.of(settings.callRecordLimit),
    )

    private fun RuleSet.toJson(): Json = Json.obj(
        KEY_NAME to Json.of(name),
        KEY_ENABLED to Json.of(enabled),
        KEY_RULES to Json.arr(rules.map { it.toJson() }),
    )

    private fun DialRule.toJson(): Json = Json.obj(
        KEY_CONDITION to condition.toJson(),
        KEY_ACTION to action.toJson(),
        KEY_ENABLED to Json.of(enabled),
    )

    private fun RuleCondition.toJson(): Json = when (this) {
        is RuleCondition.OfType -> Json.obj(
            KEY_TYPE to Json.of(TYPE_OF_TYPE),
            KEY_CATEGORY to Json.of(category.name),
        )
        is RuleCondition.StartsWith -> Json.obj(
            KEY_TYPE to Json.of(TYPE_STARTS_WITH),
            KEY_DIGITS to Json.of(digits),
        )
    }

    private fun RuleAction.toJson(): Json = when (this) {
        is RuleAction.PassThrough -> Json.obj(KEY_TYPE to Json.of(TYPE_PASS_THROUGH))
        is RuleAction.Apply -> Json.obj(
            KEY_TYPE to Json.of(TYPE_APPLY),
            KEY_PREFIX to Json.of(prefix),
            KEY_LEADING_ZERO to Json.of(leadingZero.name),
            KEY_SUFFIX to Json.of(suffix),
        )
    }

    // ------------------------------------------------------------------
    // 読み込み
    // ------------------------------------------------------------------

    /**
     * JSON 文字列から設定を復元する。
     *
     * パースに失敗した場合は [fallback] を返す。部分的に壊れている場合は、
     * 読めたところだけを採用し残りは既定値で補う。
     */
    fun decode(text: String?, fallback: Settings = Settings()): Settings {
        if (text.isNullOrBlank()) return fallback
        val root = Json.parse(text) ?: return fallback
        return fromJson(root, fallback)
    }

    fun fromJson(root: Json, fallback: Settings = Settings()): Settings = Settings(
        ruleSet = root[KEY_RULE_SET]?.toRuleSet() ?: fallback.ruleSet,
        callLogRewriteEnabled = root.bool(KEY_CALL_LOG_REWRITE) ?: fallback.callLogRewriteEnabled,
        disableWhileRoaming = root.bool(KEY_DISABLE_ROAMING) ?: fallback.disableWhileRoaming,
        disabledPhoneAccountIds = root.array(KEY_DISABLED_ACCOUNTS)
            ?.mapNotNull { it.asString() }?.toSet() ?: fallback.disabledPhoneAccountIds,
        excludedNumbers = root.array(KEY_EXCLUDED)
            ?.mapNotNull { it.asString() }
            ?.map { Settings.normalizeNumber(it) }
            ?.filter { it.isNotEmpty() }
            ?.toSet() ?: fallback.excludedNumbers,
        callRecordLimit = root.int(KEY_RECORD_LIMIT)?.coerceAtLeast(0)
            ?: fallback.callRecordLimit,
    )

    private fun Json.toRuleSet(): RuleSet? {
        val name = string(KEY_NAME) ?: return null
        // ルール配列が読めない場合はルールなしに倒す。壊れた設定で意図しない番号へ
        // 発信するより、プレフィックスが付かないほうが安全。
        val rules = array(KEY_RULES)?.mapNotNull { it.toRule() } ?: emptyList()
        return RuleSet(
            name = name,
            rules = rules,
            enabled = bool(KEY_ENABLED) ?: true,
        )
    }

    private fun Json.toRule(): DialRule? {
        val condition = this[KEY_CONDITION]?.toCondition() ?: return null
        val action = this[KEY_ACTION]?.toAction() ?: return null
        return DialRule(condition, action, bool(KEY_ENABLED) ?: true)
    }

    private fun Json.toCondition(): RuleCondition? = when (string(KEY_TYPE)) {
        TYPE_OF_TYPE -> string(KEY_CATEGORY)
            ?.let { name -> NumberCategory.entries.firstOrNull { it.name == name } }
            ?.let { RuleCondition.OfType(it) }
        TYPE_STARTS_WITH -> string(KEY_DIGITS)
            ?.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
            ?.let { RuleCondition.StartsWith(it) }
        else -> null                                  // 未知の条件は読み飛ばす
    }

    private fun Json.toAction(): RuleAction? = when (string(KEY_TYPE)) {
        TYPE_PASS_THROUGH -> RuleAction.PassThrough
        TYPE_APPLY -> {
            // プレフィックスが読めない、あるいは数字以外を含む場合はルールごと捨てる。
            // 中途半端に解釈して意図しない番号へ発信させない。
            val prefix = string(KEY_PREFIX)?.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
            val leadingZero = string(KEY_LEADING_ZERO)
                ?.let { name -> LeadingZero.entries.firstOrNull { it.name == name } }
            if (prefix == null || leadingZero == null) null
            else RuleAction.Apply(prefix, leadingZero, string(KEY_SUFFIX) ?: "")
        }
        else -> null                                  // 未知の動作は読み飛ばす
    }
}
