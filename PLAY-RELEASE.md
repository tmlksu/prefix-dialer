# Google Play 公開に必要なこと

Prefix Dialer 1.0.0（GitHub Releases で公開済み）を Google Play にも出すための調査。
DECISIONS.md の D-03 / D-20 を実行段階に落としたもの。

調査日: **2026-09-18**。Play の要件は頻繁に変わるため、実際に提出する時点で各出典を読み直すこと。
各項目に出典 URL を付けた。**出典を確認できなかったものは「未確認」と明記している。**
Console に貼る掲載文・Data safety・審査手順・IARC の草案と、play 版ビルドの検証結果は `PLAY-LISTING.md` にある。

---

## ⛔ まず結論

**今日の時点では、コードを変えないと提出できない。**

2026-08-31 から、Google Play に新規で提出するアプリは **targetSdk 36（Android 16）以上**が必須になった。
本アプリは targetSdk 35。期限は既に 18 日過ぎている。

> 「2026 年 8 月 31 日以降: 新しいアプリとアプリのアップデートを Google Play に送信する場合は、
> Android 16（API レベル 36）以降を対象にする必要があります」
> — https://support.google.com/googleplay/android-developer/answer/11926878?hl=ja

延長申請（2026-11-01 まで）は**既存アプリのアップデート向けの救済措置**であり、
まだ 1 本も出していない新規アプリがこれで提出可能になるとは読めない。
→ 素直に 36 へ上げるのが唯一の道。詳細は §1。

もうひとつ、時間の制約がある。**個人アカウントは「12 人 × 連続 14 日間のクローズドテスト」を
終えないと製品版に出せない**（§2）。つまり今日アカウントを作っても、最短で公開は 1 か月以上先になる。

---

## チェックリスト

### 必須（これが無いと出せない）

#### 🔵 あなた（人間）にしかできない

- [ ] Google Play デベロッパー アカウントを作成（登録料 **US$25**・1 回限り）
      ※ 既にアカウントをお持ちなら、以下の本人確認とクローズドテスト要件は
      アカウント作成日が 2023-11-13 より前なら不要な可能性がある（§2 で分岐を説明）
- [ ] 本人確認（氏名・正式な住所・電話番号・身分証）を完了させる
- [ ] **アプリ登録時に「既存のアプリ署名鍵をアップロード」を選ぶ**（D-20）。
      既定のまま進むと Google 生成鍵になり、GitHub 版と上書き更新できなくなる。
      **製品版またはオープンテストにリリースを公開した時点で変更不可**（§3）
- [ ] `release.keystore` を PEPK ツールで暗号化してアップロード
- [ ] クローズドテストのテスターを **12 人**集め、**連続 14 日間**オプトインさせる
- [ ] 本番環境へのアクセスを申請（審査は通常 7 日以内）
- [ ] ストア掲載物を用意（512×512 アイコン / 1024×500 フィーチャーグラフィック /
      スクリーンショット 2 枚以上 / 短い説明 80 字 / 詳細 4000 字、ja + en）
- [ ] コンテンツ レーティング（IARC）質問票に回答
- [ ] Data safety フォームに回答（「データを収集しない」で回答できる。根拠は §5）
- [ ] プライバシー ポリシーの公開 URL を Console に登録
- [ ] 「アプリのアクセス」に審査担当者向けの手順を書く（ROLE 付与が要るため。§6）
- [ ] 対象年齢・広告なし・ニュース／政府／金融／ヘルスの各申告

#### 💻 コード側でやる作業

- [ ] **targetSdk / compileSdk を 36 へ**（必須。これが無いと受け付けられない）
- [ ] それに伴い **AGP 8.5.2 → 8.9.1 以上**、**Gradle 8.9 → 8.11.1 以上**（§1）
- [ ] Android 16 の挙動変更への追随を実機で確認（edge-to-edge 強制、予測バック、
      sw600dp 以上での向き固定無視。§1 に本アプリへの影響の具体的な評価）
- [ ] **アプリ内にプライバシー ポリシーへのリンクを置く**
      — Play は「Console のフィールド」と「アプリ内」の**両方**を要求している。
      現状アプリ内に導線が一切ない（`grep -i privacy app/src` がゼロ件）。§5
- [ ] PRIVACY.md に**デベロッパー名を明記**する。ストア掲載のエンティティ名が
      ポリシー本文に出ていることが要件（§5）
- [ ] `versionCode` を 2 以上に上げる（1 は GitHub 版 1.0.0 で使用済み。§8）

### 推奨

- [ ] **`android:allowBackup` の扱いを決める**。現状 `true` のため、発信記録
      （＝発信した電話番号そのもの）と設定が Android の自動バックアップで
      ユーザーの Google ドライブへ送られる。PRIVACY.md の
      「端末の外へ出ることはありません」と整合しない。§4 / §5
- [ ] ストア説明に「Play 版には通話履歴の書き換えが無い」旨を明記（§9）
- [ ] 審査で読まれる前提で、正当化の文章（緊急通報の保護・明示的な有効化・
      発信記録による可視化）を用意しておく（§4）
- [ ] 別途**アップロード鍵**を作り、`release.keystore` は PEPK の 1 回きりで
      手元に封印する（§3）
- [ ] Pre-launch report で ROLE 未付与のまま落ちないことを確認（§7）
- [ ] タブレット／Chromebook 用スクリーンショット（4 枚以上）

### 任意

- [ ] EU/EEA への配信をやめて DSA トレーダー申告そのものを回避する（§2、未確認あり）
- [ ] Gradle Play Publisher / fastlane supply で CI からアップロード（§8）
- [ ] Android developer verification（Play 外配布にも効いてくる別制度）への事前登録（§2）
- [ ] 発信せずに書き換え結果を確かめる「お試し入力欄」の追加（§6 / §7 の両方で効く）

---

## 1. targetSdk 要件

### 現状

`app/build.gradle.kts`:

```
compileSdk = 35
targetSdk = 35
```

ツールチェーンは AGP 8.5.2 / Gradle 8.9 / Kotlin 1.9.24 / Compose Compiler 1.5.14 /
Compose BOM 2024.06.00。

### Play の要件

| 時期 | 新規アプリ・アップデート | 既存アプリが新規ユーザーに配信され続ける条件 |
|---|---|---|
| 2025-08-31〜 | API 35 以上 | — |
| **2026-08-31〜** | **API 36 以上** | API 35 以上 |

- 出典: https://developer.android.com/google/play/requirements/target-sdk
- 出典（日本語）: https://support.google.com/googleplay/android-developer/answer/11926878?hl=ja

延長について、原文は「もっと時間が必要な場合は **2026-11-01 までの延長を申請できるようになる**」と
書いており、対象は文脈上 "update your app"。**新規アプリの初回提出に使えるかは未確認。**
いずれにせよ 36 に上げる作業自体は避けられないので、延長の可否を調べる時間で上げたほうが早い。

### ギャップとやること

**(a) ツールチェーンの引き上げ**

compileSdk 36 に必要な最低バージョンは AGP **8.9.1**。AGP 8.9 系は Gradle **8.11.1** 以上を要求する。

- 出典: https://developer.android.com/build/releases/about-agp
  （「Minimum versions of tools for Android API level」の表で API 36 → AGP 8.9.1）

現状 AGP 8.5.2 / Gradle 8.9 なので、両方とも足りない。変更案:

```
// build.gradle.kts
id("com.android.application") version "8.9.1" apply false   // 8.5.2 から

// gradle/wrapper/gradle-wrapper.properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip
```

**Kotlin と Compose は上げなくても通る見込み。** Compose Compiler のバージョンは
compileSdk ではなく Kotlin のバージョンに紐づくため（1.5.14 ↔ Kotlin 1.9.24 の対応は現状のまま正しい）、
compileSdk を 36 にしても対応表は変わらない。
- 対応表: https://developer.android.com/jetpack/androidx/releases/compose-kotlin

ただし **AGP 8.9 が Kotlin Gradle Plugin 1.9.24 を受け付けるかは未確認**（AGP は下限 KGP を持つ）。
ビルドが警告や失敗で止まる場合は Kotlin 2.0 系へ上げることになり、そのときは

- `composeOptions { kotlinCompilerExtensionVersion = ... }` を**削除**し、
- `org.jetbrains.kotlin.plugin.compose`（Kotlin と同一バージョン）を適用する

という別作業が付いてくる。Kotlin 2.0 以降は Compose Compiler が Kotlin 本体に統合されたため。
- 出典: https://developer.android.com/develop/ui/compose/compiler
- 出典: https://android-developers.googleblog.com/2024/04/jetpack-compose-compiler-moving-to-kotlin-repository.html

**この順序を勧める**: まず AGP/Gradle だけ上げて `compileSdk = 36` でビルドが通るか試す。
通らなければ Kotlin 2.x + Compose Compiler プラグイン + Compose BOM 更新をひとまとめの
別コミットにする（1 スライス 1 コミットの原則どおり、`git revert` で戻せる形に）。

