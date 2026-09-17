package io.github.tmlksu.prefixdialer

/**
 * アプリから開く外部 URL。
 *
 * 文字列リソースにはしない。翻訳の対象ではないうえ、ロケールごとに別の URL に
 * なってしまうと、プライバシーポリシーの掲載先が分裂する。Google Play は
 * 「ストアに登録した URL」と「アプリ内の導線」が同じ内容を指していることを
 * 前提にしているので、1 か所に固定する（PLAY-RELEASE.md §5）。
 */
object AppLinks {

    /**
     * プライバシーポリシーの公開先。原本は `PRIVACY.md`、`docs/privacy.html` が同期版。
     *
     * GitHub Pages に置いているのは、閲覧専用で編集ボタンの出ないページが
     * Play の "non-editable" 要件に素直に沿うため。
     */
    const val PRIVACY_POLICY = "https://tmlksu.github.io/prefix-dialer/privacy.html"

    /** ソースコード。何をしているアプリかを自分で確かめられることが、このアプリの主張の裏付け。 */
    const val SOURCE_CODE = "https://github.com/tmlksu/prefix-dialer"
}
