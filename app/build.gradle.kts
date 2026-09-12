import org.jetbrains.kotlin.gradle.dsl.JvmTarget
// Explícitos: `java.` no topo de um .kts resolve para a extensão `java` do Gradle,
// não para o pacote — `java.text.SimpleDateFormat` ali não compila.
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// --- Identificação do build, mostrada na tela de Informações ---
// A versão NÃO é derivada de tag de git de propósito: os tags v2.x das releases
// vivem só no remoto (a API do GitHub os cria), então um `git describe` local
// devolveria um tag velho do upstream — número errado, e errado em silêncio.
// `versionName` é explícito e sobe junto com cada release; data e commit são
// injetados aqui e não têm como mentir.
// Qualquer falha (git ausente) devolve string vazia e o build segue.
// `dir` vem por parâmetro de propósito: uma função de topo num .kts não enxerga o
// receptor implícito do script, então `rootDir` só resolve no ponto de chamada.
fun git(dir: File, vararg args: String): String = try {
    val process = ProcessBuilder(listOf("git") + args)
        .directory(dir)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().use { it.readText().trim() }
    if (process.waitFor() == 0) output else ""
} catch (e: Exception) {
    ""
}

val buildDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date())
val buildCommit = git(rootDir, "rev-parse", "--short=7", "HEAD")

android {
    namespace = "br.com.redesurftank.havalshisuku"
    compileSdk = 36

    defaultConfig {
        applicationId = "br.com.redesurftank.havalshisuku"
        minSdk = 28
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 28
        versionCode = 99
        // Forma com aspas mantida de propósito: o sed do build.yml casa em
        // `versionName = ".*"` e continua funcionando se o CI um dia rodar.
        // NÃO mudar o versionCode: é 99 em todas as releases desde a v2.0, e é
        // isso que faz o rollback ser uma reinstalação de mesmo versionCode,
        // que o Android aceita sem `-d`.
        versionName = "2.9"

        buildConfigField("String", "BUILD_DATE", "\"$buildDate\"")
        buildConfigField("String", "BUILD_COMMIT", "\"$buildCommit\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
        }
    }

    buildTypes {
        named("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources  = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        aidl = true
        compose = true
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(11)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.shizuku)
    implementation(libs.shizuku.provider)
    implementation(libs.hiddenapibypass)
    implementation(libs.commons.net)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.gson)
    implementation(libs.coil.compose)
    implementation(libs.material.icons.extended)
    annotationProcessor(libs.annotation.processor)
    compileOnly(libs.annotation)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}