**(b) targetSdk 36 で効いてくる Android 16 の挙動変更**

出典: https://developer.android.com/about/versions/16/behavior-changes-16

コードを読んだうえでの本アプリへの影響評価:

| 挙動変更 | 本アプリへの影響 | 対応 |
|---|---|---|
| **Edge-to-edge opt-out going away**（`windowOptOutEdgeToEdgeEnforcement` が無効化） | **影響小**。`MainActivity.onCreate` が既に `enableEdgeToEdge()` を呼んでおり、`Scaffold` の `innerPadding` を各画面に渡している。ただし Compose BOM 2024.06.00（Material3 1.2.1）は古く、`AdvancedScreen` 等は縦スクロール列に `horizontal` パディングしか当てていない | **実機で下端の表示確認が要る**。スクロール末尾がナビゲーションバーに潜る場合は `navigationBarsPadding()` か `contentWindowInsets` の明示を足す |
| **Migration or opt-out required for predictive back**（`onBackPressed` が呼ばれなくなる） | **影響なしの見込み**。戻る操作は Compose の `BackHandler`（＝`OnBackInvokedCallback` 経路）で実装しており、`onBackPressed` のオーバーライドは無い | 確認のみ。`android:enableOnBackInvokedCallback="false"` は**付けない** |
| **Adaptive layouts**（sw 600dp 以上で向き・リサイズ・アスペクト比の制限を無視） | **影響なし**。マニフェストに `screenOrientation` も `resizeableActivity` も宣言していない | 対応不要 |
| **Fixed rate work scheduling optimization**（`scheduleAtFixedRate`） | **影響なし**。使っていない | — |
| **Local Network Permission**（ローカルネットワークアクセスに `NEARBY_WIFI_DEVICES` が必要） | **影響なし**。INTERNET 権限すら持たない | — |
| **Elegant font APIs deprecated** | 影響なし（日本語・英語のみ） | — |
| **Safer Intents**（`intentMatchingFlags`） | 影響なし。オプトイン | — |
| **Health and fitness permissions**（`BODY_SENSORS` → `android.permissions.health`） | 影響なし | — |
| **MediaStore version lockdown** | 影響なし | — |

**Telecom / CallRedirectionService / READ_PHONE_STATE について: targeting 挙動変更の一覧に該当項目は無い。**
API 35→36 の `android.telecom` 差分ページ（`/sdk/api_diff/36/changes/pkg_android.telecom`）は
404 で取得できなかったため、**「変更なし」と断定はできない。未確認。**
ロールと発信経路はアプリの中核なので、36 に上げたビルドで **実機の発信確認（H-01 相当）を必ずやり直すこと。**

**(c) 16KB ページサイズ**

targetSdk 35 以上のアプリは 16KB メモリページ対応が必要、2027-02-01 以降は非対応だと更新不可。
ただし:

> 「If your app only uses code written in the Java programming language or in Kotlin,
> including all libraries or SDKs, then your app already supports 16 KB devices.」
> — https://developer.android.com/guide/practices/page-sizes

**実測で確認済み。ネイティブライブラリはゼロ:**

```
$ unzip -l app/build/outputs/bundle/playRelease/app-play-release.aab | grep '\.so$'
（0 件）
$ unzip -l app/build/outputs/apk/github/release/app-github-release.apk | grep '\.so$'
（0 件）
```

libphonenumber は純 Java。→ **対応不要。**

---

## 2. 開発者アカウント（個人）

### 登録料と本人確認

- 登録料: **US$25**（1 回限り、年額ではない）
  - https://support.google.com/googleplay/android-developer/answer/6112435
- 個人アカウントに必要な情報: デベロッパー名、氏名、**正式な住所**、連絡先メール、連絡先電話番号、
  デベロッパー プロフィールのメールアドレス、および身分証による本人確認
  - https://support.google.com/googleplay/android-developer/answer/13628312
- **D-U-N-S 番号は個人アカウントには不要**（組織アカウントのみ必須）
  - 同上

### クローズドテスト要件（2023-11-13 以降に作った個人アカウント）

> 「12 人以上のテスターが少なくとも 14 日間連続でオプトインした状態でクローズド テストを
> 実施する必要があります」
> — https://support.google.com/googleplay/android-developer/answer/14151465?hl=ja

- 14 日間は**連続**であること。途中でオプトアウトするとその人は数に入らず、
  入り直すと 14 日のカウントがやり直しになる
- 条件を満たしたら Play Console のダッシュボードから「本番環境へのアクセスを申請」
- **審査は通常 7 日以内**（それ以上かかることもある）
  - 同上
- （2023-11 当初は 20 人だったが 2024-12 に 12 人へ緩和された。現行値は 12 人）

**既にアカウントをお持ちの場合:** アカウントの作成日が 2023-11-13 より前の個人アカウントなら
この要件の対象外という読み方になるが、**既存アカウントの扱いは未確認**。
Play Console のダッシュボードに「本番環境へのアクセスを申請」という導線が出るかどうかで判別できる。

### 審査にかかる日数

新規アプリの審査は**通常 7 日以内**。新しいデベロッパー アカウントではより慎重に審査され、
7 日を超えることもある。**審査中にストア情報を変更すると審査がやり直しになる**点に注意。
- https://support.google.com/googleplay/android-developer/answer/9859751

### 公開される情報と最小化策

| 項目 | 公開されるか | 備考 |
|---|---|---|
| デベロッパー名 | **公開** | ストア掲載の署名欄。本名である必要はなく、任意の表示名を設定できる |
| 国（正式な住所に基づく） | **公開** | 住所そのものではなく国名 |
| デベロッパーのメールアドレス | **公開** | ユーザーからの問い合わせ先。**専用のアドレスを新規に作ること** |
| 正式な住所（詳細） | **無料アプリなら非公開** | 「Google Play で収益を得る場合は、詳細な住所が公開されることになります」 |
| 電話番号 | ユーザー連絡先欄に入れた場合のみ公開 | **入れない** |

- https://support.google.com/googleplay/android-developer/answer/13634081?hl=ja
- https://support.google.com/googleplay/android-developer/answer/13628312

**最小化策（本アプリは無料・アプリ内購入なしなので全部使える）:**

1. **有料化・アプリ内購入・広告を一切入れない。** これが住所非公開の条件。
   収益化した瞬間に詳細住所が公開される。後戻りできない性質の情報なので、
   Play で課金することは当面考えないと決めておいたほうがよい
2. デベロッパー名は本名にしない（例: `Tmlk` / `Prefix Dialer Project`）
3. デベロッパーのメールアドレスは**この用途専用**に新規作成する。
   GitHub の noreply とは別に必要（D-19 で GitHub 側のメール露出は既に潰してある）
4. ユーザー連絡先の電話番号は入力しない
5. 住所は**本人確認と請求先プロフィールのために Google には渡る**。これは避けられない

**日本の特定商取引法の表示義務は、無料アプリには掛からない。**

> 「日本の消費者に対して有料アプリを販売する場合やアプリ内購入を提供する場合は、
> 特定商取引法の規定に基づき、所定の情報を消費者に表示する必要があります。
> これには、事業者の名前、電話番号、住所が含まれます。」
> — https://support.google.com/googleplay/android-developer/answer/6223646?hl=ja

つまり有料化すると**日本国内向けにも氏名・電話番号・住所の表示義務が発生する**。
無料を貫く理由がもう 1 つ増えた。

### EU DSA のトレーダー申告

**未確認。** Play Console に「トレーダー / 非トレーダー」の申告欄が存在することは
実務上よく知られているが、今回 developer.android.com と
support.google.com/googleplay/android-developer を検索した範囲では、
**この申告を説明する公式ヘルプ記事を特定できなかった。**
確認した以下のページには記載が無い:

- https://support.google.com/googleplay/android-developer/answer/14659200 （EEA の一般的なアクセス条件）
- https://support.google.com/googleplay/android-developer/answer/6223646 （国・地域別の要件。EU の項はジオブロッキング禁止のみ）

**判断:** Console にアカウントを作れば申告欄の有無はその場で分かる。事前に悩む価値は薄い。
仮に申告が求められた場合:

- **無料・非収益・趣味のアプリであれば「非トレーダー」で申告できるのが自然な読み**だが、
  これも未確認。法的判断なので断定しない
- どうしても個人情報の公開を避けたいなら、**国・地域の設定から EU/EEA を外す**のが確実。
  本アプリは日本の番号計画に強く依存しており（DECISIONS.md D-16 で多言語も ja/en に限定した）、
  **EU で配信する実益がほぼ無い。** 配信先を日本のみ、あるいは日本＋英語圏に絞るのは
  機能面の損失がなく、申告と住所公開のリスクを丸ごと消せる

### 別制度: Android developer verification（Play 外配布にも効く）

Play とは別に、**認定 Android 端末へのインストール全般**にデベロッパー本人確認を課す制度が動いている。
GitHub 配布にも将来効いてくるので記録しておく。

