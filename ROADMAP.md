# Roadmap

公開アプリ化に向けた作業メモ。判断待ちは [DECISIONS.md](DECISIONS.md) を参照。

## 現状

**1.0.0 を GitHub Releases で公開済み（2026-08-26）。**
https://github.com/tmlksu/prefix-dialer/releases/tag/v1.0.0

- ✅ 設定に従って発信番号を書き換え（`CallRedirectionService`）
- ✅ 緊急通報・特番のハードガード（`ProtectedNumbers`）— ユーザー設定より上位の安全層
- ✅ ルールモデル（番号種別 × プレフィックス × 先頭0の扱い）とプリセット
- ✅ 設定の永続化・エクスポート/インポート（依存を足さない手書き JSON）
- ✅ Compose の設定 UI（ルール編集・除外番号・発信記録・詳細設定）
- ✅ ロール喪失の検知と再取得導線
- ✅ 履歴書き換えのオプトイン化（既定 OFF、有効化時に初めて権限要求）
- ✅ ローミング中の自動停止、回線ごとの ON/OFF、個別除外リスト
- ✅ 発信記録（書き換えなかった理由つき）
- ✅ アイコン、多言語（en / ja）、applicationId、署名設定、R8、CI
- ✅ ユニットテスト 118 件（実発信を伴うテストは無し）

**1.1.0（未リリース）で Play 提出の前提を揃えた。** 変更点は [CHANGELOG.md](CHANGELOG.md)。

- ✅ targetSdk / compileSdk 36（AGP 8.9.3 / Gradle 8.11.1）
- ✅ 発信記録を Android の自動バックアップから除外
- ✅ アプリ内のプライバシーポリシー導線（Play の明文要件）
- ✅ `versionCode = 2` / `versionName = "1.1.0"`
- ⬜ targetSdk 36 のビルドでの実発信確認（DECISIONS.md H-08。**作者にしかできない**）

## 残っていること

### 実機確認（人手が必要）

[DECISIONS.md](DECISIONS.md) の 🔵 セクションにチェックリストがある。
コードは書けても、この確認が済むまで 1.0 は出せない。

- [x] 通常の発信でプレフィックスが付く（2026-08-26 確認済み）
- [ ] 設定変更が次の発信に反映される
- [ ] 履歴書き換えを有効にしたときの動作（One UI のバッテリー最適化との兼ね合い）
- [ ] ロールを他アプリに奪われたときの警告表示
- [x] **R8 で難読化した release APK の動作**（2026-08-26 確認済み）

### リリース作業（ブロッカー）

- [x] リリース用 keystore の作成（[DECISIONS.md](DECISIONS.md) D-01）
- [x] プライバシーポリシーの本文と公開先 URL（D-03）
- [x] applicationId の確定（D-10）— `io.github.tmlksu.prefixdialer`
- [x] GitHub Releases での配布
- [ ] Play への提出（D-03 / D-20）— 要件と手順は [PLAY-RELEASE.md](PLAY-RELEASE.md)、掲載文の草案は [PLAY-LISTING.md](PLAY-LISTING.md)。コード側の前提（targetSdk 36 / アプリ内のポリシー導線 / `versionCode = 2`）は 1.1.0 で揃った。残りは Console 側の作業

### プリセットの拡充

- [ ] G-Call 以外の事業者（D-02）。**実際の契約と発信明細での確認が要る。推測で追加しない**

## テスト方針

**実発信を伴う自動テストは行わない。**

- 判定ロジック（`RuleEngine` / `ProtectedNumbers` / `Settings` / JSON）は Android 非依存の
  純粋関数として切り出し、ローカルユニットテストで網羅する。テスト中に発信は一切発生しない
- `ProtectedNumbersTest` は**恒久的な安全網**。ルール追加・プレフィックス変更のたびに走り、
  1 件でも特番が書き換わったら失敗する。ここが赤くなったら期待値ではなく実装を直す
- 3 桁番号は `000`〜`999` を総当たりで検証（1XY 帯の取りこぼし防止）
- 意図的に凶悪なルールセットでも安全層が貫通しないことをテストで固定している
- **実機での手動確認に緊急通報番号を使わない** — `110` / `118` / `119` / `112` および
  `#7119` / `#8000` / `#9110` / `#8103` には絶対に発信しない。
  実害なく確認できるのは `117`（時報）程度で、これも必要最小限に留める
- CI（GitHub Actions）で `testDebugUnitTest` + `lintDebug` + debug/release ビルドを回す

## パーキングロット（1.0 リリース前に再検討）

- 発信前の確認ダイアログ（`onPlaceCall` の `allowInteractiveResponse` を利用）
- 国際発信のプレフィックス対応（現状 `010` は除外）
- DTMF ポーズ付き番号への対応（現状は保護＝素通し）
- `RuleCondition.StartsWith` を UI から編集可能にする（モデルは対応済み、UI は種別のみ）
- 履歴書き換えの信頼性向上（One UI の表示キャッシュ・Samsung Cloud 同期との整合）
- Samsung 以外の端末での実機確認
- Google アカウント同期による設定バックアップ
  （JSON エクスポート/インポートで実用は満たせるため 1.0 後の課題）
