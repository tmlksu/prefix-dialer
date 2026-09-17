import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

/**
 * リリース署名の設定は keystore.properties から読む。このファイルと keystore 本体は
 * リポジトリに含めない（.gitignore 済み）。
 *
 * 用意されていない場合は署名設定を作らず、release ビルドは未署名になる。
 * これは意図した挙動で、鍵が無い環境でも `assembleDebug` と `test` は通る。
 * 手順は DECISIONS.md の D-01 を参照。
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

/**
 * 署名に必要な値が揃っているか。
 *
 * パスワードが空のまま `keystore.properties` を置いた場合も未設定として扱う。
 * 中途半端な設定でビルドを失敗させるより、未署名で通したほうが原因が分かりやすい。
 */
val hasSigningConfig = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
    .all { !keystoreProperties.getProperty(it).isNullOrBlank() } &&
    rootProject.file(keystoreProperties.getProperty("storeFile") ?: "").exists()

android {
    namespace = "io.github.tmlksu.prefixdialer"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.tmlksu.prefixdialer"
        minSdk = 29          // Android 10: CallRedirectionService が使える最低ライン
        targetSdk = 36       // Android 16。Play は 2026-08-31 以降の新規提出に必須
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasSigningConfig) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    /**
     * 配布先ごとにフレーバーを分ける。
     *
     * Google Play は、既定の電話 / SMS / アシスタントアプリではないアプリが
     * CALL_LOG 権限グループを**マニフェストに宣言すること自体**を禁じている。
     * 例外が認められる用途の一覧にも「通話のリダイレクト」は存在せず、
     * さらに例外の条件は「その権限がコア機能を実現していること」だが、
     * 本アプリは履歴の書き換えを任意機能として設計している（＝コアではない）。
     *
     * よって Play 版からは権限・サービス・実装コードごと外す。
     * 基本機能（プレフィックス付与）はこの権限を必要としないので成立する。
     *
     * - github: 全機能。GitHub Releases で直接配布する版
     * - play  : 通話履歴の書き換えなし。宣言する権限は READ_PHONE_STATE のみ
     *
     * 両者は applicationId も署名鍵も同じなので、相互に上書き更新できる。
     */
    flavorDimensions += "distribution"
    productFlavors {
        create("github") { dimension = "distribution" }
        create("play") { dimension = "distribution" }
    }

    buildFeatures {
        compose = true
        // 「このアプリについて」でバージョン名を出すために BuildConfig が要る。
        // AGP 8.0 以降は既定で生成されない。
        buildConfig = true
    }
    composeOptions {
        // Kotlin 1.9.24 に対応する Compose Compiler。Kotlin を上げるときは
        // https://developer.android.com/jetpack/androidx/releases/compose-kotlin の対応表を見ること
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    buildTypes {
        release {
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                // 未署名で通ること自体は意図した挙動（鍵の無い CI でもビルドできる）。
                // ただし黙って未署名 APK が出来ると、配布直前まで気づけない。
                logger.lifecycle(
                    "[PrefixDialer] keystore.properties が無いか値が空のため、" +
                        "release APK は未署名になります。",
                )
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.android.material:material:1.12.0")   // XML テーマ (Theme.Material3.*) 用
    implementation("com.googlecode.libphonenumber:libphonenumber:8.13.42")

    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")

    debugImplementation(composeBom)
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