- **2026-09-30**: ブラジル・インドネシア・シンガポール・タイの「参加ストア」で施行開始
- **2026-08**: 未確認デベロッパーのアプリを入れるための「高度なフロー」が一般ユーザー向けに提供開始
- **2027**: グローバル展開の予定
- 現時点では「他のストアでの配布やユーザーによるサイドロードには、まだこの要件は適用されない」
- ADB 経由のインストールは検証不要

出典: https://developer.android.com/developer-verification/guides/faq

**本アプリへの含意:** 日本は初期対象に入っていないが、2027 のグローバル展開で
GitHub Releases の APK も影響を受けうる。Play アカウントを作れば同じ本人確認で
パッケージ名を登録できるので、**Play 提出はこの制度への備えも兼ねる。**

---

## 3. 配布形式と署名

### AAB 必須

2021 年 8 月以降、**新規アプリは Android App Bundle（.aab）での公開が必須**。APK は受け付けられない。
- https://developer.android.com/guide/app-bundle
- https://android-developers.googleblog.com/2021/06/the-future-of-android-app-bundles-is.html

本アプリは `./gradlew bundlePlayRelease` で
`app/build/outputs/bundle/playRelease/app-play-release.aab` が出る。**既に用意できている**（4.3MB）。

### Play App Signing に既存の鍵をアップロードする（D-20 の実行手順）

**新規アプリの既定は Google 生成鍵**になった。原文:

> 「アプリは、Google 生成の鍵による量子対応のハイブリッド署名に自動的に登録されます」
> — https://support.google.com/googleplay/android-developer/answer/9842756?hl=ja

**変更できる期限（重要・更新）:**

> 「you can change this default before there is a release rolled out in open testing track or production track」
> — https://support.google.com/googleplay/android-developer/answer/9842756?hl=en

つまり **「アプリの初回登録時にしか選べない」よりは少し猶予がある** — オープンテストまたは
製品版トラックにリリースを公開するまでは変更できる、と読める。
DECISIONS.md D-20 の記述はやや厳しめの理解だった。ただし個人アカウントのクローズドテストは
**14 日間**続くので、その前に確定させないと「オープンテスト／製品版に出す前」という条件を
うっかり踏み越えるリスクがある。**アプリ作成直後に片付けるのが安全。**

なお **内部テスト / クローズドテストに出した段階でも変更可能かは未確認**（原文は
"open testing track or production track" としか書いていない）。
実務上は「最初のリリースを何かのトラックに出す前に済ませる」と覚えておけば間違いない。

**手順（Play Console）:**

1. Play Console でアプリを作成する
2. 「テストとリリース」→「アプリの完全性」→「アプリ署名」へ進む
3. 既定の「Google 生成鍵」ではなく **「Java キーストアからアプリ署名鍵をエクスポートしてアップロード」**
   に相当する選択肢を選ぶ
4. 表示される **PEPK ツール（`pepk.jar`）** をダウンロードする
5. `release.keystore` から暗号化済みの鍵をエクスポートし、生成されたファイルをアップロードする

**PEPK の正確なコマンドラインは未確認。**
developer.android.com / Play Console ヘルプの公開ページには完全な引数一覧が載っておらず、
**Console の画面上に、そのアプリ専用の暗号化鍵を埋め込んだコマンドが表示される方式**になっている。
画面に出るコマンドをそのままコピーして使うこと。おおむね次の形になる:

```
java -jar pepk.jar --keystore=release.keystore --alias=prefixdialer \
     --output=output.zip --include-cert \
     --rsa-aes-encryption --encryption-key-path=<Console が渡す公開鍵ファイル>
```

> ⚠️ 引数名を推測で使わない。Console の画面に出ているものを使うこと。

**鍵の要件:**

- 独自のアプリ署名鍵は **RSA 2048 ビット以上**（Google 生成のものは RSA 4096）
- `release.keystore` は RSA 4096 なので条件を満たす（D-01）
- キーストアは **Java キーストア形式**（`.jks` / `.keystore`）である必要がある。
  `release.keystore` は `keytool` で作成したものなので該当する
  （JDK 9 以降の既定は PKCS12 だが、`keytool` が出すものは Java キーストアとして扱われる）
- 出典: https://support.google.com/googleplay/android-developer/answer/9842756

### アップロード鍵を別に作るか

> 「セキュリティを最大限に高めるため、アップロード鍵とアプリ署名鍵は異なるものを使用する必要があります」
> — https://support.google.com/googleplay/android-developer/answer/9842756?hl=ja

**推奨: 別に作る。** 理由:

- アプリ署名鍵（＝`release.keystore`）は GitHub 版と Play 版の互換性の根拠そのもので、
  紛失も漏洩も取り返しがつかない。PEPK での 1 回のエクスポートを終えたら**金庫に封印し、
  日常のビルドでは触らない**のが望ましい
- アップロード鍵が漏れた場合は Play Console から差し替えられる。アプリ署名鍵は差し替えられない
- 将来 CI から自動アップロードする場合（§8）、CI に渡すのはアップロード鍵だけで済む

```
keytool -genkeypair -v -keystore upload.keystore -alias prefixdialer-upload \
        -keyalg RSA -keysize 4096 -validity 10000
```

ただし **GitHub 配布用の APK はこれまでどおり `release.keystore` で署名する**必要がある
（そちらは Play App Signing を経由しないため）。`keystore.properties` が 1 本しかない現在の
構成だと、鍵を 2 本扱うにはビルド設定の変更が要る。**変更案（今は実施しない）:**

- `keystore.properties` に `uploadStoreFile` / `uploadStorePassword` / `uploadKeyAlias` /
  `uploadKeyPassword` を追加し、`play` フレーバーの release だけ upload 鍵で署名する
- あるいは当面はアップロード鍵を作らず `release.keystore` を両方に使い、
  CI 自動化を始める段階で分離する（**シンプルで、1.0 の提出はこれで十分**）

**アップロード鍵を登録しなかった場合にアプリ署名鍵がそのままアップロード鍵として使われるかは未確認。**
現在の `bundlePlayRelease` は `release.keystore` で署名済みなので、そのまま上げて通る想定だが、
Console の表示を見て判断すること。

### mapping.txt（R8 難読化解除ファイル）

**アップロード不要。AAB に自動で入る。**

> 「If you're using an app bundle and Android Gradle plugin version 4.1 or later, there's nothing
> you need to do, as Google Play will automatically grab the deobfuscation file from the bundle.」
> — https://support.google.com/googleplay/android-developer/answer/9848633

**実測で確認済み:**

```
$ unzip -l app/build/outputs/bundle/playRelease/app-play-release.aab | grep obfuscation
 27082919  BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map
```

ネイティブコードが無いので、ネイティブ デバッグ シンボルも不要（§1-c）。

---

## 4. 権限とポリシー審査

### `play` フレーバーが宣言する権限

マニフェストを読んだ結果、`play` フレーバーで宣言されるのは
**`android.permission.READ_PHONE_STATE` の 1 つだけ**（`app/src/main/AndroidManifest.xml`）。
`app/src/github/AndroidManifest.xml` の CALL_LOG / CONTACTS / FOREGROUND_SERVICE / POST_NOTIFICATIONS は
`github` フレーバーにしか入らない。CI（`.github/workflows/build.yml`）が
`aapt2 dump permissions` で機械的に検証している。**この設計は正しい。**

> 本来ここでマージ済みマニフェストをダンプして再確認したかったが、この環境に
> `ANDROID_HOME` が設定されておらず `aapt2` を実行できなかった。CI では毎回検証されている。
→ PLAY-LISTING.md §1.2 で 2026-09-18 に実測済み（READ_PHONE_STATE のみ。INTERNET / CALL_LOG / READ_CONTACTS なし）。

### READ_PHONE_STATE は宣言フォームの対象か

Play の「機密情報にアクセスする権限と API」ポリシーの制限付き権限リストに
**`READ_PHONE_STATE` は明示されていない**（挙げられているのは通話履歴、SMS、位置情報、
ファイルアクセス、パッケージの可視性など）。
- https://support.google.com/googleplay/android-developer/answer/9888170

ただし一般原則は掛かる:

> 「You may only request permissions and APIs that access sensitive information that are necessary
> to implement current features or services in your app that are promoted in your Google Play listing.」

本アプリでの用途は「設定画面で回線（SIM）名を表示する」だけで、**判定には使っていない**
（`PhoneAccounts.kt` の設計。DECISIONS.md D-18）。しかも「回線ごとの設定」を開いたときにしか要求しない。
→ **必要最小限であり、ストア掲載で説明できる。** 宣言フォームは要らない見込みだが、
Console の「アプリのコンテンツ」に権限宣言フォームが出たら正直に埋めること。

### ROLE_CALL_REDIRECTION について

