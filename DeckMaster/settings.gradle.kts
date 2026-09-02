// Detect ARM aapt2 on Termux and override the x86_64 Maven binary
// before AGP has a chance to download and try to execute it.
run {
    val candidates = mutableListOf<String>()
    System.getenv("ANDROID_HOME")?.let {
        candidates.add("$it/build-tools/36.0.0/aapt2")
    }
    candidates.add("${System.getProperty("user.home")}/android-sdk/build-tools/36.0.0/aapt2")
    // Termux pkg install aapt2 — native ARM binary
    candidates.add("/data/data/com.termux/files/usr/bin/aapt2")
    for (path in candidates) {
        val f = File(path)
        if (f.exists() && f.canExecute()) {
            System.setProperty("android.aapt2FromMavenOverride", f.absolutePath)
            println("[DeckMaster] Using ARM aapt2 override: ${f.absolutePath}")
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
