package com.example.prefixdialer

/**
 * 依存を持たない最小限の JSON 実装。
 *
 * ## なぜ手書きなのか
 *
 * - Android の `org.json` はローカルユニットテストではスタブ化され、呼ぶと
 *   `RuntimeException("Stub!")` を投げる。判定・設定まわりを純 JVM のテストで
 *   網羅する方針と両立しない
 * - gson / kotlinx.serialization は、ルールセット 1 つとフラグ数個という規模に対して
 *   ProGuard 設定とリフレクション、あるいはコンパイラプラグインを持ち込む割に合わない
 *
 * 設定の永続化とエクスポート/インポートの両方がこの実装を使う。
 * 汎用の JSON ライブラリを目指さない（必要な範囲だけを正しく扱う）。
 */
sealed class Json {

    data class Obj(val members: Map<String, Json>) : Json()
    data class Arr(val elements: List<Json>) : Json()
    data class Str(val value: String) : Json()
    data class Num(val value: Double) : Json()
    data class Bool(val value: Boolean) : Json()
    data object Null : Json()

    // --- 読み出しヘルパ。型が違う / キーが無い場合は null を返す ---------------

    operator fun get(key: String): Json? = (this as? Obj)?.members?.get(key)

    fun asString(): String? = (this as? Str)?.value
    fun asBool(): Boolean? = (this as? Bool)?.value
    fun asInt(): Int? = (this as? Num)?.value?.toInt()
    fun asArray(): List<Json>? = (this as? Arr)?.elements

    fun string(key: String): String? = get(key)?.asString()
    fun bool(key: String): Boolean? = get(key)?.asBool()
    fun int(key: String): Int? = get(key)?.asInt()
    fun array(key: String): List<Json>? = get(key)?.asArray()

    /** 人間が読める形に整形して出力する（エクスポートしたファイルを直接編集できるように）。 */
    fun encode(indent: String = "  "): String =
        StringBuilder().also { write(it, indent, 0) }.toString()

    private fun write(out: StringBuilder, indent: String, depth: Int) {
        val pad = indent.repeat(depth)
        val padInner = indent.repeat(depth + 1)
        when (this) {
            is Null -> out.append("null")
            is Bool -> out.append(if (value) "true" else "false")
            is Num -> out.append(
                if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString(),
            )
            is Str -> out.appendQuoted(value)
            is Arr -> if (elements.isEmpty()) out.append("[]") else {
                out.append("[\n")
                elements.forEachIndexed { i, e ->
                    out.append(padInner)
                    e.write(out, indent, depth + 1)
                    if (i < elements.size - 1) out.append(',')
                    out.append('\n')
                }
                out.append(pad).append(']')
            }
            is Obj -> if (members.isEmpty()) out.append("{}") else {
                out.append("{\n")
                val entries = members.entries.toList()
                entries.forEachIndexed { i, (k, v) ->
                    out.append(padInner).appendQuoted(k).append(": ")
                    v.write(out, indent, depth + 1)
                    if (i < entries.size - 1) out.append(',')
                    out.append('\n')
                }
                out.append(pad).append('}')
            }
        }
    }

    companion object {

        fun obj(vararg pairs: Pair<String, Json>): Obj = Obj(linkedMapOf(*pairs))
        fun arr(elements: List<Json>): Arr = Arr(elements)
        fun of(value: String): Str = Str(value)
        fun of(value: Boolean): Bool = Bool(value)
        fun of(value: Int): Num = Num(value.toDouble())

        /** パースに失敗したら null を返す（不正な入力で落とさない）。 */
        fun parse(text: String): Json? = try {
            val parser = Parser(text)
            val value = parser.parseValue()
            parser.skipWhitespace()
            if (parser.atEnd()) value else null
        } catch (e: IllegalArgumentException) {
            null
        } catch (e: IndexOutOfBoundsException) {
            null
        } catch (e: NumberFormatException) {
            null
        }
    }

