package com.endeavorstreaming.plugin

import RenameDependency
import RenamePackagePluginExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleVersionSelector


internal fun Project.configureModifyDependencies(extension: RenamePackagePluginExtension) {
    afterEvaluate {
        val renamedDependencies = extension.renamedDependencies
        val unchangedConfigurations = setOf(
            "implementationEmbed",
            "embed",
            "releaseEmbed",
            "debugEmbed",
        )
        configurations
            .filter { !unchangedConfigurations.contains(it.name) }
            .forEach { configuration ->
                configuration.resolutionStrategy.eachDependency {
                    renamedDependencies
                        .firstOrNull { it.isMatched(requested) }
                        ?.let {
                            val version = requested.version
                            if (version != null && !version.endsWith(it.renamedVersionSuffix)) {
                                useVersion("${version}${it.renamedVersionSuffix}")
                            }
                        }
                }
            }
    }
}

private fun RenameDependency.isMatched(requested: ModuleVersionSelector): Boolean {
    if (requested.group == group) {
        if (requested.name == name) {
            return true
        }
        val namePrefix = namePrefix
        if (namePrefix != null && requested.name.startsWith(namePrefix)) {
            return true
        }
    }

    return false
}
