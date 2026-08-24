package com.example.prefixdialer

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.ShortNumberInfo

/**
 * 「絶対にプレフィックスを付けてはいけない番号」の判定。
 *
 * ## このオブジェクトの位置づけ
 *
 * これは**ユーザー設定より上位の安全層**である。将来プレフィックスやルールを
 * UI から自由に編集できるようになっても、ここで protected と判定された番号は
 * どんなルールでも書き換えられてはならない。
 * 呼び出し側（[PhoneNumberPrefixer]）はルール評価より **前** にこれを通すこと。
 *
 * ## 設計方針: 名簿ではなく構造で守る
 *
 * 特番の一覧を列挙して維持する方式は破綻する。総務省の 1XY 割当は改廃があり
 * （例: 177 天気予報は 2025-03-31 終了）、`#` 系の 4 桁番号は所管がばらばらで
 * 公式の網羅リストが存在しない。リストの更新漏れ = 緊急通報の書き換え事故になる。
 *
 * そこで「範囲」で弾く:
 *  - 3 桁以下の番号は **すべて** 対象外（1XY 帯は総務省が特番用に予約している。
 *    キャリアプレフィックスを付けて得をする 3 桁番号は存在しない）
 *  - `#` / `*` を含む番号は **すべて** 対象外（#7119 #8000 #9110 #8103 と将来の追加を一括で守る）
 *
 * これなら新しい特番が増えても無改修で安全側に倒れる。
 *
 * ## 誤判定のコスト非対称性
 *
 * 過剰にブロック = プレフィックスが付かない（通話料が割引されないだけ、実害なし）。
 * ブロック漏れ   = 緊急通報の番号が書き換わる（最悪、人命に関わる）。
 * よって迷ったら必ずブロック側に倒すこと。
 *
 * ## libphonenumber を主軸にしない理由（8.13.42 で実測）
 *
 * | 番号 | isEmergencyNumber | isValidShortNumber | 備考 |
 * |---|---|---|---|
 * | `110` | true | true | |
 * | `119` | true | true | |
 * | `118` | **false** | true | 海上保安庁。緊急番号として認識されない |
 * | `112` | **false** | **false** | 携帯網で警察に接続されるが未収録 |
 * | `117` `171` `188` `113` `115` `116` `100` | false | false | 未収録 |
 *
 * さらに `isPossibleShortNumberForRegion` は `09012345678` や `08001234567` にも
 * true を返すため、ガードには使えない（通常の携帯番号まで巻き込む）。
 * したがって libphonenumber は「補助の網」としてのみ使い、判定の主軸は上記の構造ルールに置く。
 */
object ProtectedNumbers {

    private const val REGION_JP = "JP"
    private const val JP_COUNTRY_CODE = "81"

    private val shortNumberInfo: ShortNumberInfo = ShortNumberInfo.getInstance()
    private val phoneUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()

    /**
     * 発信者番号の通知/非通知プレフィックス。
     *
     * `184`/`186` は後続の番号と合わせて 1 つの発信になる。キャリアプレフィックスとの
     * 併用順序（`184` が先か `0063` が先か）は事業者依存で、誤ると非通知が効かないまま
     * 発信される恐れがあるため、1.0 では触らず素通しする。
     */
    private val CALLER_ID_PREFIXES = listOf("184", "186")

    /** ダイヤル中のポーズ/待機を表す文字。これらを含む番号は DTMF 後続付きとみなす。 */
    private const val DTMF_PAUSE_CHARS = ",;pPwW"

    /**
     * 参考情報。**判定には使わない**（3 桁ルールと `#` ルールで既に覆われている）。
     *
     * ガードが壊れていないことを確認するためのテスト用データであり、
     * 同時に「何を守っているのか」のドキュメントでもある。
     * 出典: 総務省 1XY 番号利用指針 / NTT東日本 電話の3桁番号サービス。
     */
    val DOCUMENTED_SPECIAL_NUMBERS: Map<String, String> = mapOf(
        // --- 緊急通報 ---
        "110" to "警察",
        "118" to "海上保安庁",
        "119" to "消防・救急",
        "112" to "GSM共通緊急番号（携帯網で警察へ接続）",
        // --- 緊急に準じる相談窓口（3桁） ---
        "171" to "災害用伝言ダイヤル",
        "188" to "消費者ホットライン",
        "189" to "児童相談所虐待対応ダイヤル",
        // --- 生活・通信サービス（3桁） ---
        "100" to "オペレータ通話",
        "104" to "電話番号案内",
        "106" to "コレクトコール（オペレータ）",
        "108" to "自動コレクトコール",
        "113" to "電話の故障受付",
        "114" to "話中調べ",
        "115" to "電報の申し込み",
        "116" to "電話の新設・移転・各種相談",
        "117" to "時報",
        "177" to "天気予報（2025-03-31 提供終了）",
        // --- 発信者番号通知プレフィックス ---
        "184" to "発信者番号 非通知",
        "186" to "発信者番号 通知",
        // --- #系 4桁（所管がばらばらで公式の網羅リストは存在しない） ---
        "#7119" to "救急安心センター（救急車を呼ぶか迷った時）",
        "#8000" to "こども医療でんわ相談",
        "#9110" to "警察相談専用電話",
        "#8103" to "性犯罪被害相談電話",
        "#8891" to "性犯罪・性暴力被害者のためのワンストップ支援センター",
    )

