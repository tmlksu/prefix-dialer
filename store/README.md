# store/ — Google Play ストア掲載用グラフィック素材

Play Console にアップロードする画像と、その生成元 SVG・生成スクリプトを置く。
仕様の出典は `PLAY-RELEASE.md` §6、棚卸しは `PLAY-LISTING.md` §2。

生成日: **2026-09-18**

---

## 1. ファイル一覧

| パス | 用途 | 実測 |
|---|---|---|
| `src/icon.svg` | ストア用アイコンの元データ（フルブリード正方形） | 512×512 相当・viewBox 0 0 108 108 |
| `icon-512.png` | Play Console の「アプリ アイコン」 | **512×512 / RGBA(32bit) / 13,337 bytes (13.0 KB)** |
| `src/feature-graphic.svg` | フィーチャー グラフィックの元データ | 1024×500 |
| `feature-graphic-1024x500.png` | Play Console の「フィーチャー グラフィック」 | **1024×500 / RGB(24bit) / 53,361 bytes (52.1 KB)** |
| `src/build.py` | 上記 2 つの PNG を SVG から生成・検証するスクリプト | — |

スクリーンショットは**まだ無い**。→ §5。

---

## 2. Play の仕様との照合

出典: https://support.google.com/googleplay/android-developer/answer/9866151
（`PLAY-RELEASE.md` §6 の表と同じ）

### アプリアイコン

| 要件 | 実測 | 判定 |
|---|---|---|
| 512 × 512 px | 512×512 | ✅ |
| 32 ビット PNG（アルファあり） | mode=`RGBA`（4ch = 32bit） | ✅ |
| 最大 1024 KB | 13.0 KB | ✅ |
| 透過は避け背景色で全面を埋める（`PLAY-LISTING.md` §2） | アルファ値の extrema = `(255, 255)`（全画素不透明）<br>四隅 4 点とも `(27, 94, 90, 255)` = `#1B5E5A` | ✅ |
| フルブリード（マスク前提の縮小を解除） | 図柄の実占有 **378×390 px = 辺の 74% × 76%**（目安 70〜80%） | ✅ |

> アプリ内のアダプティブアイコンは squircle マスク対策で `group scale 0.58` に縮小してある
> （DECISIONS.md D-17）。ストア用はマスクが掛からないので、この縮小を**解除**し、
> 図柄の実バウンディングボックス（108 単位系で w=74.0 / h=76.2、中心 (54, 53.1)）を
> キャンバス中心に合わせたうえで `scale(1.08)` で拡大している。`src/icon.svg` のコメント参照。

### フィーチャー グラフィック

| 要件 | 実測 | 判定 |
|---|---|---|
| 1024 × 500 px | 1024×500 | ✅ |
| JPEG または 24 ビット PNG（**アルファなし**） | mode=`RGB`、bands=`('R','G','B')` | ✅ |
| 文字が小さすぎないこと（Play ではサムネイル表示） | アプリ名 78px（高さの 15.6%）／タグライン 32px（同 6.4%） | ✅ |
| 端で切れていないこと | 白い図柄・文字の bbox = `(93, 166, 943, 334)`、右マージン **81px**、上下左右とも 16px 以上の余白 | ✅ |

内容: 背景は `#22726C` → `#154A47` の斜めグラデーション（アイコン背景 `#1B5E5A` 系）、
左に角丸タイル入りのアイコン図柄、右に `Prefix Dialer` と
`Adds your carrier prefix, automatically`。

> **日本語テキストは意図的に入れていない。** ヘッドレス環境に日本語フォントが無いと
> 豆腐（□）になり、PNG を目視するまで気づけないため。日本語版のフィーチャー グラフィックが
> 必要になったら、日本語フォント（Noto Sans CJK 等）を入れたうえで別途作ること。
> この環境の `fc-list` に CJK は `Droid Sans Fallback` しか無い。

---

## 3. 生成手順（再現可能な形）

### 3.1 ツールの導入

ラスタライザは **cairosvg**（libcairo を使う Python 実装）。
`rsvg-convert` / `inkscape` / ImageMagick はこの環境に無い。

```bash
# libcairo があることの確認（あれば cairosvg が動く）
ldconfig -p | grep libcairo.so
#   => libcairo.so.2 (libc6,x86-64) => /lib/x86_64-linux-gnu/libcairo.so.2

pip install --user --break-system-packages cairosvg pillow
```

