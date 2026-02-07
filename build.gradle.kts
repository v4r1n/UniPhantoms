import xyz.srnyx.gradlegalaxy.data.config.DependencyConfig
import xyz.srnyx.gradlegalaxy.data.config.JavaSetupConfig
import xyz.srnyx.gradlegalaxy.enums.Repository
import xyz.srnyx.gradlegalaxy.enums.repository
import xyz.srnyx.gradlegalaxy.utility.setupAnnoyingAPI
import xyz.srnyx.gradlegalaxy.utility.spigotAPI


plugins {
    java
    id("xyz.srnyx.gradle-galaxy") version "2.0.2"
    id("com.gradleup.shadow") version "8.3.9"
}

spigotAPI(config = DependencyConfig(version = "1.21"))
setupAnnoyingAPI(
    javaSetupConfig = JavaSetupConfig(
        group = "xyz.srnyx",
        version = "2.2.2",
        description = "Plugin used for per-player phantom spawning/control"),
    annoyingAPIConfig = DependencyConfig(version = "59aeeb28a9"))

repository(Repository.PLACEHOLDER_API)
repositories.mavenCentral()

dependencies {
    // PlaceholderAPI integration
    compileOnly("me.clip", "placeholderapi", "2.11.7")

    // Adventure API (MiniMessage support)
    implementation("net.kyori:adventure-text-minimessage:4.26.1")
    implementation("net.kyori:adventure-platform-bukkit:4.4.1")
}
