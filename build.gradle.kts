import io.papermc.paperweight.userdev.ReobfArtifactConfiguration

plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    id("com.gradleup.shadow") version "9.5.1"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = "com.mingyu.pillage"
version = "1.0.8"

repositories {
    mavenCentral()
}

dependencies {
    // Real hosting providers (incl. the FeatherMC test server) run 26.1.2 stable -
    // 26.2 is still an alpha/snapshot cycle with no real deployable server yet.
    paperweight.paperDevBundle("26.1.2.build.+")

    implementation("org.xerial:sqlite-jdbc:3.47.1.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(25)
    }

    processResources {
        filteringCharset = "UTF-8"
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveClassifier.set("")
        // NOTE: do NOT relocate org.sqlite - sqlite-jdbc ships a JNI native library whose
        // exported symbols are bound to the org.sqlite.* class names. Renaming the Java
        // package breaks native method binding (UnsatisfiedLinkError on NativeDB._open_utf8)
        // even though the Java bytecode itself compiles and shades fine.
        mergeServiceFiles()
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    assemble {
        dependsOn(shadowJar)
    }

    reobfJar {
        inputJar.set(shadowJar.flatMap { it.archiveFile })
    }
}

// Recent Paper dev-bundles (including this 26.1.2 stable one) no longer ship Spigot/reobf
// mappings at all - Mojang-mapped-only is the only option, which is fine since the plugin
// only touches the stable public Bukkit/Paper API surface (no raw NMS calls).
paperweight.reobfArtifactConfiguration.set(ReobfArtifactConfiguration.MOJANG_PRODUCTION)