**Play Console に「通話リダイレクト ロール」専用の宣言フォームや動画提出の要件は見つからなかった。**
調べた範囲（SMS / 通話履歴の権限グループのポリシー、機密情報アクセスのポリシー、
アプリのコンテンツの宣言一覧）には ROLE_CALL_REDIRECTION の項目が無い。
`ROLE_CALL_REDIRECTION` はマニフェスト権限ではなく `RoleManager` 経由のロールで、
`BIND_CALL_REDIRECTION_SERVICE` はシステムのみが保持する署名権限だからだと考えられる。
- https://developer.android.com/develop/connectivity/telecom/dialer-app/redirect-a-call
- **未確認**: 提出時に Console 側で新しい申告が増えている可能性はある

### 「電話番号を書き換えるアプリ」のポリシーリスク評価

3 つのポリシーを読んだ。**明示的に禁じる条文は無い**が、審査担当者が引っ掛けうる箇所は特定できた。

**(a) Device and Network Abuse** — https://support.google.com/googleplay/android-developer/answer/9888379

該当しうるのは最も一般的な条項だけ:

> 「We don't allow apps that interfere with, disrupt, damage, or access in an unauthorized manner
> the user's device, other devices or computers, servers, networks, application programming
> interfaces (APIs), or services」

**テレフォニー・発信・番号の改変を名指しする条文は無い。**
本アプリは `CallRedirectionService` という**公式 API を、公式の用途どおりに**使っている。
ロールはユーザーがシステムのダイアログで明示的に付与する。
→ "unauthorized manner" には当たらない、と説明できる。

**(b) Deceptive Behavior** — https://support.google.com/googleplay/android-developer/answer/9888077

ここが**一番効く**。3 つの条項が直接刺さる:

> 「We don't allow apps that make changes to the user's device settings or features outside of the
> app without the user's knowledge and consent.」

> 「Apps must provide an accurate disclosure, description and images/video of their functionality
> in all parts of the metadata.」

> 「Your app's functionality should be reasonably clear to users; don't include any hidden, dormant,
> or undocumented features within your app.」

番号の書き換えは「アプリの外の挙動を変える」行為そのもの。
**ストア説明で何をするアプリかを一切ぼかさないこと**が実質的な合格条件になる。

**(c) SMS / 通話履歴の権限グループ** — §4 の次節。`play` フレーバーは該当しない。

### 審査で説明すべきポイント（そのまま使える文面の骨子）

ストアの詳細説明と、必要なら審査担当者向けメモに入れる:

1. **何をするアプリかを 1 行目で言い切る。**
   「発信する電話番号の先頭に、あなたが契約している中継電話事業者のアクセス番号
   （例: 0063）を自動で付けます。」
   — 婉曲に書くほど Deceptive Behavior の心証が悪くなる

2. **ユーザーの明示的な設定でしか動かない。**
   - Android のシステムダイアログで「通話リダイレクト」ロールをユーザーが承認しないと一切動作しない
   - どのプレフィックスを、どの番号種別に付けるかは、すべてユーザーが設定画面で決める
   - マスタースイッチで即座に全停止できる

3. **緊急通報は設定に関係なく保護される。**
   - 緊急通報番号・3 桁の特番・`#` / `*` を含む番号は、**どんなルール設定でも書き換えない**
   - これはユーザー設定より上位の安全層として実装されている（`ProtectedNumbers.kt`）
   - 3 桁番号 `000`〜`999` を総当たりで検証するユニットテストが CI で毎回走る
   - **意図的に凶悪なルールセットを与えても貫通しないことをテストで固定している**
   - フリーダイヤル `0120`/`0800`、ナビダイヤル `0570`、有料情報 `0990` も同様にルール上位で除外

4. **すべての発信が可視化される。**
   アプリ内の「発信記録」に、各発信で何をしたか、書き換えなかった場合はその理由が残る。
   隠し機能ではない（Deceptive Behavior の "hidden, dormant, or undocumented features" への直接の反論）

5. **課金事故を避ける設計。**
   ローミング中は自動停止、回線（SIM）ごとに ON/OFF、番号単位の除外リスト。
   「迷ったらプレフィックスを付けない」側に倒している

6. **ネットワークに接続できない。**
   INTERNET 権限を宣言していないため、技術的に送信が不可能。
   `aapt2 dump permissions` で誰でも検証できる

7. **ソースコードが公開されている。** https://github.com/tmlksu/prefix-dialer （MIT）

### CALL_LOG を外している判断の再確認 → **妥当。維持する**

現行のポリシー文面（日本語）:

> 「アプリが通話履歴や SMS に関する権限にアクセスするための要件を満たしていない場合は、
> 該当の権限を**アプリのマニフェストから削除する必要があります**。」

> 「アプリは、ユーザーに SMS や通話履歴に関する権限の許可を求める前に、
> デフォルトの SMS ハンドラ、電話ハンドラ、アシスタント ハンドラとして
> 能動的に登録されている必要があります。」

— https://support.google.com/googleplay/android-developer/answer/10208820?hl=ja

**「マニフェストから削除する必要がある」＝実行時に要求するかどうかとは独立に、宣言自体が問題になる**
という読みを公式文面が裏付けている。README と `build.gradle.kts` のコメントの理解は正しい。

例外として認められる用途の一覧（アカウント確認、フィッシング対策、バックアップ／復元、
スパム検出、コンパニオン デバイス、クロスデバイス同期、自動化、企業管理、車載ハンズフリー、
緊急 SMS、プロキシ通話、金融取引、デフォルトの電話アプリによる通話記録 など）にも
**「通話のリダイレクト」は無い。** さらに例外の条件は「その権限がコア機能を実現していること」だが、
本アプリは履歴書き換えを任意機能として設計している（＝コアではない）。

→ **`play` フレーバーから CALL_LOG 系を丸ごと外した判断は現行ポリシーでも正しい。**
CI の検証も維持すること。

### `android:allowBackup="true"` の扱い

現状 `app/src/main/AndroidManifest.xml` で `android:allowBackup="true"`（Android 6.0 以降の既定値）。

**何が起きるか:** `SettingsStore` と `CallRecordStore` はどちらも `SharedPreferences` を使う
（`SettingsStore.kt` / `CallRecordStore.kt`）。SharedPreferences は Android 自動バックアップの
既定の対象。つまり:

- ユーザーの設定（ルール、プレフィックス、除外番号）
- **発信記録 100 件 — `CallRecord.originalNumber` と `dialedNumber`、すなわち発信した電話番号そのもの**

が、ユーザーの Google ドライブへ送られる。

**これは PRIVACY.md の記述と整合していない。** PRIVACY.md は
「あなたの電話番号・通話履歴・連絡先が端末の外へ出ることはありません」
「発信記録は端末内にのみ保存されます」と書いている。
厳密には OS が運ぶのであってアプリが送るのではないが、**ユーザーから見れば番号が端末外に出ている。**

**選択肢:**

| 案 | 内容 | 評価 |
|---|---|---|
| **A** | `android:allowBackup="false"` | 最も単純で、PRIVACY.md の記述と完全に一致する。代償は機種変時に設定が引き継がれないこと。ただし**本アプリは JSON エクスポート／インポートを既に持っている**（`AdvancedScreen`）ので、実用上の損失は小さい |
| **B** | `allowBackup="true"` のまま `android:dataExtractionRules` で発信記録の SharedPreferences だけ除外し、設定は引き継ぐ | ユーザー体験は最良。実装は `xml/data_extraction_rules.xml` を足すだけ。ただし「設定」にも除外番号（＝実在の電話番号）が入りうる点は残る |
| C | 現状維持 + PRIVACY.md に自動バックアップの記述を追加 | 正直だが、「収集しない」という強い主張が弱まる |

**推奨は A。** DECISIONS.md の「迷ったらブロック側に倒す」「誤判定のコストが非対称」という
このプロジェクトの一貫した設計思想に最も素直に沿う。B は設定と発信記録を別 prefs ファイルに
分ける必要があり（現状は別ファイルなので実は可能）、選ぶなら B も現実的。

**Data safety フォームへの影響は §5 で扱う。**

---

## 5. Data safety フォームとプライバシー ポリシー

### 「データを収集しない」と回答できるか → **できる**

Play の「収集」の定義は明確で、**端末外への送信**を指す:

> 「"Collect" means transmitting data from your app off a user's device.」

> 「User data accessed by your app that is only processed locally on the user's device and
> not sent off device does **not** need to be disclosed.」

— https://support.google.com/googleplay/android-developer/answer/10787469

本アプリは **INTERNET 権限を宣言していない**ため、アプリからの送信は技術的に不可能。
発信番号の判定も、発信記録の保存も、すべて端末内で完結する。
→ **Data safety では「このアプリはユーザーデータを収集しません」と回答できる。**

**回答案:**

| 質問 | 回答 |
|---|---|
| アプリはユーザーデータを収集または共有しますか | **いいえ** |
| （以降のデータ種別の質問） | 回答不要になる |
| データは転送中に暗号化されますか | 該当なし（収集しないため） |
| ユーザーはデータの削除をリクエストできますか | 該当なし。ただし「アプリをアンインストールすれば全データが消える」旨は詳細説明に書ける |
| 独立したセキュリティ審査を受けましたか | いいえ |

