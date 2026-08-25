# プライバシーポリシー / Privacy Policy

**Prefix Dialer**

最終更新: 2026-08-26

日本語版が原文です。An English translation follows the Japanese text.

---

## 日本語

### 要約

**Prefix Dialer は、いかなる情報も収集・送信しません。**

このアプリは**インターネット権限（`android.permission.INTERNET`）を持っていません**。
これは方針ではなく技術的な制約です。Android は宣言されていない権限の使用を許さないため、
このアプリはネットワークに接続すること自体ができません。したがって、あなたの電話番号・
通話履歴・連絡先が端末の外へ出ることはありません。

この事実は、配布している APK を検査すれば誰でも確認できます:

```
aapt2 dump permissions app-release.apk
```

出力に `android.permission.INTERNET` が含まれないことを確認してください。

### 開発者が受け取る情報

**ありません。** 開発者はあなたの利用状況を一切知ることができません。
アクセス解析、クラッシュレポート送信、広告、トラッキングのいずれも組み込まれていません。

### アプリが端末内で扱う情報

以下はすべて**あなたの端末の中だけ**で処理され、端末外へ送信されることはありません。

| 情報 | 用途 | 保存されるか |
|---|---|---|
| 発信しようとしている電話番号 | プレフィックスを付けるかどうかの判定 | 発信記録として端末内に残る |
| 通話履歴 | 発信後に番号を元に戻す（**任意機能。既定では無効**） | 保存しない。書き換え後は参照しない |
| 連絡先の表示名 | 書き換えた通話履歴に名前を補完する（**任意機能**） | 保存しない |
| 回線（SIM）の名前 | 設定画面で回線ごとの ON/OFF を表示する | 保存しない |
| あなたの設定 | ルール・除外番号などの保持 | 端末内に保存される |
| 発信記録 | 各発信で何をしたかをあなたが確認するため | 端末内に既定 100 件まで保存される |

### 発信記録について

アプリ内の「発信記録」には、発信した番号・書き換え後の番号・書き換えなかった理由が残ります。
これは**アプリ自身の動作記録**であり、Android の通話履歴とは別物です。

- 端末内にのみ保存されます
- 既定で直近 100 件まで、古いものから消えます
- アプリ内の「記録を消去」でいつでも全削除できます

### 権限について

**プレフィックスを付ける基本機能は、通話履歴の権限を一切必要としません。**

| 権限 | いつ要求するか | 用途 |
|---|---|---|
| 通話リダイレクト（`ROLE_CALL_REDIRECTION`） | 初回セットアップ | 発信直前に番号を書き換える。この機能の中核 |
| `READ_PHONE_STATE` | 「回線ごとの設定」を開いたときのみ | 設定画面に回線名を表示する。**番号の判定には使いません** |
| `READ_CALL_LOG` / `WRITE_CALL_LOG` | 「通話履歴の書き換え」を有効にしたときのみ | 発信後に履歴の番号を元に戻す |
| `READ_CONTACTS` | 同上 | 書き換えた履歴に連絡先の名前を補完する |
| `POST_NOTIFICATIONS` | 同上 | 履歴書き換え中の通知を表示する |

通話履歴と連絡先の権限は、インストール直後には要求しません。
あなたが「通話履歴の書き換え」を有効にした瞬間に初めて求めます。
この機能を使わなければ、これらの権限は一度も要求されません。

### 発信先への影響

このアプリは発信番号の先頭に事業者識別番号（例: `0063`）を付けます。
これにより、**あなたが契約している電話会社の中継サービスを経由して発信されます**。
その通話に関する情報の取り扱いは、当該電話会社のプライバシーポリシーに従います。
本アプリの開発者はその通信内容に一切関与しません。

なお、緊急通報番号（110 / 118 / 119 / 112）および `#` から始まる相談ダイヤル、
3 桁の特番は、**設定内容に関わらず書き換えられません**。常にそのまま発信されます。

### データの削除

- 発信記録: アプリ内の「発信記録」→「記録を消去」
- 設定を含むすべてのデータ: アプリをアンインストールすると完全に削除されます

サーバー上にデータが存在しないため、開発者への削除依頼は不要です。

### 子どもの利用について

このアプリは特に子どもを対象としたものではなく、年齢に関わる情報を収集しません。

### 本ポリシーの変更

変更した場合は、このファイルの更新履歴（Git のコミット履歴）に残ります。
重要な変更がある場合はリリースノートでも告知します。

### お問い合わせ

GitHub リポジトリの Issues からご連絡ください。

---

## English

### Summary

**Prefix Dialer does not collect or transmit any information.**

The app does **not hold the internet permission** (`android.permission.INTERNET`).
This is a technical constraint rather than a policy promise: Android does not permit an app
to use a permission it has not declared, so this app is incapable of connecting to a network.
Your phone numbers, call log and contacts therefore cannot leave your device.

Anyone can verify this by inspecting the distributed APK:

```
aapt2 dump permissions app-release.apk
```

Confirm that `android.permission.INTERNET` does not appear in the output.

### What the developer receives

**Nothing.** The developer has no way of knowing how you use the app.
There is no analytics, no crash reporting, no advertising and no tracking.

### What the app handles on your device

All of the following is processed **only on your device** and is never transmitted.

| Data | Purpose | Stored? |
|---|---|---|
| The number you are about to dial | Deciding whether to add a prefix | Kept on device as a call record |
| Call log | Restoring the original number after a call (**optional, off by default**) | Not stored |
| Contact display names | Filling in the name on a rewritten call log entry (**optional**) | Not stored |
| SIM line names | Showing per-line switches in settings | Not stored |
| Your settings | Rules, exclusions and so on | Stored on device |
| Call records | So you can check what the app did on each call | Stored on device, 100 entries by default |

### Call records

The in-app "call records" screen lists the number dialled, the rewritten number, and the
reason when no prefix was added. This is **the app's own activity log**, distinct from
Android's call history.

- Stored only on your device
- Limited to the 100 most recent entries by default; older entries are dropped
- Can be erased at any time from within the app

### Permissions

**The core feature — adding a prefix — requires no call log permission at all.**

| Permission | When requested | Purpose |
|---|---|---|
| Call redirection role | Initial setup | Rewriting the number just before dialling. The core of the app |
| `READ_PHONE_STATE` | Only when you open per-line settings | Showing line names in settings. **Not used to decide anything about a number** |
| `READ_CALL_LOG` / `WRITE_CALL_LOG` | Only when you enable call log rewriting | Restoring the original number after a call |
| `READ_CONTACTS` | Same as above | Filling in the contact name on a rewritten entry |
| `POST_NOTIFICATIONS` | Same as above | Showing a notification while rewriting |

Call log and contacts permissions are never requested at install time. They are requested
only at the moment you enable call log rewriting. If you do not use that feature, they are
never requested at all.

### Effect on your calls

The app prepends a carrier access code (for example `0063`) to the number you dial, which
routes the call through the carrier service you have subscribed to. How that carrier handles
information about the call is governed by that carrier's own privacy policy. The developer of
this app has no involvement in it.

Emergency numbers (110 / 118 / 119 / 112), `#` consultation lines and three-digit service
numbers are **never rewritten, regardless of your settings**. They are always dialled as-is.

### Deleting your data

- Call records: in-app "Call records" → "Clear records"
- Everything including settings: uninstalling the app removes it completely

No data exists on any server, so there is no deletion request to make.

### Children

This app is not directed at children and does not collect any age-related information.

### Changes to this policy

Any change is recorded in this file's history (the Git commit log). Significant changes are
also announced in the release notes.

### Contact

Please open an issue on the GitHub repository.
