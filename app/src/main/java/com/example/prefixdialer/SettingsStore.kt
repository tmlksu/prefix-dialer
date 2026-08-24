package com.example.prefixdialer

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * [Settings] の永続化。
 *
 * SharedPreferences に [SettingsJson] の形式で 1 本の文字列として保存する。
 * 設定項目ごとにキーを分けないのは、エクスポート/インポートと同じ形式を使い回して
 * 「保存されている内容」と「書き出される内容」が食い違わないようにするため。
 *
 * ## このクラスを薄く保つこと
 *
 * 判定や検証のロジックは [Settings] / [SettingsJson] 側に置く。ここは Android API との
 * 境界だけを担当する。境界を薄くしておけば、ロジックは純 JVM のユニットテストで
 * 網羅できる（実機もエミュレータも要らない）。
 *
 * ## 発信経路からの参照について
 *
 * [PrefixRedirectionService.onPlaceCall] は発信のたびに呼ばれ、応答が遅れると
 * 発信そのものが遅れる。そのため読み出し結果をメモリに保持し、毎回の I/O を避ける。
 * 別プロセスからの変更は想定しない（設定 UI と発信サービスは同一プロセス）。
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var cached: Settings? = null

    /** 現在の設定。初回のみ読み出し、以降はメモリ上の値を返す。 */
    fun load(): Settings {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: read().also { cached = it }
        }
    }

    /** 設定を保存する。書き込みは非同期（`apply`）だがメモリ上の値は即座に更新される。 */
    fun save(settings: Settings) {
        synchronized(this) {
            cached = settings
            prefs.edit().putString(KEY_SETTINGS, SettingsJson.encode(settings)).apply()
        }
    }

    /** 現在の設定を変換して保存する。 */
    fun update(transform: (Settings) -> Settings): Settings {
        synchronized(this) {
            val updated = transform(load())
            save(updated)
            return updated
        }
    }

    /**
     * エクスポート用の JSON 文字列。
     *
     * 端末固有の値（回線の識別子）は他の端末で意味を持たないため含めない。
     */
    fun exportJson(): String =
        SettingsJson.encode(load().copy(disabledPhoneAccountIds = emptySet()))

    /**
     * JSON 文字列から設定を復元して保存する。
     *
     * 読めなかった場合は false を返し、既存の設定を変更しない。
     * SIM の設定は端末固有なので、インポート元の値ではなく現在の値を維持する。
     */
    fun importJson(text: String): Boolean {
        val current = load()
        val imported = SettingsJson.decode(text, fallback = current)
        if (imported == current) {
            // 内容が同一、あるいは 1 つも読み取れなかった場合。後者と区別できないが、
            // どちらの場合も「変更なし」で正しい。
            return Json.parse(text) != null
        }
        save(imported.copy(disabledPhoneAccountIds = current.disabledPhoneAccountIds))
        return true
    }

    private fun read(): Settings {
        val raw = try {
            prefs.getString(KEY_SETTINGS, null)
        } catch (e: ClassCastException) {
            Log.w(TAG, "settings entry had an unexpected type; falling back to defaults", e)
            null
        }
        return SettingsJson.decode(raw)
    }

    companion object {
        private const val TAG = "SettingsStore"
        private const val PREFS_NAME = "prefix_dialer_settings"
        private const val KEY_SETTINGS = "settings_json"
    }
}