**`allowBackup` との関係（判断が要る）:**
Android 自動バックアップで SharedPreferences がユーザーの Google ドライブに送られる件（§4）が
Data safety 上の「収集」に当たるかは、**公式ヘルプに明示の記述を見つけられなかった。未確認。**

- 「収集」の主語は "your app" であり、OS のバックアップはアプリによる送信ではない、という読み
- 一方で「番号が端末外に出る」ことは事実

**推奨: §4 の案 A（`allowBackup="false"`）を採れば、この曖昧さごと消える。**
「データを収集しない」という回答と PRIVACY.md の主張が、解釈の余地なく成り立つ。
安全側に倒すという本プロジェクトの原則そのまま。

### プライバシー ポリシーの要件

**無収集でも必須:**

> 「Apps that do not access any personal and sensitive user data must still submit a privacy policy.」

**掲載場所は 2 か所:**

> 「All apps must post a privacy policy link in the designated field within Play Console,
> **and a privacy policy link or text within the app itself**.」

**URL の要件:**

> 「Please make sure your privacy policy is available on an active, publicly accessible and
> non-geofenced URL (no PDFs) and is non-editable.」

**本文に必要な内容:**

- デベロッパー情報とプライバシーに関する問い合わせ窓口（または問い合わせの手段）
- アプリがアクセス・収集・使用・共有する個人データおよび機密データの種類、共有先
- 個人データ／機密データの安全な取り扱い手順
- デベロッパーのデータ保持・削除に関するポリシー
- プライバシー ポリシーであることが明確に分かる表記
- **「アプリの Google Play ストア掲載情報に記載されたエンティティ（デベロッパー、企業など）の名前が
  プライバシー ポリシーに記載されているか、アプリ名がプライバシー ポリシーに記載されていること」**

— https://support.google.com/googleplay/android-developer/answer/10144311

### PRIVACY.md の評価

**良い点:** 収集しないことの技術的根拠（INTERNET 権限なし・検証方法）、端末内で扱う情報の表、
権限ごとの要求タイミング、削除方法、日英併記。要求内容の大半を既に満たしている。

**不足（要修正）:**

| # | 不足 | 対応 |
|---|---|---|
| 1 | **アプリ内にプライバシー ポリシーへの導線が無い** | `grep -i privacy app/src` が 0 件。`AdvancedScreen` の末尾か新設の「このアプリについて」に、PRIVACY.md の公開 URL を開くリンクを追加する。**これは Play の明文要件。必須** |
| 2 | **デベロッパー名が本文に無い** | 「ストア掲載のエンティティ名がポリシーに記載されていること」が要件。冒頭に「デベロッパー: ◯◯（Google Play 上の表示名）」を追記する |
| 3 | 問い合わせ窓口が「GitHub Issues から」のみ | GitHub アカウントが無いユーザーは連絡できない。Play で公開するデベロッパー メールアドレスを併記する |
| 4 | データ保持・削除ポリシーが暗黙的 | 「発信記録は既定 100 件で古いものから自動削除」「アンインストールで全削除」は書かれているので、**「データの保持と削除」という見出しにまとめ直す**だけでよい |
| 5 | Play 版には通話履歴の書き換えが無いのに、権限表に CALL_LOG が並んでいる | Play 版のユーザーが読むと不正確。「配布版による違い」の節を足すか、フレーバーごとに URL を分ける |
| 6 | 自動バックアップ（§4） | `allowBackup="false"` にするなら記述不要。維持するなら明記が要る |

### 公開 URL をどれにするか

**候補と評価:**

| URL | 評価 |
|---|---|
| `https://github.com/tmlksu/prefix-dialer/blob/main/PRIVACY.md` | 有効・公開・ジオフェンスなし・PDF でない → 3 条件は満たす。**"non-editable" の解釈が問題。** リポジトリ所有者は編集できるが、これはどのポリシーページでも同じ（Google の意図は「閲覧者が編集できるページ」＝編集可能な Google ドキュメント等の排除）。実務では GitHub の URL は広く通っている。ただし GitHub の UI には "Edit this file" ボタンが出る点が形式上の弱み |
| `https://raw.githubusercontent.com/.../PRIVACY.md` | **使わない。** プレーンテキストで返り、ブラウザでの可読性が低い。「プライバシー ポリシーであることが明確に分かる」要件にも不利 |
| **GitHub Pages**（`https://tmlksu.github.io/prefix-dialer/privacy.html`） | **推奨。** 純粋な閲覧専用の HTML で、編集ボタンも出ない。`docs/` ディレクトリを追加して Pages を有効にするだけ。リポジトリは既に public |

**推奨: GitHub Pages に置き、blob URL は補助にする。** 形式要件でつつかれる余地を消せるうえ、
日英併記のページを整形して出せる。`PRIVACY.md` が原本であることは変わらない。

---

## 6. ストア掲載物

### 必須アセットの仕様

出典: https://support.google.com/googleplay/android-developer/answer/9866151

| アセット | 仕様 |
|---|---|
| **アプリアイコン** | **512 × 512 px**、**32 ビット PNG（アルファあり）**、最大 **1024 KB** |
| **フィーチャー グラフィック** | **1024 × 500 px**、**JPEG または 24 ビット PNG（アルファなし）** |
| **スマートフォンのスクリーンショット** | **最低 2 枚**、最大 8 枚。JPEG または 24 ビット PNG（アルファなし）。最小辺 320 px / 最大辺 3840 px。縦 **9:16**（最小 1080×1920）／横 **16:9**（最小 1920×1080） |
| **タブレット / Chromebook** | **最低 4 枚**。1,080〜7,680 px。16:9（横）／9:16（縦）。※必須かどうかは対象デバイスの設定による。**未確認**だが、用意すると大画面での表示品質評価に効く |

**アイコンについての注意:** リポジトリにあるのはアダプティブ アイコン（ベクター）で、
512×512 の PNG は存在しない。**Play Console 用に別途書き出しが必要。**
DECISIONS.md D-17 でアイコンを安全領域に収まるよう縮小した経緯があるが、
ストア用の 512×512 は**正方形いっぱいに描く**（アダプティブ アイコンのマスクは掛からない）。
同じ図柄をそのまま出すと余白が大きく見えるので、**ストア用は別に調整すること。**

### テキスト

出典: https://support.google.com/googleplay/android-developer/answer/9859152
（リリースノートは https://support.google.com/googleplay/android-developer/answer/9859348 ）

| 項目 | 上限 |
|---|---|
| アプリ名 | **30 文字** |
| 短い説明 | **80 文字** |
| 詳細な説明 | **4000 文字** |
| リリースノート | 言語ごとに **500 文字** |

**アプリ名案:** `Prefix Dialer`（13 文字）。日本語版も同じでよい。
日本語で意味を伝えたいなら `Prefix Dialer - 中継電話プレフィックス` のような形も 30 文字に収まる。

**短い説明（ja）案（80 字以内）:**

貼れる完成形の文面は PLAY-LISTING.md §3。

> 発信時に中継電話事業者のプレフィックスを自動で付与。緊急通報は設定に関わらず保護されます。（44 字）

**短い説明（en）案:**

> Adds your carrier access code to outgoing calls. Emergency numbers are never rewritten. （87 字 → 要短縮）
> → `Adds a carrier access code to outgoing calls. Emergency numbers are protected.` （78 字）

貼れる完成形の文面は PLAY-LISTING.md §3。

**詳細な説明に必ず入れるもの**（§4 の「審査で説明すべきポイント」と Deceptive Behavior 対策）:

- 何をするアプリか（書き換えることを 1 行目で明言）
- ロールの付与がユーザー操作で必要なこと
- 緊急通報・特番が保護されること
- 発信記録で全発信が確認できること
- **「この版には通話履歴の書き換え機能はありません」**（§9）
- 中継電話事業者との契約が別途必要なこと（アプリは割引を提供しない）
- 情報を収集・送信しないこと、ソースコードが公開されていること

### ローカライズ

**ja + en の 2 言語**（DECISIONS.md D-16 と同じ方針）。
既定言語を **英語**にし、日本語を追加するのがアプリ内リソースの構成
（`values/` が英語、`values-ja/` が日本語）と一致していて分かりやすい。
ただし**実質のユーザーは日本のみ**なので、日本語を既定にしても実害はない。どちらでもよい。

### カテゴリ

利用可能なカテゴリに **Tools（ツール）** と **Communications（コミュニケーション）** の両方がある。
- https://support.google.com/googleplay/android-developer/answer/9859673

**推奨: Tools（ツール）。** 本アプリは通話そのものを提供せず、OS の発信に介入する設定ユーティリティ。
Communications は通話・メッセージ・ブラウザなど通信手段そのものを提供するアプリの区分で、
そちらに置くと「電話アプリ」と誤解され、SMS / 通話履歴系のポリシー確認を余計に呼び込みかねない。

