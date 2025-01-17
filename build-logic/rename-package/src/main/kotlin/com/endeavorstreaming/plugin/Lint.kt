package com.endeavorstreaming.plugin

import RenamePackagePluginExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import java.io.File

internal fun Project.configureCopyAndRenameLint(
    libraryExtension: LibraryExtension,
    extension: RenamePackagePluginExtension,
) {
    val lintBaseLine = file("lint-baseline.xml")
    if (lintBaseLine.exists()) {
        val renamedRootDir = extension.getRenamedRootDir(this)
        val javaDirs = libraryExtension.sourceSets.getByName("main").java.srcDirs
        val fromPath = extension.renamePackageFrom.replace(".", "/")
        val toPath = extension.renamePackageTo.replace(".", "/")

        val copyLint = tasks.register("copyLint", Copy::class.java) {
            from(lintBaseLine)
            into(File("$renamedRootDir/"))
            val fromPaths = javaDirs.map { File(it, fromPath) }.map { it.path }
            filter { line ->
                var replacedLine = line
                fromPaths.forEach { fromPath ->
                    replacedLine = replacedLine.replace(
                        fromPath,
                        "$renamedRootDir/$RENAMED_JAVA_DIR/$toPath/"
                    )
                }
                replacedLine
            }
        }
        tasks.getByName("preBuild").dependsOn(copyLint)
    }
}

internal fun Project.configureLintBaseline(
    libraryExtension: LibraryExtension,
    extension: RenamePackagePluginExtension,
) {
    val lintBaselineFile = file("lint-baseline.xml")
    if (lintBaselineFile.exists()) {
        val renamedRootDir = extension.getRenamedRootDir(this)
        libraryExtension.lint.baseline = file("$renamedRootDir/lint-baseline.xml")
    }
}
