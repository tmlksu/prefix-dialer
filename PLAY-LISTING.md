# Prefix Dialer — Google Play 提出用メモ（play フレーバー）

Play 提出の要件・チェックリスト・スケジュールは `PLAY-RELEASE.md` を参照。本ファイルは Console に貼る文面と回答の草案である。

作成日: 2026-09-18。対象バージョン: versionCode `1` / versionName `1.0.0`。
このファイルは Play Console への入力作業用の草案と検証記録である。
**秘密情報（keystore.properties の中身、パスワード類）は一切記載しない。**

根拠資料: `README.md` / `CHANGELOG.md` / `PRIVACY.md` / `DECISIONS.md` /
`app/build.gradle.kts` / `app/src/main/AndroidManifest.xml` /
`app/src/main/res/values/strings.xml` / `app/src/main/res/values-ja/strings.xml`。

---

## 1. 検証結果（2026-09-18 実施）

### 1.1 ビルド

- コマンド: `./gradlew --no-daemon assemblePlayRelease bundlePlayRelease`
  （`JAVA_HOME=$HOME/android-build/jdk`、`ANDROID_HOME=$HOME/android-build/sdk`）
- 結果: **BUILD SUCCESSFUL**（所要時間 1 分 4 秒。Gradle 計測 1m 3s、56 タスク中 18 実行・38 up-to-date の増分ビルド）
- 警告: 増分ビルドのため新規の警告出力なし。`keystore.properties` ありのため署名設定ありでビルドされた（未署名警告は出ていない）。
- 成果物:
  - APK: `app/build/outputs/apk/play/release/app-play-release.apk`（3,796,028 bytes）
  - AAB: `app/build/outputs/bundle/playRelease/app-play-release.aab`（4,330,340 bytes）

### 1.2 APK の検査

- 権限（`aapt2 dump permissions`）:
  - `android.permission.READ_PHONE_STATE` のみ。
  - `android.permission.INTERNET` **なし**、`READ_CALL_LOG` / `WRITE_CALL_LOG` **なし**、`READ_CONTACTS` **なし**を確認。
  - 参考: `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`（自アプリ定義・androidx 由来）は含まれる。
- マニフェスト・dex 内の文字列検査: `CALL_LOG` / `READ_CONTACTS` / `android.permission.INTERNET` の文字列は AndroidManifest・dex 内に**ゼロ件**（`CallLogRewrite` は play 版では no-op 実装。`app/src/play/.../CallLogRewrite.kt`）。
- badging（`aapt2 dump badging | grep`）:
  - package: `io.github.tmlksu.prefixdialer` / versionCode `1` / versionName `1.0.0`
  - minSdkVersion `29`（Android 10）/ targetSdkVersion `35` / compileSdk `35`
  - application-label: `Prefix Dialer`（全ロケール同一）
  - launchable-activity: `io.github.tmlksu.prefixdialer.MainActivity`
- ネイティブライブラリ（`unzip -l | grep '\.so$'`）: **なし**（`.so` ゼロ件）。
- 署名（`apksigner verify --print-certs`）:
  - `verify: OK`。v2 署名 **true**（v1 / v3 / v3.1 / v4 は false）。
  - 証明書 DN: `CN=Tmlk, OU=su, O=Individual`、SHA-256: `5a0a68bb361ae02e8c6457185dc8df965d751ed094f595ecace0e33a60b08de5`。
  - （パスワード等の秘密情報は扱わない・書かない）

### 1.3 mapping.txt と AAB

- `app/build/outputs/mapping/playRelease/mapping.txt`: **あり**、27,082,919 bytes（約 25.8 MiB）。
- AAB（4,330,340 bytes）内の `.so`: **なし**。なお AAB には `BUNDLE-METADATA/.../proguard.map` が同梱されている（難読化マッピングはバンドル経由で Play 側に渡る）。

---

## 2. ストア掲載アセットの棚卸し

