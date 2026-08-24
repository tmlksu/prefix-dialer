package io.github.tmlksu.prefixdialer

/**
 * 1 件の発信に対して、このアプリが何をしたかの記録。
 *
 * ## なぜ必要か
 *
 * この機能は「黙って動く」ため、ユーザーは効いているかどうかを自力で確認できない。
 * 通話履歴を書き換える設定にしていると元番号に戻るので、なおさら分からない。
 * 記録が無いと、機能が壊れていても気づけないまま通常料金を払い続けることになる。
 *
 * ## 保存範囲
 *
 * **端末内のみ。外部へ送信しない。** アプリ自身の動作記録であり、
 * 通話履歴（`CallLog`）を読むわけではないので権限も要らない。
 *
 * @param timestamp 発信時刻（エポックミリ秒）
 * @param originalNumber ダイヤラーから渡された元の番号
 * @param dialedNumber 実際に発信した番号。書き換えなかった場合は null
 * @param skipReason 書き換えなかった理由。書き換えた場合は null
 */
data class CallRecord(
    val timestamp: Long,
    val originalNumber: String,
    val dialedNumber: String? = null,
    val skipReason: SkipReason? = null,
) {

    /** プレフィックスが付いたか。 */
    val wasRewritten: Boolean get() = dialedNumber != null

    companion object {

        /** 判定結果から記録を作る。 */
        fun from(timestamp: Long, originalNumber: String, decision: DialDecision): CallRecord =
            when (decision) {
                is DialDecision.Rewrite -> CallRecord(
                    timestamp = timestamp,
                    originalNumber = originalNumber,
                    dialedNumber = decision.dialNumber,
                )
                is DialDecision.Skip -> CallRecord(
                    timestamp = timestamp,
                    originalNumber = originalNumber,
                    skipReason = decision.reason,
                )
            }
    }
}

/** 発信記録の一覧と、その JSON 変換。 */
object CallRecordJson {

    private const val KEY_RECORDS = "records"
    private const val KEY_TIMESTAMP = "timestamp"
    private const val KEY_ORIGINAL = "original"
    private const val KEY_DIALED = "dialed"
    private const val KEY_SKIP_REASON = "skipReason"

    fun encode(records: List<CallRecord>): String = Json.obj(
        KEY_RECORDS to Json.arr(records.map { it.toJson() }),
    ).encode(indent = "")

    fun decode(text: String?): List<CallRecord> {
        if (text.isNullOrBlank()) return emptyList()
        val root = Json.parse(text) ?: return emptyList()
        return root.array(KEY_RECORDS)?.mapNotNull { it.toRecord() } ?: emptyList()
    }

    private fun CallRecord.toJson(): Json {
        val members = buildList {
            add(KEY_TIMESTAMP to Json.Num(timestamp.toDouble()))
            add(KEY_ORIGINAL to Json.of(originalNumber))
            dialedNumber?.let { add(KEY_DIALED to Json.of(it)) }
            skipReason?.let { add(KEY_SKIP_REASON to Json.of(it.name)) }
        }
        return Json.obj(*members.toTypedArray())
    }

    private fun Json.toRecord(): CallRecord? {
        val timestamp = (this[KEY_TIMESTAMP] as? Json.Num)?.value?.toLong() ?: return null
        val original = string(KEY_ORIGINAL) ?: return null
        return CallRecord(
            timestamp = timestamp,
            originalNumber = original,
            dialedNumber = string(KEY_DIALED),
            skipReason = string(KEY_SKIP_REASON)
                ?.let { name -> SkipReason.entries.firstOrNull { it.name == name } },
        )
    }
}
