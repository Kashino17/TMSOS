plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}

// The project lives in an iCloud-synced Desktop folder. iCloud creates "name 2"
// conflict copies inside build outputs, which breaks resource merging.
// Folders ending in ".nosync" are skipped by iCloud.
allprojects {
    layout.buildDirectory.set(layout.projectDirectory.dir("build.nosync"))
}
