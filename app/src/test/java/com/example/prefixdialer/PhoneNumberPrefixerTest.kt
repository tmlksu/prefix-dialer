package com.example.prefixdialer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * buildDialNumber の網羅テスト。
 * libphonenumber を使うだけなので JVM 上のローカルユニットテストで動く。
 */
class PhoneNumberPrefixerTest {

    private fun dial(raw: String) = PhoneNumberPrefixer.buildDialNumber(raw)

    // --- プレフィックスを付ける（国内の携帯・固定・IP電話） -----------------

    @Test fun mobile_getsPrefix() {
        assertEquals("006309012345678", dial("09012345678"))
    }

    @Test fun mobile_withHyphens_getsPrefix() {
        assertEquals("006309012345678", dial("090-1234-5678"))
    }

    @Test fun mobile_e164_isConvertedThenPrefixed() {
        // +8190… は 090… に正規化してから付与
        assertEquals("006309012345678", dial("+819012345678"))
    }

    @Test fun mobile_070_getsPrefix() {
        assertEquals("006307012345678", dial("07012345678"))
    }

    @Test fun tokyoLandline_getsPrefix() {
        assertEquals("00630312345678", dial("0312345678"))
    }

    @Test fun osakaLandline_e164_getsPrefix() {
        assertEquals("00630612345678", dial("+81612345678"))
    }

    @Test fun ipPhone_050_getsPrefix() {
        assertEquals("006305012345678", dial("05012345678"))
    }

    // --- プレフィックスを付けない ------------------------------------------

    @Test fun tollFree_0120_noPrefix() {
        assertNull(dial("0120123456"))
    }

    @Test fun tollFree_0800_noPrefix() {
        assertNull(dial("0800123456"))
    }

    @Test fun naviDial_0570_noPrefix() {
        assertNull(dial("0570001234"))
    }

    @Test fun premium_0990_noPrefix() {
        assertNull(dial("0990123456"))
    }

    @Test fun internationalDialFromJp_010_noPrefix() {
        assertNull(dial("010112025550123"))
    }

    @Test fun foreignE164_us_noPrefix() {
        assertNull(dial("+12125550123"))
    }

    @Test fun otherCarrierSelect_00xx_noPrefix() {
        assertNull(dial("0033044123456"))
    }

    @Test fun alreadyPrefixed_noDoublePrefix() {
        assertNull(dial("006309012345678"))
    }

    @Test fun emergency_110_noPrefix() {
        assertNull(dial("110"))
    }

    @Test fun emergency_119_noPrefix() {
        assertNull(dial("119"))
    }

    @Test fun empty_noPrefix() {
        assertNull(dial(""))
    }

    @Test fun garbage_noPrefix() {
        assertNull(dial("abc"))
    }
}
