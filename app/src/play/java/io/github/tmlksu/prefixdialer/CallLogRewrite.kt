package io.github.tmlksu.prefixdialer

import android.content.Context

/**
 * 通話履歴の書き換え機能への入り口（play フレーバー＝機能なし）。
 *
 * Google Play 版はこの機能を持たない。Play は、既定の電話 / SMS / アシスタント
 * アプリでないアプリが CALL_LOG 権限グループを**マニフェストに宣言すること自体**を
 * 禁じているため（実際に要求するかどうかは関係ない）。
 *
 * 「宣言しないが機能は残す」ことはできないので、権限・サービス・実装コードごと
 * このフレーバーから外している。APK を検査しても CALL_LOG 関連は一切出てこない。
 *
 * プレフィックスを付ける基本機能はこの権限を必要としないので、Play 版でも
 * アプリの主目的はそのまま満たせる。履歴にプレフィックス付きの番号が残る点だけが違う。
 *
 * 設定から読み込んだ [Settings.callLogRewriteEnabled] が true でも、ここが
 * 何もしないので安全側に倒れる（github 版で書き出した設定を読み込んだ場合など）。
 */
object CallLogRewrite {

    /** この版で通話履歴の書き換えが使えるか。設定 UI の出し分けに使う。 */
    const val AVAILABLE = false

    /** 要求する権限は無い。CALL_LOG 系の権限名は APK に一切現れない。 */
    val PERMISSIONS: Array<String> = emptyArray()

    /** 何もしない。 */
    @Suppress("UNUSED_PARAMETER")
    fun onCallRedirected(context: Context, dialedNumber: String, originalNumber: String) = Unit
}
