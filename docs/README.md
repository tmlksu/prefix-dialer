# 配布サイト（GitHub Pages）

`https://tmlksu.github.io/prefix-dialer/` として公開するための静的サイト。
ビルド不要（素の HTML / CSS / JS）。

| ファイル | 役割 |
|---|---|
| `index.html` | トップページ。Play ストアの「ウェブサイト」欄に登録する URL |
| `privacy.html` | プライバシーポリシー。Play Console の「プライバシー ポリシー URL」に登録する（原本は `../PRIVACY.md`） |
| `assets/icon.svg` | アプリアイコン（`ic_launcher_foreground.xml` と背景色から移植）。512×512 PNG の生成元にも使える |
| `assets/style.css` | 共通スタイル。ライト / ダーク両対応 |
| `assets/lang.js` | 日本語 / English 切り替え（`?lang=en`・localStorage・ブラウザ設定の順で判定） |
| `.nojekyll` | Jekyll の処理を止め、置いたファイルをそのまま配信する |

## 公開の手順（リポジトリ管理者の操作）

1. このディレクトリを含むコミットを `main` に push する。
2. GitHub の **Settings → Pages** を開く。
3. **Source** を `Deploy from a branch`、**Branch** を `main` / `/docs` にして Save。
4. 1〜2 分待ち、`https://tmlksu.github.io/prefix-dialer/` と
   `https://tmlksu.github.io/prefix-dialer/privacy.html` が開けることを確認する。

## 手を入れるときの注意

- `PRIVACY.md` を変更したら `privacy.html` も必ず同じ内容に更新する（原本は Markdown 側）。
- Play 公開後は `index.html` の CTA にある Google Play ボタン（コメントアウト中）を有効にする。
  公式バッジ画像を使う場合は <https://play.google.com/intl/en_us/badges/> から取得し、
  ブランドガイドラインに従うこと。
- JS を切っていても両言語が表示されるだけで、内容が欠けないようにしてある。