| # | アセット | 要件（目安） | 状態 |
|---|---|---|---|
| 1 | アプリアイコン 512×512 PNG | 512×512、32bit PNG | **ない**。現状はアダプティブアイコン XML のみ（`drawable/ic_launcher_foreground.xml`＋`values/ic_launcher_background.xml`＋`mipmap-anydpi-v26/ic_launcher*.xml`） |
| 2 | フィーチャーグラフィック | 1024×500、JPG/PNG | **ない** |
| 3 | 電話スクリーンショット | 最低 2 枚（JPEG/PNG。機種別に複数推奨） | **ない** |
| 4 | 短い説明（ja / en） | 各 80 字以内 | **ない**（§3 に草案あり） |
| 5 | 詳細説明（ja / en） | 各 4000 字以内 | **ない**（§3 に草案あり） |
| 6 | プライバシーポリシー URL | 公開 URL が必須 | ページは用意済み（`docs/privacy.html` → `https://tmlksu.github.io/prefix-dialer/privacy.html`）。**GitHub Pages の有効化（Settings → Pages → main / `/docs`）が未実施**。原本は `PRIVACY.md` |
| 7 | 連絡先メールアドレス | Play Console 登録に必須 | **未確認**（本棚卸しの対象外。要準備） |
| 8 | アプリカテゴリ・対象年齢等の申告 | Console 上で回答 | **未回答**（§6・§7 に想定回答あり） |

### 512×512 PNG アイコン生成手順案（未実行）

現行アセット: 前景ベクター（受話器＋3 ドット、`ic_launcher_foreground.xml`、安全領域内に 0.58 縮小済み）、背景色 `#1B5E5A`（`ic_launcher_background.xml`）、monochrome あり。背景は単色なのでラスター化が容易。

- 案 A（推奨）: Android Studio の **Image Asset Studio**（`File → New → Image Asset`、Launcher Icons）で既存アダプティブアイコンから生成し、プレビューから 512×512 の PNG を書き出す。マスク適用後の見え方（Galaxy の squircle で外周が切れないこと＝D-17 の対応済み範囲）がその場で確認できる。
- 案 B（CLI）: `rsvg-convert` または ImageMagick（`convert` / `magick`）で前景 SVG 化→背景色 `#1B5E5A` と合成→512×512 PNG にリサイズ。ベクター XML は Android 固有形式なので、そのままでは読めない。pathData を SVG の `d` 属性へ移植する一手間が要る（単純な図形なので移植は容易）。実行時は背景→前景の順に重ね、アルファ（ドット 0.75）を保持すること。
- 注意: Play 用アイコンは**マスク済みの完成形**（角丸 squircle 等を焼き込んだものではなく、フルブリードの正方形）で出す。透過背景は避け、背景色で全面を埋める。

---

## 3. ストアの掲載情報（草案・そのまま貼れる文面）

アプリ名: **Prefix Dialer**（13 字。ja / en 共通、30 字以内）

### 3.1 短い説明（80 字以内）

- ja（39 字）:
  > 発信前に事業者プレフィックスを自動付与。国内通話の料金節約を助けるアプリです。
- en（75 字）:
  > Auto-adds your carrier prefix before you call. Cut costs on Japanese calls.

### 3.2 詳細説明（4000 字以内）

#### ja（827 字）

> 発信する電話番号の先頭に、あなたが契約している中継電話事業者のアクセス番号（例: 0063）を自動で付けます。標準の電話アプリはそのまま使えます。
>
> 【主な機能】
> ・番号種別（携帯 / 固定 / IP電話）ごとのプレフィックス設定
> ・事業者プリセット（G-Call / 楽天でんわ。いずれも公開資料で動作を確認したもののみ収録）
> ・マスタースイッチ、回線（SIM）ごとの ON/OFF、ローミング中の自動停止
> ・番号単位の除外リスト
> ・発信記録（各発信で何をしたか、付けなかった場合はその理由を表示）
> ・設定の JSON エクスポート / インポート
> ・日本語 / 英語対応
>
> 【Play 版と GitHub 版の違い】
> ・この Play 版には「通話履歴の書き換え」機能がありません。発信後の通話履歴にはプレフィックス付きの番号が残ります。
> ・GitHub で配布している版には、履歴の番号を元に戻す機能（任意・初期設定 OFF）があります。基本のプレフィックス付与機能はどちらも同じです。
>
> 【対象】
> ・日本の電話番号への発信が対象です。海外の番号には対応していません。
> ・Android 10 以上が必要です（通話リダイレクト機能を利用します）。
> ・通話リダイレクトの役割は端末に 1 アプリしか設定できません。他の番号書き換え・着信拒否系アプリとは併用できません。
>
> 【安全への配慮】
> ・緊急通報・3 桁の特番・# や * を含む番号・フリーダイヤル等は、どんな設定でも書き換えません。常にそのまま発信されます。
>
> 【プライバシー】
> ・このアプリはインターネット権限を持たず、情報を外部へ送信できません。発信記録や設定は端末内にのみ保存されます。
>
> 【料金について】
> ・プレフィックスを利用するには、各事業者との契約・申込みが別途必要です。このアプリは通話料金の割引を保証するものではありません。実際の料金はご契約の事業者の明細でご確認ください。