    private class Parser(private val src: String) {
        private var pos = 0

        fun atEnd(): Boolean = pos >= src.length

        fun skipWhitespace() {
            while (pos < src.length && src[pos].isWhitespace()) pos++
        }

        fun parseValue(): Json {
            skipWhitespace()
            require(pos < src.length) { "unexpected end of input" }
            return when (val c = src[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> Str(parseString())
                't' -> literal("true", Bool(true))
                'f' -> literal("false", Bool(false))
                'n' -> literal("null", Null)
                else -> {
                    require(c == '-' || c.isDigit()) { "unexpected character '$c' at $pos" }
                    parseNumber()
                }
            }
        }

        private fun literal(word: String, value: Json): Json {
            require(src.startsWith(word, pos)) { "invalid literal at $pos" }
            pos += word.length
            return value
        }

        private fun parseObject(): Obj {
            pos++ // '{'
            val members = linkedMapOf<String, Json>()
            skipWhitespace()
            if (pos < src.length && src[pos] == '}') {
                pos++
                return Obj(members)
            }
            while (true) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                require(pos < src.length && src[pos] == ':') { "expected ':' at $pos" }
                pos++
                members[key] = parseValue()
                skipWhitespace()
                require(pos < src.length) { "unterminated object" }
                when (src[pos]) {
                    ',' -> pos++
                    '}' -> {
                        pos++
                        return Obj(members)
                    }
                    else -> throw IllegalArgumentException("expected ',' or '}' at $pos")
                }
            }
        }

        private fun parseArray(): Arr {
            pos++ // '['
            val elements = mutableListOf<Json>()
            skipWhitespace()
            if (pos < src.length && src[pos] == ']') {
                pos++
                return Arr(elements)
            }
            while (true) {
                elements += parseValue()
                skipWhitespace()
                require(pos < src.length) { "unterminated array" }
                when (src[pos]) {
                    ',' -> pos++
                    ']' -> {
                        pos++
                        return Arr(elements)
                    }
                    else -> throw IllegalArgumentException("expected ',' or ']' at $pos")
                }
            }
        }

        private fun parseString(): String {
            require(pos < src.length && src[pos] == '"') { "expected a string at $pos" }
            pos++
            val sb = StringBuilder()
            while (true) {
                require(pos < src.length) { "unterminated string" }
                when (val c = src[pos]) {
                    '"' -> {
                        pos++
                        return sb.toString()
                    }
                    '\\' -> {
                        pos++
                        require(pos < src.length) { "unterminated escape" }
                        when (val e = src[pos]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                require(pos + 5 <= src.length) { "truncated unicode escape" }
                                sb.append(src.substring(pos + 1, pos + 5).toInt(16).toChar())
                                pos += 4
                            }
                            else -> throw IllegalArgumentException("invalid escape '\\$e' at $pos")
                        }
                        pos++
                    }
                    else -> {
                        sb.append(c)
                        pos++
                    }
                }
            }
        }

        private fun parseNumber(): Num {
            val start = pos
            if (pos < src.length && src[pos] == '-') pos++
            while (pos < src.length && (src[pos].isDigit() || src[pos] in ".eE+-")) pos++
            val text = src.substring(start, pos)
            return Num(text.toDoubleOrNull() ?: throw IllegalArgumentException("invalid number '$text'"))
        }
    }
}

/** JSON 文字列として安全に引用符で囲む。 */
private fun StringBuilder.appendQuoted(value: String): StringBuilder {
    append('"')
    for (c in value) {
        when {
            c == '"' -> append("\\\"")
            c == '\\' -> append("\\\\")
            c == '\n' -> append("\\n")
            c == '\r' -> append("\\r")
            c == '\t' -> append("\\t")
            c < ' ' -> append("\\u").append(c.code.toString(16).padStart(4, '0'))
            else -> append(c)
        }
    }
    return append('"')
}