> `--break-system-packages` を付けている理由: この環境の pip は PEP 668 で
> externally-managed。本来は venv を使うところだが、`python3 -m venv` は
> `python3.12-venv` パッケージが無く失敗し、その導入には apt（= システム全体への変更）が要る。
> `--user` を併用しているので書き込み先は `~/.local/lib/python3.12/site-packages` のみで、
> システムの `/usr` 配下には一切触れていない。
> venv が使える環境なら `python3 -m venv .venv && .venv/bin/pip install cairosvg pillow` の方が望ましい。

検証に使ったバージョン: Python 3.12.3 / cairosvg 2.9.1 / Pillow 12.3.0

### 3.2 生成

リポジトリのルートで:

```bash
python3 store/src/build.py
```

出力（このスクリプトは生成後に寸法・モード・ファイルサイズ・透過の有無・
図柄が端で切れていないかを検証して表示する）:

```
icon-512.png                       512x512 RGBA    13337 bytes (13.0 KB)  alpha=(255, 255)  OK
feature-graphic-1024x500.png       1024x500 RGB     53361 bytes (52.1 KB)  OK
  ink bbox (明るい画素の範囲) = (93, 166, 943, 334) / margin right = 81 px
すべて OK
```

スクリプトを使わず 1 コマンドで出す場合（ビット深度の調整が入らない点に注意。
cairosvg は全画素が不透明だと RGB で書き出すため、アイコンは 24bit になる）:

```bash
python3 -c "import cairosvg; cairosvg.svg2png(url='store/src/icon.svg', write_to='store/icon-512.png', output_width=512, output_height=512)"
```

### 3.3 フォント

フィーチャー グラフィックの文字は SVG の `<text>` で、`font-family="DejaVu Sans"`。
cairosvg は fontconfig 経由で解決する。

```bash
fc-match "DejaVu Sans"        # => DejaVuSans.ttf: "DejaVu Sans" "Book"
fc-match "DejaVu Sans:bold"   # => DejaVuSans-Bold.ttf: "DejaVu Sans" "Bold"
```

別環境で DejaVu が無い場合は `font-family` を `sans-serif` に変えれば動く
（字幅が変わるので、生成後に右端マージンを `build.py` の検証出力で確認すること）。

### 3.4 目視確認

**数値チェックだけでは不十分**（フォントが無くて豆腐になっても寸法は正しいままなので）。
生成した 2 枚は画像として開いて確認済み:

- `icon-512.png` — 受話器と 3 ドットが四辺で切れておらず、四隅まで `#1B5E5A` で埋まっている。
- `feature-graphic-1024x500.png` — `Prefix Dialer` と英文タグラインが豆腐にならず描画され、
  右端に余白があり、アイコンタイルと重なっていない。

---

## 4. 素材の出自

- 図柄: `app/src/main/res/drawable/ic_launcher_foreground.xml`（受話器 + 3 ドット、白）
- 背景色: `app/src/main/res/values/ic_launcher_background.xml` の `#1B5E5A`
- `docs/assets/icon.svg`（別担当が移植済みのフルブリード SVG）を参照した。
  ただし `docs/assets/icon.svg` は**アプリ内と同じ `scale(0.58)` のまま**なので、
  ストア用にはそのまま使えない。`store/src/icon.svg` はその縮小を解除したもの。
  `app/` `docs/` 側のファイルは一切変更していない。

---

## 5. スクリーンショット（未作成・撮影の選択肢）

Play の必須要件: **スマートフォン 最低 2 枚**（最大 8 枚）、JPEG または 24bit PNG（アルファなし）、
最小辺 320 px / 最大辺 3840 px、縦 **9:16**（最小 1080×1920）。
タブレット / Chromebook は最低 4 枚（必須かは対象デバイス設定による。`PLAY-RELEASE.md` §6 では「未確認」）。

### (a) 実機の adb で撮る

接続中の端末を読み取り専用で調べた結果（**アプリの操作・インストールは行っていない**）:

| serial | モデル | Android | 画面 | `io.github.tmlksu.prefixdialer` |
|---|---|---|---|---|
| `RFCY30Z5RCK` (USB) | **SM-S931Z**（Galaxy S25） | 16 (SDK 36) | 1080×2340 | **インストール済み**（`versionName=1.0.0`、`lastUpdateTime=2026-08-26 12:36:36`、`pkgFlags=[ HAS_CODE ALLOW_CLEAR_USER_DATA ALLOW_BACKUP KILL_AFTER_RESTORE ]`） |
| `172.20.121.133:5555` | Echo Show 5 (2nd Generation) / LineageOS | 11 (SDK 30) | 960×480 | 未インストール |
| `172.20.121.214:5555` | Echo Show 5 (2nd Generation) / LineageOS | 11 (SDK 30) | 960×480 | 未インストール |

