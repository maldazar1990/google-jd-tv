plugins {
    id("com.android.application") version "8.3.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
}

// Redirect build artifacts outside OneDrive to eliminate Windows file-locking conflicts
allprojects {
    val branch = try {
        providers.exec {
            commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
        }.standardOutput.asText.get().trim().replace('/', '_')
    } catch (_: Exception) {
        "default"
    }
    val tempBuild = file("C:/Users/jdgen/AppData/Local/Temp/tv_build_${branch}/${project.name}")
    layout.buildDirectory.set(tempBuild)
}
