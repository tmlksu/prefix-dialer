package io.github.tmlksu.prefixdialer

/**
 * アプリの設定一式。
 *
 * Android に依存しない不変データなので、ローカルユニットテストでそのまま検証できる。
 * 永続化は [SettingsStore]、JSON との相互変換は [SettingsJson] が担当する。
 *
 * ## 既定値の方針
 *
 * 「設定し忘れても勝手に課金が変わらない」「機微な権限を勝手に要求しない」方向に倒す。
 * 判断の経緯は `DECISIONS.md` の D-11〜D-15 を参照。
 */
data class Settings(

    /** 現在有効な書き換えルール。マスタースイッチは `ruleSet.enabled`。 */
    val ruleSet: RuleSet = Presets.default,

    /**
     * 発信後に通話履歴を元番号へ書き戻すか。
     *
     * 既定 false。CALL_LOG は Play の機微な権限であり、書き込み権限を持つアプリは
     * 履歴を消すこともできてしまう。インストール直後には要求せず、
     * ユーザーがこの機能を有効化した瞬間に初めて権限を求める。
     */
    val callLogRewriteEnabled: Boolean = false,

    /**
     * ローミング中はプレフィックスを停止するか。
     *
     * 既定 true。海外で国内向けの事業者プレフィックスを付けても意味がなく、
     * 発信が失敗するか意図しない課金になる可能性がある。安全側に倒す。
     */
    val disableWhileRoaming: Boolean = true,

    /**
     * プレフィックスを適用しない回線の `PhoneAccountHandle.id`。
     *
     * 既定は空＝全 SIM で有効。デュアル SIM で契約していない回線に付けると
     * 課金事故になるため、UI から個別に OFF にできる。
     *
     * 購読 ID(subscriptionId) ではなくこの ID を鍵にしているのは、
     * `CallRedirectionService.onPlaceCall` が受け取る値そのものだから。
     * 判定のたびに `READ_PHONE_STATE` を必要とする API を呼ばずに済む
     * （権限が要るのは設定画面で回線名を表示するときだけ）。
     */
    val disabledPhoneAccountIds: Set<String> = emptySet(),

    /**
     * 個別に除外する番号（数字のみに正規化して保持）。
     *
     * 「この番号だけは絶対に付けない」を表現する。ルールより優先される。
     */
    val excludedNumbers: Set<String> = emptySet(),

    /** アプリ内の発信記録の保持件数。0 なら記録しない。 */
    val callRecordLimit: Int = DEFAULT_CALL_RECORD_LIMIT,
) {

    /**
     * 指定の回線でプレフィックスを適用してよいか。
     *
     * @param phoneAccountId 発信に使われる回線の `PhoneAccountHandle.id`。不明なら null
     */
    fun isEnabledForPhoneAccount(phoneAccountId: String?): Boolean {
        if (!ruleSet.enabled) return false
        if (phoneAccountId == null) return true
        return phoneAccountId !in disabledPhoneAccountIds
    }


    companion object {

        const val DEFAULT_CALL_RECORD_LIMIT = 100

        /** 除外リストの比較用に、番号を数字のみへ正規化する。 */
        fun normalizeNumber(number: String): String = number.filter { it.isDigit() }
    }
}