    /**
     * この番号にプレフィックスを付けてはいけないなら true。
     *
     * @param raw ダイヤラーから渡された生の番号文字列（整形前でよい）
     */
    fun isProtected(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return true

        val trimmed = raw.trim()

        // 1) # / * を含む番号はすべて保護。
        //    #7119 #8000 #9110 #8103 等の相談ダイヤルと、将来追加される # 番号、
        //    および *xx 形式のキャリアサービスコードを一括で守る。
        if (trimmed.any { it == '#' || it == '*' }) return true

        // 2) ポーズ/待機文字を含む番号は保護。
        //    「代表番号 + 内線」のような DTMF 後続付き。プレフィックスを付けると
        //    後続トーンの送出タイミングが崩れる恐れがあるため 1.0 では素通しする。
        if (trimmed.any { it in DTMF_PAUSE_CHARS }) return true

        val digits = trimmed.filter { it.isDigit() }
        if (digits.isEmpty()) return true

        // 3) 表記ゆれをすべて展開してから判定する。
        //    "110" / "+81110" / "81110" は同じ相手に繋がるので、どの書き方でも保護されねばならない。
        //    候補のいずれか 1 つでも該当したら保護する（安全側に倒す）。
        val candidates = normalizedForms(trimmed, digits)

        // 4) 3桁以下はすべて保護。1XY 帯は特番用の予約帯であり、
        //    プレフィックスを付ける正当な理由を持つ 3 桁番号は存在しない。
        //    110/118/119/112/911/117/171/188/189/104/113/115/116 … を一括で覆う。
        if (candidates.any { it.length <= 3 }) return true

        // 5) 発信者番号通知/非通知プレフィックスで始まる番号は保護（順序問題のため）。
        if (candidates.any { form -> CALLER_ID_PREFIXES.any { form.startsWith(it) } }) return true

        // 6) 補助の網: libphonenumber が緊急通報に繋がると判断するものは保護。
        //    connectsToEmergencyNumber は前方一致で判定するため 3桁ルールの取りこぼしを拾える。
        //    isPossibleShortNumber は通常の携帯番号にも true を返すので使わない。
        if (candidates.any { shortNumberInfo.connectsToEmergencyNumber(it, REGION_JP) }) return true
        if (candidates.any { shortNumberInfo.isEmergencyNumber(it, REGION_JP) }) return true
        if (candidates.any { isValidShortNumber(it) }) return true

        return false
    }

    /**
     * 判定にかける番号表記の候補を返す。
     *
     * 例: `+81110` -> `81110`(生), `110`(国番号除去), `0110`(国内表記)
     *     `110`    -> `110`
     *     `09012345678` -> `09012345678`, `9012345678`
     *
     * 過剰に候補を挙げても「プレフィックスが付かない」だけで実害がないため、
     * 取りこぼしを防ぐ方向に広く取る。
     */
    private fun normalizedForms(trimmed: String, digits: String): Set<String> {
        val forms = linkedSetOf(digits)

        val hasPlus = trimmed.startsWith("+")
        if (digits.startsWith(JP_COUNTRY_CODE) && (hasPlus || digits.length > 3)) {
            val subscriber = digits.substring(JP_COUNTRY_CODE.length)
            if (subscriber.isNotEmpty()) {
                forms += subscriber                       // 国番号を落とした形
                forms += "0" + subscriber                 // 国内表記(先頭0)
            }
        }
        // 先頭 0 を落とした形。"0110" のような書かれ方で桁数チェックをすり抜けさせない。
        digits.trimStart('0').takeIf { it.isNotEmpty() }?.let { forms += it }

        return forms
    }

    private fun isValidShortNumber(number: String): Boolean = try {
        shortNumberInfo.isValidShortNumberForRegion(
            phoneUtil.parse(number, REGION_JP),
            REGION_JP,
        )
    } catch (e: NumberParseException) {
        true                                              // パースできないものは触らない
    }
}
