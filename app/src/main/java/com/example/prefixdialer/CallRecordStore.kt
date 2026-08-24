package com.example.prefixdialer

import android.content.Context
import android.content.SharedPreferences

/**
 * [CallRecord] の保存。端末内のみ、件数上限つき。
 *
 * 設定と同じく SharedPreferences に JSON 文字列で持つ。件数が上限に収まるため
 * データベースを持ち込むほどではない。
 *
 * 発信経路から呼ばれるので、[record] は速く返る必要がある。書き込みは `apply` で
 * 非同期に行い、メモリ上の一覧は即座に更新する。
 */
class CallRecordStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var cached: List<CallRecord>? = null

    /** 新しい順の一覧。 */
    fun list(): List<CallRecord> {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: CallRecordJson.decode(prefs.getString(KEY_RECORDS, null)).also { cached = it }
        }
    }

    /**
     * 1 件記録する。上限を超えた古い記録は捨てる。
     *
     * @param limit 保持件数。0 以下なら記録せず、既存の記録も消す
     */
    fun record(record: CallRecord, limit: Int) {
        synchronized(this) {
            if (limit <= 0) {
                clear()
                return
            }
            val updated = (listOf(record) + list()).take(limit)
            cached = updated
            prefs.edit().putString(KEY_RECORDS, CallRecordJson.encode(updated)).apply()
        }
    }

    fun clear() {
        synchronized(this) {
            cached = emptyList()
            prefs.edit().remove(KEY_RECORDS).apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "prefix_dialer_call_records"
        private const val KEY_RECORDS = "records_json"
    }
}
