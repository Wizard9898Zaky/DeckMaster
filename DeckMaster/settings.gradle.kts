// Auto-detect Android SDK and native aapt2 on Termux.
// This makes ./gradlew assembleRelease work from any shell session
// without needing ANDROID_HOME or local.properties to be pre-set.
run {
    val home = System.getProperty("user.home")

    // --- SDK detection ---
    val sdkCandidates = mutableListOf<String>()
    System.getenv("ANDROID_HOME")?.let { sdkCandidates.add(it) }
    sdkCandidates.add("$home/android-sdk")
    for (path in sdkCandidates) {
        val f = File(path)
        if (f.exists() && f.isDirectory) {
            // Write local.properties so AGP finds the SDK
            val lp = File(settingsDir, "local.properties")
            if (!lp.exists() || !lp.readText().contains("sdk.dir=")) {
                lp.writeText("sdk.dir=${f.absolutePath}\n")
                println("[DeckMaster] Wrote local.properties: sdk.dir=${f.absolutePath}")
            }
            break
        }
    }

    // --- aapt2 detection (Termux pkg first, SDK build-tools fallback) ---
    val aapt2Candidates = mutableListOf<String>()
    aapt2Candidates.add("/data/data/com.termux/files/usr/bin/aapt2")
    System.getenv("ANDROID_HOME")?.let { aapt2Candidates.add("$it/build-tools/36.0.0/aapt2") }
    aapt2Candidates.add("$home/android-sdk/build-tools/36.0.0/aapt2")
    for (path in aapt2Candidates) {
        val f = File(path)
        if (f.exists() && f.canExecute()) {
            System.setProperty("android.aapt2FromMavenOverride", f.absolutePath)
            println("[DeckMaster] Using native aapt2 override: ${f.absolutePath}")
            break
        }
    }
}

pluginManagement {
    repositories {
        google()
        maven { url = uri("https://repo1.maven.org/maven2/") }
        maven { url = uri("https://maven.aliyun.com/repository/public/") }
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven { url = uri("https://repo1.maven.org/maven2/") }
        maven { url = uri("https://maven.aliyun.com/repository/public/") }
    }
}
rootProject.name = "DeckMaster"
include(":app")
