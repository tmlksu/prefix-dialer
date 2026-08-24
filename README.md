# Prefix Dialer

発信時に、国内の携帯・固定電話番号へ自動で `0063` プレフィックスを付ける Android アプリ。
標準の電話アプリを置き換えず、`CallRedirectionService`（Android 10+）で発信直前に番号を書き換える。
通話履歴には元番号が残るよう、発信後に `CallLog` を書き戻す（電話帳マッチを維持）。

対象端末: Samsung Galaxy S25 / One UI 7（Android 15）を想定。minSdk 29 / targetSdk 35。

## ステータス

- **MVP 動作中** — S25 実機で発信〜履歴書き換えまで確認済み（2026-08-24）
- 公開アプリ化を見据えて開発中。残タスクは [ROADMAP.md](ROADMAP.md) 参照
  （applicationId のリネーム、署名設定、Call Log 権限の Play ポリシー対応、プライバシーポリシー等）
- 現状の `applicationId` は `io.github.tmlksu.prefixdialer`（公開前に要変更）

## プレフィックスの判定ロジック

`PhoneNumberPrefixer.buildDialNumber()` が心臓部。libphonenumber で正規化・種別判定する。

| 入力 | 判定 | 結果 |
|---|---|---|
| `090…` `080…` `070…`（携帯） | MOBILE | ✅ `0063` + 元番号 |
| `03…` `06…` など（固定） | FIXED_LINE | ✅ |
| `050…`（IP電話） | VOIP | ✅ |
| `+8190…` など | +81→0 に正規化 | ✅ |
| `0120…` `0800…` | TOLL_FREE | ❌ |
| `0570…`（ナビダイヤル） | SHARED_COST | ❌ |
| `0990…` | PREMIUM_RATE | ❌ |
| `+1…` など海外 | 国番号≠81 | ❌ |
| `010…`（国際発信） | 明示除外 | ❌ |
| `0033…` など他社識別番号 | 明示除外 | ❌ |
| `0063…`（付与済み） | 二重防止 | ❌ |
| `110` `119` `118` `112` | 緊急通報（ハードガード） | ❌ |
| `117` `171` `188` `189` など 3桁 | 特番（ハードガード） | ❌ |
| `#7119` `#8000` `#9110` | #系ダイヤル（ハードガード） | ❌ |
| `184…` `186…` | 発信者番号通知プレフィックス | ❌ |
| `0312345678,,,123` | DTMF ポーズ付き | ❌ |

### ルールモデル

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
現状はプレフィックスの変更に `Presets.kt` の編集が必要（1.0 で UI から設定可能にする）。

### 緊急通報のハードガード

`ProtectedNumbers` が、ユーザー設定より**上位の安全層**として動く。ここで保護された番号は
どんなルール設定でも書き換えられない。

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

## ビルド

Android Studio（Koala 以降推奨）で `PrefixDialer/` を開くだけ。あるいは JDK 17 + Android SDK
(platform-35 / build-tools 35.0.0) があれば同梱の Gradle Wrapper で CLI ビルドできる:

```
./gradlew test          # ユニットテスト 52件（判定 19 / 緊急通報の不変条件 13 / ルール 15 / プリセット 5）
./gradlew assembleDebug # APK -> app/build/outputs/apk/debug/app-debug.apk
```

SDK の場所は `local.properties`（`sdk.dir=...`、リポジトリには含めない）か環境変数
`ANDROID_HOME` で指定する。

## 端末での有効化（順番どおりに）

アプリを起動し、画面のボタンで:

1. **通話リダイレクトを有効化** — `ROLE_CALL_REDIRECTION` を取得（端末に1アプリのみ）
2. **権限を許可** — 通話履歴の読み書き・連絡先・通知
3. **バッテリー最適化を解除** — One UI が履歴書き換えサービスを殺さないため（重要）

3項目すべて ✅ になれば準備完了。

## 動作確認

1. 標準の電話アプリで自分の携帯などへ発信 → 実際には `0063` 付きで発信される
2. 通話履歴を開く → 数秒以内に元番号へ戻り、連絡先名が表示されることを確認

`adb logcat -s PrefixRedirection CallLogRewrite` で書き換えの流れを追える。

## Samsung / One UI の既知の注意点

- **バッテリー最適化の解除は必須級。** 未設定だと発信後の履歴書き換えサービスが即座に殺され、
  履歴が `0063…` のまま残ることがある。設定 → アプリ → 本アプリ → バッテリー →「制限なし」も併用推奨。
- **Samsung Cloud の通話履歴同期が ON** だと、書き換えがクラウド側の値で巻き戻ることがある。
  検証時は一旦 OFF にして切り分ける。
- **純正ダイヤラーの表示キャッシュ**により、履歴一覧の表示が一瞬 `0063…` に見えてから元番号へ更新される
  ことがある。`CACHED_NAME` も併せて更新して表示ズレを抑えている。
- 緊急番号は `CallRedirectionService` 側でも OS が保護するが、`ProtectedNumbers` で独立に弾いている
  （OS の保護に依存しない）。

## 制約

- `WRITE_CALL_LOG` は Google Play ではセンシティブ権限。自分用・sideload 前提なら問題なし。
  Play 公開時は審査が厳しい。
- 通話リダイレクトアプリは端末に1つだけ。他の番号書き換え／着信拒否系アプリとは排他。

## ファイル構成

```
app/src/main/java/io.github.tmlksu.prefixdialer/
  PhoneNumberPrefixer.kt      判定の入り口（有効なルールセットを解決して委譲）
  DialRule.kt                 ルールのデータモデル（種別×prefix×先頭0の扱い）
  RuleEngine.kt               ルール評価器（Android非依存・テスト対象）
  Presets.kt                  事業者プリセット（裏取りできたものだけ収録）
  ProtectedNumbers.kt         緊急通報・特番のハードガード（設定より上位の安全層）
  PrefixRedirectionService.kt 発信直前に番号を書き換える
  CallLogRewriteService.kt    発信後に履歴を元番号へ戻す（短命FGS）
  PendingRewrites.kt          発信番号→元番号 の一時対応表
  MainActivity.kt             ロール/権限/バッテリーのセットアップ画面
app/src/test/java/io.github.tmlksu.prefixdialer/
  PhoneNumberPrefixerTest.kt  buildDialNumber の網羅テスト
```
