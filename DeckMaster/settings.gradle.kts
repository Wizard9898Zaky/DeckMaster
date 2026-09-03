// Detect a native aapt2 on Termux and override the x86_64 Maven binary
// before AGP has a chance to download and try to execute it.
// Priority: Termux pkg (guaranteed device-arch) > SDK build-tools
run {
    val candidates = mutableListOf<String>()
    // Termux pkg install aapt2 — compiled for the device's exact CPU (32-bit or 64-bit ARM)
    candidates.add("/data/data/com.termux/files/usr/bin/aapt2")
    // SDK build-tools aapt2 — may be wrong arch on 32-bit devices
    System.getenv("ANDROID_HOME")?.let {
        candidates.add("$it/build-tools/36.0.0/aapt2")
    }
    candidates.add("${System.getProperty("user.home")}/android-sdk/build-tools/36.0.0/aapt2")
    for (path in candidates) {
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