タグは**最大 5 個**。`電話` `ユーティリティ` あたりを選ぶ（正確な選択肢は Console 上で確認）。

### コンテンツ レーティング（IARC）

**必須。** 新規アプリは質問票に回答しないと「未評価」となり、地域によって配信が制限される。
- https://support.google.com/googleplay/android-developer/answer/9859655

IARC とのやり取り用にメールアドレスの入力が要る（§2 で作る Play 用アドレスでよい）。

**本アプリの回答:** カテゴリは「ユーティリティ、生産性、コミュニケーション、その他」を選ぶ。
暴力・性的表現・不適切な言葉・薬物・ギャンブル・恐怖 — **すべて「いいえ」**。
ユーザー間のやり取り、位置情報の共有、個人情報の共有、購入 — **すべて「いいえ」**。
→ 全年齢（日本では CERO 相当の最低区分 / IARC 3+）になる見込み。

> ⚠️ 「アプリが通話を発信できますか」に類する設問が出た場合、**アプリ自身は発信しない**
> （システムの発信に番号を渡すだけ）が、正直に答えたうえで説明を添えること。**設問の有無は未確認。**

### 対象年齢 / 広告

- **対象年齢**: 13 歳以上（または 18 歳以上）。子ども向けではない。
  PRIVACY.md にも「特に子どもを対象としたものではなく」と既に書いてある
- **広告**: 「このアプリには広告が含まれていません」。
  広告 SDK は一切入っていない（`app/build.gradle.kts` の依存関係で確認済み）。
  **広告を入れると §2 の住所非公開が崩れるので、入れないこと**

### その他の申告

| 申告 | 回答 |
|---|---|
| 政府アプリ | いいえ |
| 金融機能（ローン、送金、暗号資産、保険、投資） | **いいえ**。プレフィックスによって通話料金が変わるが、アプリが決済や送金を行うわけではない |
| ヘルスアプリ | いいえ |
| ニュース アプリ | いいえ |
| COVID-19 接触確認 | いいえ |
| データ セーフティ | §5 のとおり「収集しない」 |

- 申告の一覧: https://support.google.com/googleplay/android-developer/answer/9859455

### 「アプリのアクセス」（審査担当者への手順）

ログインは不要なので「**すべての機能が制限なく利用できます**」に相当する回答でよい。
ただし **`ROLE_CALL_REDIRECTION` をユーザーが付与しないとアプリの中核が動かない**ため、
補足の手順を書いておくと審査がスムーズになる。

> 🚨 **この欄に緊急通報番号を書かない。** 審査担当者が実際に発信を試みる可能性がある。
> 「保護されている番号の例」としてすら書かないこと。

**手順案（そのまま貼れる。発信は一切不要）:**

```
このアプリはログイン不要で、すべての機能が最初から利用できます。

中核機能（発信番号の書き換え）は Android の「通話リダイレクト」ロールを使います。
ロールを付与しなくても設定画面はすべて操作でき、書き換え結果も確認できます。

■ 実際に発信せずに動作を確認する手順

1. アプリを起動する
2. 「書き換えルール」を開き、プリセット（例: G-Call）を選ぶ
3. 各ルールの下に「書き換え前 → 書き換え後」の具体例が表示される
   （携帯 09012345678 / 固定 0312345678 / IP電話 05012345678 のダミー番号を使用）
   ここでプレフィックスと先頭 0 の扱いが正しく適用されることを確認できる
4. ホーム画面の「現在の設定」にも同じ形式の要約が出る
5. マスタースイッチを OFF にすると、すべて「変更せずに発信」に変わる

■ ロールを付与して確認する場合（任意）

ホーム画面の「通話リダイレクトを有効化」を押すと Android のシステムダイアログが出ます。
承認後、端末の電話アプリから任意の番号にダイヤルすると、アプリ内の「発信記録」に
その発信で何をしたか（書き換えた番号、または書き換えなかった理由）が記録されます。

■ 安全性について

緊急通報番号、3 桁の特番、# や * を含む番号は、ユーザーの設定内容に関わらず
書き換えられません。これはユーザー設定より上位の安全層として実装されており、
3 桁番号 000〜999 の総当たりを含むユニットテストで CI ごとに検証しています。

ソースコード: https://github.com/tmlksu/prefix-dialer (MIT)
```

手順 3 の「具体例表示」は既に実装されている（`ui/Labels.kt` の `sampleNumber()` と
`ui/RulesScreen.kt` / `ui/HomeScreen.kt`）。**発信させずに機能を見せられるのは大きい。**

---

## 7. 審査・テスト

### Pre-launch report（自動クロール）

Play Console は、アップロードしたビルドを実機で自動操作して
**安定性・Android 互換性・パフォーマンス・ユーザー補助**の問題を報告する。
Android 9 以上のスマートフォン／タブレット／Wear OS／Chromebook が対象。
- https://support.google.com/googleplay/android-developer/answer/7002270

**本アプリで出そうな結果:**

| 想定される事象 | 評価 | 対処 |
|---|---|---|
| **Robo クローラーがロールを付与できない** | ロール要求はシステムのダイアログで、クローラーはアプリ外の UI を操作できない。**「通話リダイレクトが OFF」の警告が出たままクロールが終わる** | **問題として報告されない見込み**（クラッシュでも ANR でもない）。対処不要 |
| **発信が一度も起きない** | クローラーは電話を掛けない | 同上。むしろ望ましい |
| **クローラーが「通話リダイレクトを有効化」を押してシステムダイアログに入り、そこで詰まる** | アプリ外に遷移するのでクロール範囲外になる | 実害なし。気になるなら Robo スクリプトで当該ボタンを避ける |
| **Chromebook / タブレットでのレイアウト崩れ** | Compose の縦スクロール中心の UI なので大きくは崩れない見込みだが、targetSdk 36 の「adaptive layouts」で大画面の扱いが変わる（§1） | **Pre-launch report の該当端末のスクリーンショットを必ず見る** |
| **ユーザー補助の警告**（タップ領域が小さい、コントラスト不足、`contentDescription` 欠落） | Material3 の既定コンポーネント中心なので少ない見込み。`MainActivity` の戻るアイコンには `contentDescription` がある | 出たら個別に対応。**リリースを止める性質のものではない** |
| `READ_PHONE_STATE` を拒否された状態でのクラッシュ | `PhoneAccounts.list()` は権限が無ければ空リストを返し、`SecurityException` も捕まえている | 問題なし |

**「アプリのアクセス」に §6 の手順を書いておくと、クローラーではなく人間の審査担当者に効く。**
Robo スクリプトの作成は**任意**。本アプリの規模では不要と判断する。

**推奨（任意）:** 発信せずに任意の番号の書き換え結果を確かめられる入力欄（「お試し」）を
「書き換えルール」画面に追加すると、Robo クローラーが触れる範囲で中核ロジックが動き、
審査担当者の確認手順も 1 つ減る。ダミー番号 3 種の表示は既にあるので、その延長。

### トラックの構成

| トラック | テスター数 | 反映速度 |
|---|---|---|
| 内部テスト | **最大 100 人** | 「数分以内にテスターが利用可能になる」 |
| クローズドテスト | メールアドレスのリスト **最大 200 件 × 各 2,000 人**、または Google グループ | 初回は「テストリンクが利用可能になるまで数時間かかることがある」 |
| オープンテスト | 制限なし | 同上 |
| 製品版 | — | 審査 7 日以内が目安 |

- https://support.google.com/googleplay/android-developer/answer/9845334

> ⚠️ **内部テストが 12 人 × 14 日の要件にカウントされるかは未確認。**
> 要件の文面は「クローズド テストを実施する」と明示しているので、
> **クローズドテスト トラックで 14 日を走らせること。** 内部テストは事前の動作確認に使う。

### 現実的なスケジュール

アカウントが未作成の前提で、**最短でも約 5〜6 週間**。

| 週 | やること | 所要 |
|---|---|---|
| **第 0 週** | targetSdk 36 対応（AGP/Gradle 引き上げ、ビルド、実機での発信確認やり直し）。アプリ内プライバシー リンク追加。`allowBackup` の判断。versionCode 更新 | 2〜4 日 |
| 同 | Play アカウント作成（$25）+ 本人確認 | 申請後**数日**。身分証の確認待ちがある |
| 同 | ストア掲載物の準備（512 アイコン、フィーチャー グラフィック、スクショ、説明文 ja/en） | 1〜2 日 |
| **第 1 週** | Play Console でアプリ作成 → **アプリ署名鍵をアップロード（最優先）** → 内部テストへ AAB を上げて自分の端末で動作確認 | 1 日 |
| 同 | **クローズドテスト開始。テスター 12 人を集めてオプトインさせる** | ここが律速 |
| **第 1〜3 週** | **14 日間の連続オプトイン期間。途中でオプトアウトされると数えなおし**。この間に Pre-launch report とストア掲載物を詰める | **14 日固定** |
| **第 3 週** | 本番環境へのアクセスを申請 | — |
| **第 4 週** | 申請の審査（通常 7 日以内） | 最大 7 日+ |
| **第 5 週** | 製品版へのリリース → アプリ審査（通常 7 日以内） | 最大 7 日+ |

