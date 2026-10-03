// Top-level build file where you can add configuration options common to all sub-projects/modules.
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.jvm) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.firebase) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

val nodeVersion = versionCatalogs
    .named("libs")
    .findVersion("node")
    .get()
    .requiredVersion
val nodeArtifactDir = layout.projectDirectory
    .dir("libs/network/mysterium/provider-mobile-node/$nodeVersion")
    .asFile
val nodeAarFile = File(nodeArtifactDir, "provider-mobile-node-$nodeVersion.aar")
val nodePomFile = File(nodeArtifactDir, "provider-mobile-node-$nodeVersion.pom")

val downloadProviderMobileNode by tasks.registering {
    group = "build setup"
    description = "Downloads provider-mobile-node AAR + POM from the GitHub release if missing."

    inputs.property("version", nodeVersion)
    outputs.files(nodeAarFile, nodePomFile)
    outputs.upToDateWhen { nodeAarFile.exists() && nodePomFile.exists() }

    doLast {
        nodeArtifactDir.mkdirs()
        val baseUrl = "https://github.com/mysteriumnetwork/node/releases/download/$nodeVersion"
        listOf(nodeAarFile, nodePomFile).forEach { f ->
            if (f.exists()) return@forEach
            val url = "$baseUrl/${f.name}"
            logger.lifecycle("Downloading ${f.name} from $url")
            val tmp = File(f.parentFile, "${f.name}.tmp")
            try {
                val conn = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30_000
                    readTimeout = 60_000
                    instanceFollowRedirects = true
                }
                if (conn.responseCode !in 200..299) {
                    throw GradleException("Failed to download $url: HTTP ${conn.responseCode} ${conn.responseMessage}")
                }
                conn.inputStream.use { input ->
                    tmp.outputStream().use { output -> input.copyTo(output) }
                }
                Files.move(
                    tmp.toPath(), f.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING
                )
            } finally {
                tmp.delete()
            }
        }
    }
}

// provider-mobile-node is resolved from the local libs/ repo, and ANY task in :app / :node
// can end up resolving it (e.g. :app:mergeReleaseNativeLibs), not just preBuild. If such a
// task runs in parallel while the AAR is still downloading, Gradle fails with
// "Could not find network.mysterium:provider-mobile-node". Make every subproject task
// wait for the download so the files always exist before any resolution happens.
subprojects {
    tasks.configureEach {
        dependsOn(rootProject.tasks.named("downloadProviderMobileNode"))
    }
}
