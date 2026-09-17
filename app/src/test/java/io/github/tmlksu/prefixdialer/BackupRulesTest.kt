package io.github.tmlksu.prefixdialer

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Android 自動バックアップから発信記録だけを除外していることの固定（P-01 案 B）。
 *
 * `android:allowBackup="true"` を維持し、発信記録（`CallRecordStore.PREFS_NAME`）の
 * SharedPreferences ファイルだけを `fullBackupContent` / `dataExtractionRules` で除外する。
 * 設定（`SettingsStore.PREFS_NAME`）は機種変で引き継ぐため除外しない。
 *
 * テスト実行時のカレントディレクトリは `app/` のため、`src/main/res/xml/...` を
 * ファイルとして直接読む。Android 依存なし（`javax.xml` のみ）。
 */
class BackupRulesTest {

    private val expectedExcluded = "${CallRecordStore.PREFS_NAME}.xml"
    private val settingsFile = "${SettingsStore.PREFS_NAME}.xml"

    private fun parse(file: File): Element {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        doc.documentElement.normalize()
        return doc.documentElement
    }

    /** 指定した親要素直下の `exclude[domain=sharedpref]` の path 一覧。親自体が root の場合も含む。 */
    private fun excludedPaths(root: Element, parentTag: String): List<String> {
        val parents = root.getElementsByTagName(parentTag)
        val out = mutableListOf<String>()
        if (root.tagName == parentTag) collectExcludes(root, out)
        for (i in 0 until parents.length) {
            collectExcludes(parents.item(i) as Element, out)
        }
        return out
    }

    private fun collectExcludes(parent: Element, out: MutableList<String>) {
        val excludes = parent.getElementsByTagName("exclude")
        for (j in 0 until excludes.length) {
            val e = excludes.item(j) as Element
            if (e.getAttribute("domain") == "sharedpref") {
                out += e.getAttribute("path")
            }
        }
    }

    @Test
    fun `backup_rulesは発信記録だけを除外し設定は除外しない`() {
        val root = parse(File("src/main/res/xml/backup_rules.xml"))
        val paths = excludedPaths(root, "full-backup-content")
        assertTrue(paths.contains(expectedExcluded))
        assertFalse(paths.contains(settingsFile))
    }

    @Test
    fun `data_extraction_rulesはcloud-backupとdevice-transferの両方で発信記録を除外する`() {
        val root = parse(File("src/main/res/xml/data_extraction_rules.xml"))
        for (parent in listOf("cloud-backup", "device-transfer")) {
            val paths = excludedPaths(root, parent)
            assertTrue("$parent に $expectedExcluded の除外が無い", paths.contains(expectedExcluded))
            assertFalse("$parent が $settingsFile を除外してはならない", paths.contains(settingsFile))
        }
    }
}