#### en（1763 字）

> Automatically adds the access code of the relay carrier you have subscribed to (e.g. 0063) to the beginning of the phone number you dial. Keep using your normal phone app.
>
> [Features]
> - Per-type prefix settings (mobile / landline / VoIP 050)
> - Carrier presets (G-Call / Rakuten Denwa; only carriers whose behaviour was verified from public sources)
> - Master switch, per-SIM-line on/off, auto-pause while roaming
> - Per-number exclusion list
> - Call records (see what the app did on each call, with the reason when no prefix was added)
> - JSON export / import of settings
> - Japanese / English
>
> [Play edition vs GitHub edition]
> - This Play edition has no call log rewriting feature. After a call, the number with the prefix remains in your call history.
> - The edition distributed on GitHub can restore the original number in the call history (optional, off by default). The core prefix feature is identical in both.
>
> [Requirements]
> - Only calls to Japanese phone numbers are rewritten. International numbers are left unchanged.
> - Requires Android 10 or later (uses the call redirection feature).
> - Only one app on the device can hold the call redirection role. It cannot be used together with other number-rewriting or call-blocking apps.
>
> [Safety]
> - Emergency calls, three-digit service numbers, numbers containing # or *, and toll-free numbers are never rewritten, whatever the settings. They are always dialled unchanged.
>
> [Privacy]
> - The app holds no internet permission and cannot send any information anywhere. Records and settings stay only on your device.
>
> [Charges]
> - Using a prefix requires a separate contract or registration with each carrier. This app does not guarantee any discount on call charges. Please check your actual charges on your carrier bill.

備考: 事業者名（G-Call / 楽天でんわ・Rakuten Denwa）はプリセット名としての事実記載のみ。割引・料金の保証表現は入れていない。

---

## 4. Data safety（データ セーフティ）回答案

前提: play 版 APK は `INTERNET` 権限なし（§1.2 で実測）。ネットワーク送信の手段自体がなく、解析・クラッシュ報告・広告・トラッキングは組み込んでいない（README / PRIVACY.md）。

| 質問 | 回答 | 根拠（PRIVACY.md） |
|---|---|---|
| ユーザーデータを収集するか | いいえ（収集・共有ともなし） | 「要約: いかなる情報も収集・送信しません」「開発者が受け取る情報: ありません」 |
| 第三者と共有するか | いいえ | 同上。送信手段自体がない |
| 送信時の暗号化 | 該当なし（送信しないため） | INTERNET 権限なし＝技術的に接続不可 |
| 削除リクエストの手段 | 該当なし（サーバーにデータなし）。端末内データはアプリ内消去＋アンインストールで全削除 | 「データの削除: 発信記録→記録を消去／アンインストールで完全削除。サーバー上にデータが存在しないため削除依頼は不要」 |
| 電話番号（発信先番号） | 収集しない。端末内でのみ判定に使用し、発信記録として端末内に残るのみ | 「アプリが端末内で扱う情報」表・「発信記録について」（直近 100 件、アプリ内消去可） |
| 通話履歴 | 収集しない。**Play 版には履歴書き換え機能自体がなく、権限宣言もない** | 権限表（CALL_LOG は履歴書き換え有効時のみ＝Play 版は対象外）、§1.2 の実測 |
| 連絡先（表示名） | 収集しない。Play 版では使用しない | 同上（連絡先使用は履歴書き換え時のみ＝Play 版は対象外） |
| 端末 ID・広告 ID 等 | 収集しない（該当機能なし） | 「アクセス解析、クラッシュレポート送信、広告、トラッキングのいずれも組み込まれていません」 |
| 位置情報・写真・ファイル等 | 収集しない（使用しない） | 同上 |
| 発信先事業者への送信では？ | アプリは送信しない。プレフィックス付き発信が事業者網を経由する点は掲載文で開示 | 「発信先への影響」（事業者の中継サービス経由となり、取扱いは各事業者のポリシーに従う） |

注意: 発信記録は「アプリ自身の動作記録」であり Android の通話履歴とは別物である旨を、審査で問われたら PRIVACY.md の「発信記録について」を引用する。

---

## 5. アプリのアクセス（App access）欄・審査担当者向け手順