> フレーバー（`play` / それ以外）は `dumpsys package` からは判別できなかった。
> `lastUpdateTime` が 2026-08-26 で、フレーバー分離のコミット（`Play 配布用にフレーバーを分ける`）より前の
> 1.0.0 なので、**`play` フレーバーではない可能性が高い**。

撮影コマンド:

```bash
mkdir -p store/screenshots
adb -s <serial> exec-out screencap -p > store/screenshots/01-home.png
```

**アスペクト比の注意:** S25 の生キャプチャは 1080×2340 = 約 **9:19.5** で、
上表の 9:16 に収まらない（最小辺 1080 と最大辺の条件は満たす）。提出前にどちらかで揃えること:

```bash
# 案 1: 9:16 にクロップ（下端 420px を落とす。サムネイル映えはこちら）
python3 -c "from PIL import Image; im=Image.open('store/screenshots/01-home.png').convert('RGB'); im.crop((0,0,1080,1920)).save('store/screenshots/01-home.png')"

# 案 2: 左右に背景色を足して 1316×2340 にする（画面の内容を一切削らない）
```

いずれの案でも、最後に **24bit PNG（アルファなし）**へ変換すること
（`screencap -p` の出力は RGBA になることがある）:

```bash
python3 -c "from PIL import Image; import sys; p=sys.argv[1]; Image.open(p).convert('RGB').save(p)" store/screenshots/01-home.png
```

### (b) ⚠️ 常用端末で撮らないこと

`RFCY30Z5RCK` は**作者の常用端末**で、実在の電話番号が**発信記録**と**除外リスト**に入っている。
そのまま撮るとストア掲載画像に他人の電話番号が載る。

→ **ストア用は `play` フレーバーをクリーンな端末かエミュレータに入れて撮ること。**

エミュレータの用意状況（調査のみ）:

- `ANDROID_HOME` / `ANDROID_SDK_ROOT` は**未設定**
- `~/Android/Sdk` は**無い**。`/usr/lib/android-sdk` はあるが中身は `platform-tools` のみ（= adb だけ）
- `emulator` / `avdmanager` / `sdkmanager` / `qemu-system-x86_64` いずれも**PATH に無い**
- `~/.android/avd` も**無い**（AVD 未作成）
- Android Studio も見当たらない

→ **エミュレータは現状この環境では使えない。** 使うなら commandline-tools と system-image の
導入（数 GB）か、Android Studio の導入が必要。それが重いなら、予備端末を用意して
`play` フレーバーの debug ビルドを入れ、ダミーデータだけで撮るのが早い。

### (c) 撮る画面の推奨リスト

1. **ホーム** — マスタースイッチ ON と「現在の設定」の要約が見える状態
2. **書き換えルール** — プリセット選択と「書き換え前 → 書き換え後」のサンプル表示が見える状態
   （`ui/Labels.kt` の `sampleNumber()` によるダミー番号 09012345678 / 0312345678 / 05012345678。
   実在の番号が映らないので掲載向き。審査手順としても `PLAY-RELEASE.md` §6 が推している画面）
3. **発信記録** — ダミー端末で 1〜2 件だけ作った記録。**実在の番号が 1 件でも残っていないこと**
4. **詳細設定** — 除外リストや安全層の説明が見える画面。**除外リストは空か、ダミー番号のみ**

🚨 **緊急通報番号を画面に映さない。**「保護されている番号の例」としてもスクリーンショットに入れないこと
（`PLAY-RELEASE.md` §6 の「アプリのアクセス」欄と同じ理由 — 審査担当者が発信を試みうる）。
発信記録・除外リスト・入力欄のいずれにも残さない。

---

## 6. 残っているもの

- [ ] スマートフォン スクリーンショット 2〜8 枚（§5）
- [ ] タブレット / Chromebook スクリーンショット 4 枚（必要かは対象デバイス設定次第・未確認）
- [ ] 日本語版フィーチャー グラフィック（作るなら。日本語フォントの導入が前提）
