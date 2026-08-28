package io.github.tmlksu.prefixdialer

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * 通話履歴の書き換え機能への入り口（github フレーバー＝機能あり）。
 *
 * この機能は Google Play には出せない。Play は、既定の電話 / SMS / アシスタント
 * アプリでないアプリが CALL_LOG 権限グループを**マニフェストに宣言すること自体**を
 * 禁じている。例外が認められる用途の一覧にも「通話のリダイレクト」は無く、
 * さらに例外の条件が「その権限がコア機能を実現していること」であるのに対し、
 * 本アプリはこれを任意機能として設計している（＝コアではない）。
 *
 * そこで機能そのものをフレーバーで分けた。`play` 側は同名の no-op 実装を持ち、
 * 権限もサービスもコードも APK に含まれない。
 *
 * 呼び出し側（[PrefixRedirectionService]）はどちらのフレーバーかを意識しない。
 */
object CallLogRewrite {

    private const val TAG = "CallLogRewrite"

    /** この版で通話履歴の書き換えが使えるか。設定 UI の出し分けに使う。 */
    const val AVAILABLE = true

    /**
     * 履歴書き換えを有効にするときに要求する権限。
     *
     * この一覧をフレーバー側に置くことで、`play` 版の APK には CALL_LOG 系の
     * 権限名が文字列としても残らない。審査で見られる場所に痕跡を作らないため。
     */
    val PERMISSIONS: Array<String> = buildList {
        add(android.Manifest.permission.READ_CALL_LOG)
        add(android.Manifest.permission.WRITE_CALL_LOG)
        add(android.Manifest.permission.READ_CONTACTS)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    /**
     * 番号を書き換えて発信したことを通知する。履歴を元番号へ戻す処理を開始する。
     *
     * 発信経路から呼ばれるので速く返ること。実処理は [CallLogRewriteService] が行う。
     *
     * @param dialedNumber 実際に発信した番号（プレフィックス付き）
     * @param originalNumber 履歴に残したい元の番号
     */
    fun onCallRedirected(context: Context, dialedNumber: String, originalNumber: String) {
        PendingRewrites.add(dialedNumber, originalNumber, System.currentTimeMillis())
        runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, CallLogRewriteService::class.java),
            )
        }.onFailure { Log.w(TAG, "could not start rewrite service", it) }
    }
}
