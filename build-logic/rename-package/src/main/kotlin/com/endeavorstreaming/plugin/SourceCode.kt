package com.endeavorstreaming.plugin

import RenamePackagePluginExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import java.io.File

private const val RENAMED_SRC_ROOT = "src/main"
internal const val RENAMED_JAVA_DIR = "$RENAMED_SRC_ROOT/java"
private const val RENAMED_RES_DIR = "$RENAMED_SRC_ROOT/res"
private const val RENAMED_AIDL_DIR = "$RENAMED_SRC_ROOT/aidl"
private const val RENAMED_JNI_DIR = "$RENAMED_SRC_ROOT/jni"
private const val RENAMED_MANIFEST_PATH = "$RENAMED_SRC_ROOT/AndroidManifest.xml"

internal fun Project.configureCopyAndRenameSource(
    libraryExtension: LibraryExtension,
    extension: RenamePackagePluginExtension
) {
    val renamedRootDir = extension.getRenamedRootDir(this)
    val cleanRenamedDir = tasks.register("cleanRenamedDir", Delete::class.java) {
        delete(File("$renamedRootDir/"))
    }

    val from = extension.renamePackageFrom
    val fromPath = from.replace(".", "/")
    val fromJniPath = from.replace(".", "_")

    val to = extension.renamePackageTo
    val toPath = to.replace(".", "/")
    val toJniPath = to.replace(".", "_")

    val mainSourceSet = libraryExtension.sourceSets.getByName("main")
    val javaDirs = mainSourceSet.java.srcDirs
    val resDirs = mainSourceSet.res.srcDirs
    val aidlDirs = mainSourceSet.aidl.srcDirs
    val jniDirs = mainSourceSet.jniLibs.srcDirs
    val manifestPath = mainSourceSet.manifest.srcFile

    val copyFromPathJava = tasks.register("copyFromPathAndRenameJava", Copy::class.java) {
        from(javaDirs.map { File(it, fromPath).path })
        into(File("$renamedRootDir/$RENAMED_JAVA_DIR/$toPath"))
        filter { it.renameMedia3Source(from, to) }
    }
    val copyNormalJava = tasks.register("copyAndRenameNormalJava", Copy::class.java) {
        from(javaDirs.map { File(it, "com").path })
        into(File("$renamedRootDir/$RENAMED_JAVA_DIR/com"))
        filter { line ->
            line.renameImport(from, to)
                .renameForActionsAndReflection(from, to)
        }
    }

    val copyMediaRes = tasks.register("copyMediaRes", Copy::class.java) {
        from(resDirs.map { it.path })
        into(File("$renamedRootDir/$RENAMED_RES_DIR"))
        exclude("**/*.xml")
    }
    val copyXmlRes = tasks.register("copyAndRenameXmlRes", Copy::class.java) {
        from(resDirs.map { it.path })
        into(File("$renamedRootDir/$RENAMED_RES_DIR"))
        include("**/*.xml")
        filter { line -> line.renameXml(from, to) }
    }

    val copyFromPathAidl = tasks.register("copyFromPathAndRenameAidl", Copy::class.java) {
        from(aidlDirs.map { File(it, fromPath).path })
        into(File("$renamedRootDir/$RENAMED_AIDL_DIR/$toPath"))
        filter { it.renameMedia3Source(from, to) }
    }
    val copyNormalAidl = tasks.register("copyAndRenameNormalAidl", Copy::class.java) {
        from(aidlDirs.map { File(it, "com").path })
        into(File("$renamedRootDir/$RENAMED_AIDL_DIR/$toPath"))
        filter { line ->
            line.renameImport(from, to)
                .renameForActionsAndReflection(from, to)
        }
    }

    val copyJni = tasks.register("copyAndRenameJni", Copy::class.java) {
        from(jniDirs.map { it.path })
        into("$renamedRootDir/$RENAMED_JNI_DIR")
        filter { line ->
            line.replace("\"$fromPath/", "\"$toPath/")
                .replace("Java_$fromJniPath", "Java_$toJniPath")
        }
    }

    val copyManifest = tasks.register("copyAndRenameManifest", Copy::class.java) {
        from(manifestPath.path)
        into(File("$renamedRootDir/$RENAMED_SRC_ROOT"))
        filter { line -> line.renameXml(from, to) }
    }

    val copyAndRename = tasks.register("copyAndRename", DefaultTask::class.java) {
        dependsOn(
            cleanRenamedDir,
            copyFromPathJava,
            copyNormalJava,
            copyMediaRes,
            copyXmlRes,
            copyManifest,
            copyFromPathAidl,
            copyNormalAidl,
            copyJni,
        )
    }

    afterEvaluate {
        tasks.findByName("releaseSourcesJar")?.dependsOn(copyAndRename)
    }
    tasks.getByName("preBuild").dependsOn(copyAndRename)
}

internal fun Project.configureUseRenamedSource(
    libraryExtension: LibraryExtension,
    extension: RenamePackagePluginExtension
) {
    val renamedRootDir = extension.getRenamedRootDir(this)
    libraryExtension.sourceSets.getByName("main").apply {
        java.setSrcDirs(listOf("${renamedRootDir}/$RENAMED_JAVA_DIR"))
        res.setSrcDirs(listOf("${renamedRootDir}/$RENAMED_RES_DIR"))
        manifest.srcFile("${renamedRootDir}/$RENAMED_MANIFEST_PATH")
        aidl.setSrcDirs(listOf("${renamedRootDir}/$RENAMED_AIDL_DIR"))
        jniLibs.setSrcDirs(listOf("${renamedRootDir}/$RENAMED_JNI_DIR"))
    }
}

private fun String.renameMedia3Source(from: String, to: String): String {
    return renamePackage(from, to)
        .renameImport(from, to)
        .renameForActionsAndReflection(from, to)
}

private fun String.renamePackage(from: String, to: String): String {
    return replace(
        regex = "^\\s*package\\s+".appendNormalString(from).toRegex(),
        replacement = "package $to"
    )
}

private fun String.renameImport(from: String, to: String): String {
    return replace(
        // rename import
        regex = "^\\s*import\\s+".appendNormalString(from).toRegex(),
        replacement = "import $to"
    ).replace(
        // rename static import
        regex = "^\\s*import\\s+static\\s+".appendNormalString(from).toRegex(),
        replacement = "import static $to"
    ).replace(
        // rename full name class's package
        regex = "([\\s;()\\[\\]{}:])".appendNormalString(from).toRegex(),
        replacement = "$1${to}"
    )
}

private fun String.appendNormalString(normalText: String): String {
    val regexBuilder = StringBuilder(this)
    val normalTextBuilder = StringBuilder()
    normalText.forEach { char ->
        if (isRegexSpecialChar(char)) {
            normalTextBuilder.append("\\")
        }
        normalTextBuilder.append(char)
    }
    regexBuilder.append(normalTextBuilder.toString())
    return regexBuilder.toString()
}

private fun isRegexSpecialChar(char: Char): Boolean {
    val specialChars = "\\.*+?^$|{}()[]"
    return specialChars.contains(char)
}

private fun String.renameForActionsAndReflection(from: String, to: String) = renameString(from, to)

private fun String.renameXml(from: String, to: String): String {
    return renameString(from, to)
        // <androidx.media3.   =>    <es.androidx.media3.
        .replace("<$from", "<$to")
        .replace("</$from", "</$to")
}

private fun String.renameString(from: String, to: String): String {
    // "androidx.media3.   =>    "es.androidx.media3.
    return replace("\"" + from, "\"" + to)
}