**律速は「12 人 × 14 日」。** ここを短縮する方法は無い。

**テスターを集める現実的な手段:**

- 知人・家族（Google アカウントのメールアドレスが要る。端末で実際にインストールしてもらう）
- GitHub の Issue / README でテスター募集（既に 1.0.0 を公開しているので導線はある）
- ⚠️ **相互テスト用のコミュニティやサービスには注意。** テスターが実際にアプリを使っていない
  ことが分かると本番アクセス申請が却下される事例が報告されている。
  申請時に「テスターからどんなフィードバックを得たか」を書かされるため、
  **本当に使ってもらって、本当のフィードバックを受け取ること**

**注意:** 審査中にストア情報を変更すると審査がやり直しになる。第 4 週以降は触らない。
- https://support.google.com/googleplay/android-developer/answer/9859751

---

## 8. 技術要件その他

### 16 KB ページサイズ → **該当なし（実測で確認済み）**

§1-(c) のとおり。ネイティブ `.so` がゼロ。Java/Kotlin のみなので既に対応済み扱い。
- https://developer.android.com/guide/practices/page-sizes

### edge-to-edge → §1-(b)

`enableEdgeToEdge()` 実装済み。targetSdk 36 でオプトアウト不可になるため、**実機での表示確認が要る。**

### `android:exported` の明示

**対応済み。** `MainActivity`（`exported="true"`）、`PrefixRedirectionService`（`exported="true"` +
`android:permission="android.permission.BIND_CALL_REDIRECTION_SERVICE"`）、
`CallLogRewriteService`（`exported="false"`、github のみ）。
すべてのコンポーネントに明示されている。Android 12 以降の要件を満たす。

`PrefixRedirectionService` が `exported="true"` なのは正しい — システムからバインドされる必要があり、
`BIND_CALL_REDIRECTION_SERVICE` はシステムのみが保持する署名権限なので、他アプリからは触れない。

### deprecated API

コードを読んだ範囲で問題になる使用は見当たらない。

- `ACTION_NEW_OUTGOING_CALL` ブロードキャスト（Android 10 で非推奨）は**使っていない**。
  正しく `CallRedirectionService` を使っている
- `Uri.parse`（`SystemStatus.appDetailsIntent`）は非推奨ではない
- `PhoneAccounts.kt` の `SubscriptionManager` / `TelephonyManager.isNetworkRoaming` は現役
- `androidx.activity.enableEdgeToEdge` は推奨 API

> ⚠️ AGP 8.9 系は Lint のルールが増えている。引き上げ後に `./gradlew lintPlayRelease` を
> 走らせて、新しい警告を確認すること。

### versionCode の運用（GitHub 版と Play 版）

**前提:** 両フレーバーは `applicationId` も `versionCode` も共有している
（`defaultConfig` に書かれているため）。現在 `versionCode = 1` / `versionName = "1.0.0"`。

**問題:** `versionCode = 1` は **GitHub Releases の 1.0.0 で既に配布済み**。
同じ署名鍵・同じ applicationId なので両者は上書き更新できるが、
`versionCode` が同じだと Android / Play はどちらが新しいか判断できない。

**Play 側の制約:**
> 「You can't upload an APK to the Play Store with a `versionCode` you have already used」
> 最大値は **2,100,000,000**
> — https://developer.android.com/studio/publish/versioning

**推奨する運用: 両フレーバーで単一の連番を共有し、絶対に再利用しない。**

- Play 初回提出は **`versionCode = 2`**（`versionName` は `1.0.1` か `1.1.0`。targetSdk 36 対応と
  アプリ内プライバシー リンクの追加が入るので、`1.1.0` が実態に合う）
- 以降、GitHub 版と Play 版のどちらを出すときも同じ連番を進める。
  「GitHub は偶数、Play は奇数」のような分け方は**しない** — どちらが新しいかが分からなくなる
- フレーバーごとに `versionCode` をずらす設定（`flavor.versionCode = base * 10 + n` 等）も**採らない**。
  D-20 で「同じ鍵で相互に上書き更新できる」ことを狙って設計したのに、
  片方が常に大きくなると意図しない方向にだけ更新できるアプリになる

**注意:** GitHub 版の `versionCode` が Play 版を追い越すと、
Play 版を入れているユーザーに Play が更新を出さない状態が生じうる。
**リリースは片方ずつ、同じ `versionCode` を使い回さずに進めること。**
（この運用の詳細は、実際に 2 系統の配布を続けるなら DECISIONS.md に新しい項目として起票する価値がある）

### CI から Play へアップロード（任意）

現行の `.github/workflows/build.yml` はテストとビルド検証のみで、リリースは手作業。

**選択肢:**

| ツール | 評価 |
|---|---|
| **Gradle Play Publisher** (`com.github.triplet.play`) | Gradle プラグインとして完結する。`./gradlew publishPlayReleaseBundle` の形。**このプロジェクトに最もなじむ**（既に Gradle Kotlin DSL） |
| **fastlane supply** | Ruby 環境が増える。メタデータ・スクショの管理まで含めて扱える。多言語のストア掲載物を git で管理したいなら強い |

どちらも **Google Play Developer API を Cloud Console で有効化し、サービス アカウントを作って
Play Console 側で権限を付与する**必要がある。
- https://docs.fastlane.tools/actions/supply/
- https://github.com/Triple-T/gradle-play-publisher

**推奨: 当面やらない。**

- 初回提出は Console の手作業でしかできない工程（署名鍵アップロード、各種申告、掲載物）が中心で、
  自動化の余地が小さい
- サービス アカウントの JSON 鍵という**新しい秘密情報**が増える。
  このプロジェクトは鍵の管理を注意深く設計してきた（D-01 / D-20）ので、
  実際にリリース頻度が上がってから導入するほうが筋がよい
- リリースが年に数回なら手作業のほうが安い

---

## 9. GitHub 版との共存

### 相互更新できる前提の再確認 → **成立する。ただし条件が 2 つある**

| 条件 | 状態 |
|---|---|
| `applicationId` が同じ | ✅ 両フレーバーとも `io.github.tmlksu.prefixdialer`（`defaultConfig`。フレーバーで上書きしていない） |
| **署名証明書が同じ** | ⚠️ **Play App Signing に `release.keystore` をアプリ署名鍵としてアップロードした場合にのみ成立**（§3）。既定の Google 生成鍵を選んでしまうと**この時点で永久に破綻する** |
| `versionCode` の単調増加 | ⚠️ §8 の運用が要る |

**つまり D-20 の判断は現在も有効で、実行時の唯一の急所は「アプリ署名鍵の選択」。**
Play Console でアプリを作ったら、**他の何よりも先にこれを済ませること。**

Play App Signing が入ると、ユーザーに届く APK は Play が AAB から生成して署名する。
**バイト列は GitHub 配布の APK と一致しないが、署名証明書は同一**なので、
Android から見れば同じアプリの別バージョンとして扱われる。上書き更新が成立する。

（なお debug 版からの上書きが失敗する既知の問題（README / CHANGELOG / H-05）は、
debug 署名鍵が別物であるためで、これとは無関係。Play 版でも同じく debug からは上書きできない。）

### Play 版に履歴書き換えが無いことのユーザーへの案内

**必要。** 書かないと「GitHub 版にあった機能が消えた」という形で必ず問い合わせになる。
Deceptive Behavior の観点でも、機能差を明示しておくほうが安全。

**ストアの詳細な説明に入れる一文（ja）:**

> **この Google Play 版には「通話履歴の書き換え」機能はありません。**
> 発信後、端末の通話履歴にはプレフィックスが付いた番号が残ります。
> プレフィックスを付ける基本機能はこの機能を必要としないため、割引の適用そのものには影響しません。
> Google Play は、既定の電話アプリではないアプリが通話履歴の権限をマニフェストに宣言すること自体を
> 認めていないため、Play 版からはこの機能を権限ごと外しています。
> この機能が必要な場合は GitHub で配布している版をご利用ください:
> https://github.com/tmlksu/prefix-dialer/releases

**英語版:**

> **This Google Play build does not include call log rewriting.**
> After a call, the dialled number remains in your call history with the prefix attached.
> The core feature — adding the prefix — does not depend on it, so your discount still applies.
> Google Play does not allow apps that are not the default phone app to declare call log
> permissions in the manifest at all, so this build omits the feature and its permissions entirely.
> If you need it, use the build distributed on GitHub.

**置き場所:** 詳細な説明の**冒頭近く**（末尾だと読まれない）。
併せて README の「配布版の違い（フレーバー）」の表に Play ストアへのリンクを足すこと。

**アプリ内の案内も推奨:** Play 版の「詳細設定」画面には通話履歴のセクションが丸ごと出ない
（`AdvancedScreen` が `CallLogRewrite.AVAILABLE` で出し分けている）。
GitHub 版から移行したユーザーは「設定項目が消えた」と感じる。
Play 版では**その場所に 1 行の説明を出す**ほうが親切
（例: 「この版には通話履歴の書き換え機能はありません」）。

