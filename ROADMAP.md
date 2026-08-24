# Roadmap

公開アプリ化を見据えた作業メモ。現状は「自分の S25 で動く MVP」段階。

## 現状 (2026-08-24)

- ✅ 発信時に国内の携帯/固定/IP電話へ自動で `0063` プレフィックスを付与（`CallRedirectionService`）
- ✅ 発信後に `CallLog` を元番号へ書き戻し、電話帳マッチを維持（短命フォアグラウンドサービス）
- ✅ 除外条件（`0120`/`0800`/`0570`/`0990`/`+81`以外/`010`/二重付与）を実装・ユニットテスト18件
- ✅ Samsung Galaxy S25 / One UI 7 / Android 15 実機で発信〜履歴書き換えまで動作確認
- MVP の applicationId は `com.example.prefixdialer` のまま（公開前に要変更）

## 公開までにやること

### 必須（ブロッカー）
- [ ] **applicationId のリネーム** — `com.example.*` は Play Console で拒否される。所有ドメイン等に基づく ID へ（例 `net.<yourdomain>.prefixdialer`）。パッケージ名も合わせて変更
- [ ] **署名設定** — release 用 keystore を作成し `signingConfigs` を設定。keystore はリポジトリに含めない（`.gitignore` 済みの想定で `keystore.properties` 方式に）
- [ ] **`WRITE_CALL_LOG` / `READ_CALL_LOG` のポリシー対応** — Play の機微な権限。Call Log 権限は原則「デフォルトの電話/SMSアプリ」向け。本アプリは既定ダイヤラーではないため、
      - 例外申請フォームでの用途説明が必要、または
      - 履歴書き換え機能をオプト イン/別建てにする、あるいは
      - 既定ダイヤラー化（`InCallService` 実装）まで踏み込む、のいずれかを検討
- [ ] **プライバシーポリシー** — 通話履歴・連絡先へアクセスするため必須。URL を Play Console に登録
- [ ] **ターゲット/コンプライアンス** — targetSdk を公開要件に合わせて維持（現状 35）

### 品質・UX
- [ ] アプリアイコン（現状は端末標準アイコンを流用）
- [ ] プレフィックス値を UI から設定可能に（現状 `PhoneNumberPrefixer.PREFIX` のハードコード）
- [ ] SIM/回線ごとの ON/OFF 切り替え（現状は両 SIM 一律）
- [ ] 除外/対象番号のカスタムルール UI
- [ ] 履歴書き換えの信頼性向上（One UI のダイヤラー表示キャッシュ・Samsung Cloud 同期との整合）
- [ ] 多言語対応（現状 日本語のみ）

### テスト
- [ ] `PhoneNumberPrefixer` のケース追加（境界・異常系）
- [ ] Instrumented テスト（`CallRedirectionService` / `CallLogRewriteService` の結合）
- [ ] 複数端末での実機確認（Samsung 以外の One UI 非搭載機）

## 判断が必要な論点

- **公開形態**: 既定ダイヤラーを置き換えない現方式のまま Play 申請するか、既定ダイヤラー化まで踏み込むか。
  Call Log 権限のポリシー通過難易度に直結する。
- **配布**: Play 公開 / GitHub Releases での APK 配布 / 内部共有、のどれを主とするか。
- **ライセンス**: 現状 MIT（`LICENSE`）。公開前に最終確認。
