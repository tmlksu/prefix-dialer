package io.github.tmlksu.prefixdialer

import java.util.Collections

/**
 * 「実際に発信したプレフィックス番号」→「履歴に残したい元番号」の対応を一時的に保持する。
 *
 * onPlaceCall で add し、CallLogRewriteService が新規履歴を見つけたら consume する。
 * プロセス内メモリのみ（永続化不要・短命）。
 */
object PendingRewrites {

    private const val TTL_MS = 60_000L // 1分以上前の未消化エントリは破棄

    data class Entry(
        val dialDigits: String,   // 実際に発信した番号（数字のみ）
        val original: String,     // 履歴に戻したい元番号（生のまま）
        val addedAt: Long,
    )

    private val entries = Collections.synchronizedList(mutableListOf<Entry>())

    fun add(dialNumber: String, original: String, now: Long) {
        purgeOld(now)
        entries.add(Entry(digitsOnly(dialNumber), original, now))
    }

    fun isEmpty(): Boolean = entries.isEmpty()

    /**
     * 履歴に載っていた番号にマッチする元番号を返し、その対応を消化する。
     * マッチしなければ null。
     */
    fun consumeFor(loggedNumber: String?): String? {
        if (loggedNumber == null) return null
        val digits = digitsOnly(loggedNumber)
        synchronized(entries) {
            val idx = entries.indexOfFirst { it.dialDigits == digits }
            if (idx < 0) return null
            return entries.removeAt(idx).original
        }
    }

    fun purgeOld(now: Long) {
        synchronized(entries) {
            entries.removeAll { now - it.addedAt > TTL_MS }
        }
    }

    private fun digitsOnly(s: String): String = s.filter { it.isDigit() }
}