ログイン不要。アカウント・パスワード・有料契約は不要。以下 en / ja。

### English

> No login is required. No account, password, or paid contract is needed.
>
> 1. Open the app. On the home screen ("Prefix Dialer"), tap "Turn on" if "Call redirection is off" is shown, and grant the call redirection role to this app in the system screen.
> 2. Turn the master switch ("Add prefix") on.
> 3. Open "Rewrite rules", choose a preset or enter a prefix (e.g. `0063`) for Mobile, and set "Leading zero of the original number" to "Keep".
> 4. To review what the app did on real calls, open "Call records" ("What was done on each call"). After any call you place yourself, this screen shows whether a prefix was added or the reason it was not (e.g. "Emergency or service number (always protected)", "The feature is paused", "Roaming"). Reviewers who do not wish to place a call can confirm the full behaviour from this screen's descriptions and the "Never rewritten, whatever the settings" section on the "Rewrite rules" screen.
> 5. To verify the rewriting result without placing a call, open "Rewrite rules" and look below each rule: a monospace sample line shows the result, e.g. with the G-Call preset the Mobile row reads `09012345678 → 006309012345678` (Landline: `0312345678 → 00630312345678`; VoIP: `05012345678 → 006305012345678`). The same `sample → result` summary appears under "Current settings" on the home screen. Turn the master switch ("Add prefix") OFF and each line changes to `09012345678 → Paused`. No real call is needed, and no emergency or consultation numbers are involved.
> 6. Note: the "Advanced" screen in this Play edition shows "SIM, roaming, backup" only. There is no call log rewriting feature (no call log permission is ever requested).

### 日本語

> ログインは不要です。アカウント・パスワード・有料契約は不要です。
>
> 1. アプリを開く。ホーム画面（「Prefix Dialer」）に「通話リダイレクトが無効です」と出たら「有効にする」を押し、システム画面で通話リダイレクトの役割をこのアプリに付与する。
> 2. マスタースイッチ（「プレフィックスを付ける」）を有効にする。
> 3. 「書き換えルール」を開き、プリセットを選ぶか、携帯電話向けにプレフィックス（例 `0063`）を入力し、「元番号の先頭 0 の扱い」を「残す」にする。
> 4. 実際の発信で何をしたかは「発信記録」（「各発信で何をしたかの記録」）で確認できる。発信後はここに、プレフィックスの有無と、付けなかった場合の理由（例「緊急通報・特番のため（設定に関わらず保護されます）」「機能が停止中のため」「ローミング中のため」）が残る。発信したくない審査担当者は、この画面の説明と「書き換えルール」画面の「設定に関わらず書き換えない番号」の節で全挙動を確認できる。
> 5. 実発信なしで書き換え結果を確認するには「書き換えルール」を開き、各ルールの下の等幅フォントのサンプル表示を見る。G-Call プリセットなら携帯電話の行は `09012345678 → 006309012345678`（固定電話 `0312345678 → 00630312345678`、IP電話 `05012345678 → 006305012345678`）と出る。ホーム画面の「現在の設定」にも同じ「サンプル → 結果」の要約が出る。マスタースイッチ（「プレフィックスを付ける」）を OFF にすると各行が `09012345678 → 停止中` に変わる。実発信は不要で、緊急・相談番号も使わない。
> 6. 補足: Play 版の「詳細設定」に表示されるのは「SIM・ローミング・バックアップ」のみ。通話履歴の書き換え機能はなく、通話履歴の権限も要求されない。

---

## 6. コンテンツ レーティング（IARC）質問票の想定回答

| 項目 | 想定回答 | 理由 |
|---|---|---|
| 暴力・流血・残虐表現 | なし | 電話発信用ユーティリティ。画像・映像・音声コンテンツを持たない |
| 性的コンテンツ・ヌード | なし | 同上 |
| 強い言葉・冒涜 | なし | UI 文言は設定・記録表示のみ |
| 薬物・アルコール・タバコ | なし | 言及も表示もなし |
| ギャンブル（現金・模擬含む） | なし | なし |
| 恐怖・ホラー要素 | なし | なし |
| ユーザー生成コンテンツ（UGC）の共有・公開 | なし | 除外番号・設定・発信記録は端末内のみ。共有・公開機能なし（INTERNET 権限なし） |
| ユーザー間コミュニケーション | なし | チャット・投稿機能なし |
| 位置情報の共有 | なし | 位置情報を使用しない |
| 個人情報の共有 | なし | 端末外送信なし（§4 参照） |
| アプリ内購入・課金・広告 | なし | 広告・課金機能なし。事業者契約は端末外の話でアプリ内購入ではない |
| ニュース | 該当しない | ニュースアプリではない |
| 対象年齢 | 13 歳未満向けではない（子ども向けではない）。年齢情報は収集しない | PRIVACY.md「子どもの利用について」 |