---

## 未確認・要判断

### 未確認（出典を特定できなかった / 記述が見つからなかった）

| # | 項目 | 状況 |
|---|---|---|
| 1 | **targetSdk の延長申請（〜2026-11-01）が新規アプリの初回提出に使えるか** | 原文は "update your app" の文脈。新規アプリへの適用は読み取れなかった。**いずれにせよ 36 に上げる必要があるので実害なし** |
| 2 | **API 35 → 36 の `android.telecom` API 差分** | 差分ページが 404。CallRedirectionService に変更が無いと断定できない。**36 に上げたら実機で発信を必ず再確認すること** |
| 3 | **AGP 8.9 系が Kotlin Gradle Plugin 1.9.24 を受け付けるか** | 下限 KGP のバージョンを確認できなかった。ビルドを試せば即座に分かる |
| 4 | **EU DSA のトレーダー申告** | Play Console に該当欄がある前提で語られることが多いが、公式ヘルプ記事を特定できなかった。無料・非収益の個人開発者が「非トレーダー」で申告できるかも未確認。**EU 配信を外せば丸ごと回避できる** |
| 5 | **PEPK ツールの正確なコマンドライン** | 公開ページに完全な引数一覧が無い。Console 画面に表示されるコマンドを使うこと |
| 6 | **アプリ署名鍵の選択変更が「内部テスト / クローズドテストに出した後」も可能か** | 原文は "open testing track or production track" のみ。**アプリ作成直後に済ませれば問題にならない** |
| 7 | **アップロード鍵を別途登録しない場合、アプリ署名鍵がそのままアップロード鍵になるか** | 確認できず。現在の `bundlePlayRelease` は `release.keystore` 署名なので、そのまま通る想定 |
| 8 | **既存の Play アカウントがある場合、クローズドテスト要件の対象になるか** | 2023-11-13 より前に作成した個人アカウントは対象外という読みだが未確認。Console の導線の有無で判別できる |
| 9 | **内部テストが「12 人 × 14 日」にカウントされるか** | 要件の文面は「クローズド テスト」と明示。**クローズドテスト トラックで走らせること** |
| 10 | **Android 自動バックアップ（`allowBackup="true"`）が Data safety の「収集」に当たるか** | 「収集」の主語は "your app" なので当たらない読みが自然だが、明示の記述なし。**`allowBackup="false"` にすれば曖昧さごと消える** |
| 11 | **ROLE_CALL_REDIRECTION に専用の宣言フォーム／動画提出が要るか** | 調べた範囲では見当たらない。提出時に Console で確認すること |
| 12 | **コンテンツ レーティング質問票に「通話の発信」に関する設問があるか** | 未確認。あれば正直に回答し、説明を添える |
| 13 | **タブレット用スクリーンショットが必須か** | 「最低 4 枚追加できる」という記述で、必須かどうかは読み取れなかった |

### 要判断（人間に決めてもらう）

| # | 判断事項 | 推奨 | 理由 |
|---|---|---|---|
| **P-01** | **`android:allowBackup` を `false` にするか** | **`false` にする** | 現状 `true` だと発信記録（＝発信した番号）と設定がユーザーの Google ドライブへ行く。PRIVACY.md の「端末の外へ出ることはありません」と整合しない。JSON エクスポートがあるので機種変時の移行手段は確保されている。**安全側に倒すという本プロジェクトの原則そのまま。§4 / §5** |
| **P-02** | **EU/EEA へ配信するか** | **配信しない** | 日本の番号計画専用のアプリで、EU の実益がほぼ無い。DSA トレーダー申告と住所公開の不確実性（未確認 #4）を丸ごと回避できる。配信先は日本（＋必要なら英語圏）に絞る |
| **P-03** | **アップロード鍵を別に作るか** | **初回は作らない。CI 自動化を始めるときに分離する** | 今のビルド設定は鍵 1 本前提。初回提出で鍵を 2 本にすると手順が増え、間違える余地が増える。§3 |
| **P-04** | **プライバシー ポリシーの URL** | **GitHub Pages を新設する** | blob URL でも通る見込みだが、"non-editable" の形式要件をつつかれる余地を消せる。日英併記のページを整形して出せる。§5。※ 2026-09-18 時点で別セッションが `docs/`（`privacy.html` ほか）を作成中。Pages 有効化は人手（docs/README.md） |
| **P-05** | **デベロッパー名** | 本名以外（例: `Tmlk`） | ストアで公開される。D-19 で GitHub のメール露出を潰した意図と一貫させる。§2 |
| **P-06** | **Play 用の連絡先メールアドレス** | **専用に新規作成する** | 公開される。既存のアドレスを使うと収集ボットの対象になる。§2 |
| **P-07** | **`versionName` を何にするか** | **`1.1.0` / `versionCode = 2`** | targetSdk 36 対応、アプリ内プライバシー リンク追加、`allowBackup` 変更が入るので patch ではなく minor。§8 |
| **P-08** | **将来も無料を貫くか** | **貫く** | 有料化・アプリ内購入・広告のいずれかを入れると、Play で詳細住所が公開され、さらに日本の特定商取引法により氏名・電話番号・住所の表示義務が発生する。個人情報の露出を避けたい意向と正面から衝突する。§2 |
| **P-09** | **「お試し」入力欄を追加するか** | 任意。余裕があれば | 審査担当者と Robo クローラーの両方に効く。既存の `sampleNumber()` 表示の延長で実装できる。§6 / §7 |

---

## 出典一覧

**targetSdk / Android 16 / ツールチェーン**
- https://developer.android.com/google/play/requirements/target-sdk
- https://support.google.com/googleplay/android-developer/answer/11926878?hl=ja
- https://developer.android.com/about/versions/16/behavior-changes-16
- https://developer.android.com/build/releases/about-agp
- https://developer.android.com/jetpack/androidx/releases/compose-kotlin
- https://developer.android.com/develop/ui/compose/compiler
- https://android-developers.googleblog.com/2024/04/jetpack-compose-compiler-moving-to-kotlin-repository.html
- https://developer.android.com/guide/practices/page-sizes
- https://developer.android.com/studio/publish/versioning

**アカウント / テスト要件 / 本人確認**
- https://support.google.com/googleplay/android-developer/answer/6112435
- https://support.google.com/googleplay/android-developer/answer/13628312
- https://support.google.com/googleplay/android-developer/answer/13634081?hl=ja
- https://support.google.com/googleplay/android-developer/answer/14151465?hl=ja
- https://support.google.com/googleplay/android-developer/answer/6223646?hl=ja
- https://support.google.com/googleplay/android-developer/answer/14659200
- https://developer.android.com/developer-verification/guides/faq

**配布形式 / 署名 / リリース**
- https://developer.android.com/guide/app-bundle
- https://android-developers.googleblog.com/2021/06/the-future-of-android-app-bundles-is.html
- https://support.google.com/googleplay/android-developer/answer/9842756
- https://developer.android.com/studio/publish/app-signing
- https://support.google.com/googleplay/android-developer/answer/9848633
- https://support.google.com/googleplay/android-developer/answer/9859348
- https://support.google.com/googleplay/android-developer/answer/9859751
- https://support.google.com/googleplay/android-developer/answer/9845334
- https://support.google.com/googleplay/android-developer/answer/7002270

**ポリシー**
- https://support.google.com/googleplay/android-developer/answer/10208820?hl=ja （SMS / 通話履歴の権限グループ）
- https://support.google.com/googleplay/android-developer/answer/9888170 （機密情報にアクセスする権限と API）
- https://support.google.com/googleplay/android-developer/answer/9888379 （Device and Network Abuse）
- https://support.google.com/googleplay/android-developer/answer/9888077 （Deceptive Behavior）
- https://support.google.com/googleplay/android-developer/answer/10144311 （ユーザーデータ / プライバシー ポリシー）
- https://support.google.com/googleplay/android-developer/answer/10787469 （Data safety）

**ストア掲載**
- https://support.google.com/googleplay/android-developer/answer/9866151 （グラフィック アセット）
- https://support.google.com/googleplay/android-developer/answer/9859152 （ストアの掲載情報）
- https://support.google.com/googleplay/android-developer/answer/9859673 （カテゴリとタグ）
- https://support.google.com/googleplay/android-developer/answer/9859655 （コンテンツ レーティング）
- https://support.google.com/googleplay/android-developer/answer/9859455 （アプリのコンテンツ / 各種申告）

**API / 実装**
- https://developer.android.com/develop/connectivity/telecom/dialer-app/redirect-a-call
- https://developer.android.com/reference/android/telecom/CallRedirectionService
- https://developer.android.com/identity/data/autobackup

**CI 自動化（任意）**
- https://github.com/Triple-T/gradle-play-publisher
- https://docs.fastlane.tools/actions/supply/
