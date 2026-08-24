# Prefix Dialer

発信時に、国内の携帯・固定電話番号へ自動で `0063` プレフィックスを付ける Android アプリ。
標準の電話アプリを置き換えず、`CallRedirectionService`（Android 10+）で発信直前に番号を書き換える。
通話履歴には元番号が残るよう、発信後に `CallLog` を書き戻す（電話帳マッチを維持）。

対象端末: Samsung Galaxy S25 / One UI 7（Android 15）を想定。minSdk 29 / targetSdk 35。

## ステータス

- **MVP 動作中** — S25 実機で発信〜履歴書き換えまで確認済み（2026-08-24）
- 公開アプリ化を見据えて開発中。残タスクは [ROADMAP.md](ROADMAP.md) 参照
  （applicationId のリネーム、署名設定、Call Log 権限の Play ポリシー対応、プライバシーポリシー等）
- 現状の `applicationId` は `com.example.prefixdialer`（公開前に要変更）

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
| `110` `119` | 無効/短縮 | ❌ |

プレフィックスを変えたい場合は `PhoneNumberPrefixer.PREFIX` を変更。

## ビルド

Android Studio（Koala 以降推奨）で `PrefixDialer/` を開くだけ。あるいは JDK 17 + Android SDK
(platform-35 / build-tools 35.0.0) があれば同梱の Gradle Wrapper で CLI ビルドできる:

```
./gradlew test          # ユニットテスト（buildDialNumber の網羅・18件）
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
- 緊急番号は `CallRedirectionService` 側でも OS が保護するが、`buildDialNumber` でも弾いている。

## 制約

- `WRITE_CALL_LOG` は Google Play ではセンシティブ権限。自分用・sideload 前提なら問題なし。
  Play 公開時は審査が厳しい。
- 通話リダイレクトアプリは端末に1つだけ。他の番号書き換え／着信拒否系アプリとは排他。

## ファイル構成

```
app/src/main/java/com/example/prefixdialer/
  PhoneNumberPrefixer.kt      判定ロジック（Android非依存・テスト対象）
  PrefixRedirectionService.kt 発信直前に番号を書き換える
  CallLogRewriteService.kt    発信後に履歴を元番号へ戻す（短命FGS）
  PendingRewrites.kt          発信番号→元番号 の一時対応表
  MainActivity.kt             ロール/権限/バッテリーのセットアップ画面
app/src/test/java/com/example/prefixdialer/
  PhoneNumberPrefixerTest.kt  buildDialNumber の網羅テスト
```
