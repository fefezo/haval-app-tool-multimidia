package br.com.redesurftank.havalshisuku.models

data class AppInfo(
    val name: String,
    val version: String,
    val packageName: String,
    val link: String,
    val iconUrl: String? = null,
    // Ícone e APK embutidos no próprio Impulse (res/drawable e res/raw).
    // Quando `bundledResourceId` não é nulo, a instalação não usa rede: o APK
    // é copiado de dentro do Impulse. Ver InstallAppsTab.
    val iconResId: Int? = null,
    val bundledResourceId: Int? = null
)