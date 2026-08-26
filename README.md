# Prefix Dialer

発信時に、国内の携帯・固定電話番号へ自動で事業者プレフィックス（例: `0063`）を付ける Android アプリ。
標準の電話アプリを置き換えず、`CallRedirectionService`（Android 10+）で発信直前に番号を書き換える。

対象端末: Samsung Galaxy S25 / One UI 7（Android 15）で確認。minSdk 29 / targetSdk 35。

## ダウンロード

[Releases](https://github.com/tmlksu/prefix-dialer/releases/latest) から APK を取得してください。

現在の最新は **1.0.0**。変更履歴は [CHANGELOG.md](CHANGELOG.md)、
今後の予定は [ROADMAP.md](ROADMAP.md) を参照。

## プライバシー

**このアプリは情報を一切収集・送信しない。**

インターネット権限（`android.permission.INTERNET`）を**持っていない**。これは方針ではなく
技術的な制約で、Android は宣言されていない権限の使用を許さないため、ネットワークに接続すること
自体ができない。配布 APK を検査すれば誰でも確認できる:

```
aapt2 dump permissions app-release.apk   # INTERNET が無いことを確認
```

解析・クラッシュレポート・広告・トラッキングのいずれも組み込んでいない。
詳細は [PRIVACY.md](PRIVACY.md) を参照。

## できること

- 番号種別（携帯 / 固定 / IP電話）ごとにプレフィックスと先頭 `0` の扱いを設定
- 事業者プリセット（G-Call / 楽天でんわ）
- マスタースイッチ、回線（SIM）ごとの ON/OFF、ローミング中の自動停止
- 番号単位の除外リスト
- 発信記録 — 各発信で何をしたか、付かなかった場合はその理由
- 通話履歴を元番号へ書き戻す（**オプトイン**。既定 OFF）
- 設定の JSON エクスポート / インポート

## 安全性の設計

このアプリは発信番号を書き換える。**誤判定のコストが非対称**であることを設計の起点にしている。

| | 影響 |
|---|---|
| 過剰にブロック | プレフィックスが付かない（割引が効かないだけ、実害なし） |
| ブロック漏れ | 緊急通報が書き換わる / 意図しない回線で課金事故 |

したがって、迷ったら必ずブロック側に倒す。

### 緊急通報のハードガード

`ProtectedNumbers` が、ユーザー設定より**上位の安全層**として動く。ここで保護された番号は
どんなルール設定でも書き換えられない。意図的に凶悪なルールセットでも貫通しないことを
テストで固定している。

一覧を列挙して維持する方式は採らない。総務省の 1XY 割当には改廃があり（例: `177` 天気予報は
2025-03-31 終了）、`#` 系 4 桁は所管がばらばらで公式の網羅リストが存在しないため、
リストの更新漏れがそのまま緊急通報の書き換え事故になる。代わりに範囲で弾く:

- **3 桁以下の番号はすべて対象外** — 1XY 帯は特番用の予約帯。プレフィックスを付けて得をする
  3 桁番号は存在しない
- **`#` / `*` を含む番号はすべて対象外** — `#7119` `#8000` `#9110` `#8103` と将来の追加を一括で守る
- `184` / `186`（発信者番号通知）で始まる番号、DTMF ポーズを含む番号も素通し

libphonenumber の短縮番号データは**主軸に使えない**（8.13.42 で実測）:

| 番号 | `isEmergencyNumber` | `isValidShortNumber` |
|---|---|---|
| `110` `119` | ✅ | ✅ |
| `118`（海上保安庁） | ❌ | ✅ |
| `112`（携帯網で警察へ） | ❌ | ❌ |
| `117` `171` `188` `113` `115` `116` | ❌ | ❌ |

さらに `isPossibleShortNumberForRegion` は `09012345678` にも `true` を返すため、
ガードに使うと通常の携帯番号まで巻き込む。よってライブラリは補助の網としてのみ使う。

## ルールモデル

書き換えは `RuleSet`（ルールの並び）で表現する。上から評価し、最初にマッチしたものを適用する。

```kotlin
RuleSet(name = "G-Call", rules = listOf(
    DialRule(RuleCondition.OfType(NumberCategory.MOBILE),     RuleAction.Apply("0063", LeadingZero.KEEP)),
    DialRule(RuleCondition.OfType(NumberCategory.FIXED_LINE), RuleAction.Apply("0063", LeadingZero.KEEP)),
))
```

先頭 `0` の扱い（`LeadingZero`）は **ルールごと** に持つ。事業者によっては携帯向けと固定向けで
プレフィックスも先頭 0 の扱いも異なるため、事業者単位・プリセット単位では表現できない。

| `LeadingZero` | `09012345678` に適用した結果 |
|---|---|
| `KEEP` | `0063` + `09012345678` |
| `STRIP` | `prefix` + `9012345678` |
| `TO_COUNTRY_CODE` | `prefix` + `819012345678` |

`RuleAction.PassThrough` を上位に置けば、広いルールから特定の番号帯だけを抜ける。

### 判定の順序

1. ローミング / 回線ごとの無効化
2. **`ProtectedNumbers`** — 緊急通報・特番。ルールでは覆せない
3. マスタースイッチ
4. 個別除外リスト
5. 二重付与・他社プレフィックス（`00XY`）・国際発信（`010`）の除外
6. 日本の有効な番号かどうか
7. **種別による除外** — フリーダイヤル `0120`/`0800`、ナビダイヤル `0570`、有料情報 `0990`。
   これもルールでは覆せない
8. ルールの評価

2 と 7 がルールより上位にあるのが要点。プレフィックスやルールを UI から自由に編集できても、
この 2 つはバイパスされない。

## 権限

**基本機能（プレフィックス付与）は通話履歴の権限を一切必要としない。**

| 権限 | いつ要求するか | 何に使うか |
|---|---|---|
| `ROLE_CALL_REDIRECTION` | 初回セットアップ | 発信直前の番号書き換え。端末に 1 アプリのみ |
| `READ_PHONE_STATE` | 回線ごとの設定を開いたとき | 設定画面に回線名を表示する。**判定には使わない** |
| `READ/WRITE_CALL_LOG` `READ_CONTACTS` `POST_NOTIFICATIONS` | 履歴書き換えを有効にしたとき | 発信後に履歴を元番号へ戻す |

通話履歴の権限はインストール直後には要求しない。機能を有効化した瞬間に初めて求める。

## ビルド

Android Studio で `PrefixDialer/` を開くだけ。あるいは JDK 17 + Android SDK
(platform-35 / build-tools 35.0.0) があれば同梱の Gradle Wrapper で CLI ビルドできる:

```
./gradlew testDebugUnitTest   # ユニットテスト 116 件
./gradlew assembleDebug       # APK -> app/build/outputs/apk/debug/
./gradlew assembleRelease     # keystore.properties があれば署名される
```

SDK の場所は `local.properties`（`sdk.dir=...`、リポジトリには含めない）か環境変数
`ANDROID_HOME` で指定する。

リリース署名は `keystore.properties.example` をコピーして設定する。
このファイルが無い場合は署名設定を作らず、release ビルドは未署名になる（意図した挙動）。

## 端末での有効化

アプリを起動し、画面の指示に従う:

1. **通話リダイレクトを有効化** — `ROLE_CALL_REDIRECTION` を取得（端末に 1 アプリのみ）
2. **書き換えルールを設定** — プリセットを選ぶか、種別ごとに手で設定
3. （任意）**詳細設定で通話履歴の書き換えを有効化** — 権限とバッテリー最適化の解除を案内

## 動作確認

**緊急通報番号には絶対に発信しないこと。** `110` / `118` / `119` / `112` および
`#7119` / `#8000` / `#9110` / `#8103` は確認に使わない。実害なく確認できるのは
`117`（時報）程度で、これも必要最小限に留める。

発信後、アプリの「発信記録」を開けば、その発信で何をしたか（付かなかった場合はその理由）が分かる。
`adb logcat -s PrefixRedirection CallLogRewrite` でも追える。

## Samsung / One UI の既知の注意点

- **履歴書き換えを使う場合、バッテリー最適化の解除は必須級。** 未設定だと発信後の書き換え
  サービスが即座に殺され、履歴がプレフィックス付きのまま残ることがある
- **Samsung Cloud の通話履歴同期が ON** だと、書き換えがクラウド側の値で巻き戻ることがある
- **純正ダイヤラーの表示キャッシュ**により、履歴一覧の表示が一瞬プレフィックス付きに見えてから
  元番号へ更新されることがある。`CACHED_NAME` も併せて更新して表示ズレを抑えている
- 緊急番号は OS 側でも保護されるが、`ProtectedNumbers` で独立に弾いている（OS の保護に依存しない）

## インストール時の注意

**自分でビルドした debug 版が入っている端末に、配布版（release）を上書きできない。**
`applicationId` は同じだが署名鍵が違うため、Android が更新を拒否する
（`INSTALL_FAILED_UPDATE_INCOMPATIBLE`。「インストールされませんでした」と表示される）。

debug 版をアンインストールしてから release 版を入れること。**設定は消える**ので、
必要なら事前に「詳細設定 → 設定のバックアップ → 書き出す」で JSON を保存しておく。

逆方向（release → debug）も同じ理由で失敗する。

## 制約

- 通話リダイレクトアプリは端末に 1 つだけ。他の番号書き換え／着信拒否系アプリとは排他。
  他アプリに奪われるとプレフィックスが黙って付かなくなるため、アプリ起動時に検知して警告する
- `WRITE_CALL_LOG` は Google Play のセンシティブ権限。履歴書き換えをオプトインにすることで、
  この機能を使わない限り要求しない構成にしている

## ファイル構成

```
app/src/main/java/io/github/tmlksu/prefixdialer/
  ProtectedNumbers.kt         緊急通報・特番のハードガード（設定より上位の安全層）
  DialRule.kt                 ルールのデータモデル（種別 × prefix × 先頭0の扱い）
  RuleEngine.kt               ルール評価器（Android非依存・テスト対象）
  DialDecision.kt             判定結果と、書き換えなかった理由
  Presets.kt                  事業者プリセット（裏取りできたものだけ収録）
  Settings.kt                 設定のデータモデル
  SettingsJson.kt             設定の JSON 相互変換（永続化とエクスポートで共用）
  SettingsStore.kt            設定の永続化（SharedPreferences）
  Json.kt                     依存を持たない最小限の JSON 実装
  CallRecord.kt               発信記録のモデル
  CallRecordStore.kt          発信記録の保存
  PhoneAccounts.kt            回線一覧とローミング状態
  SystemStatus.kt             ロール・権限の状態
  PrefixRedirectionService.kt 発信直前に番号を書き換える
  CallLogRewriteService.kt    発信後に履歴を元番号へ戻す（短命FGS・オプトイン）
  MainActivity.kt             設定画面のホスト
  ui/                         Compose の各画面
```

## ライセンス

MIT（[LICENSE](LICENSE)）。同梱する第三者ソフトウェアの表記は [NOTICE](NOTICE) を参照。