---

## 7. その他の申告の想定回答

| 項目 | 想定回答 |
|---|---|
| 広告の有無 | 広告なし（広告 SDK なし） |
| 対象年齢 | 13 歳未満向けではない。一般向けツール |
| ニュースアプリか | いいえ |
| 政府アプリか | いいえ |
| 金融機能（送金・融資・暗号資産等） | なし |
| 健康・医療機能 | なし（健康アプリではない） |
| カテゴリ | 第一候補「ツール」、代替「通信」 |
| 権限の申告 | `READ_PHONE_STATE` は「回線ごとの設定」画面での回線名表示にのみ使用（判定には不使用）。CALL_LOG 系は Play 版に存在しない（§1.2 実測） |

---

## 8. 不足アセットと未決事項

### 不足アセット（§2 の再掲・対応要）

1. 512×512 PNG アプリアイコン（§2 手順案 A/B で作成）
2. フィーチャーグラフィック 1024×500
3. 電話スクリーンショット 2 枚以上（実機 Galaxy S25 等で撮影。緊急・相談番号は映さない）
4. プライバシーポリシーの公開 URL（`PRIVACY.md` の公開先を確定し Console に登録）
5. 連絡先メールアドレス（Console 登録用）
6. §3〜§7 の草案の人間による最終確認（特に料金・事業者名の表現）

### 未決事項（DECISIONS.md 由来）

- **D-20: 初回登録時に `release.keystore` を「アプリ署名鍵」としてアップロードすること。後から変更不可。** 既定のまま進めると Google 生成鍵になり GitHub 版と互換が切れる。`release.keystore` と `keystore.properties` のバックアップを別所に保管。
- versionCode は 2 以上に上げる（1 は GitHub 1.0.0 で使用済み。PLAY-RELEASE.md §8。versionName は `1.1.0` が実態に合う）。
- D-10 applicationId（`io.github.tmlksu.prefixdialer`）は公開後に変更不可。提出前に確定すること。
- Play 版の実機確認（H-05 相当: 署名済み play リリースでの発信・設定画面・発信記録）は Console 提出前に実施すること。
- targetSdk 36 への引き上げが必須（2026-08-31 から新規アプリは API 36 必須。PLAY-RELEASE.md §1）。提出作業より先決。
- `allowBackup` の判断（現状 `true` では発信記録が Google ドライブへ送られ PRIVACY.md と不整合。`false` 推奨。PLAY-RELEASE.md P-01）。

---

## 9. リリースノート（Play Console「このリリースの新機能」・各 500 字以内）

初回リリース用。GitHub 版から見た変更点（targetSdk 36 など）は Play ユーザーには初回なので書かない。

```
<ja-JP>
Google Play での初回リリースです。

・発信時に、携帯 / 固定 / IP電話の種別ごとに事業者プレフィックスを自動で付けます
・プリセット: G-Call、楽天でんわ
・緊急通報・3桁の特番・# や * を含む番号は、設定に関わらず書き換えません
・回線（SIM）ごとの ON/OFF、ローミング中の自動停止、番号単位の除外
・発信記録で、各発信で何をしたか（付けなかった理由）を確認できます
・インターネット権限なし。情報を外部に送信しません

この Play 版には「通話履歴の書き換え」機能はありません。発信後の履歴にはプレフィックス付きの番号が残ります。必要な場合は GitHub で配布している版をご利用ください。
</ja-JP>
<en-US>
First release on Google Play.

• Automatically adds your carrier prefix when calling Japanese mobile, landline and VoIP numbers
• Presets: G-Call, Rakuten Denwa
• Emergency numbers, 3-digit service numbers and numbers containing # or * are never rewritten, whatever the settings
• Per-SIM on/off, auto-pause while roaming, per-number exclusions
• Call records show what the app did on each call, and why a prefix was not added
• No internet permission. Nothing leaves your device

This Play edition does not include call log rewriting; the prefixed number stays in your call history. If you need it, use the edition distributed on GitHub.
</en-US>
```
