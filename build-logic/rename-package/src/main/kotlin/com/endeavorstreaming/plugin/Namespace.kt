package com.endeavorstreaming.plugin

import RenamePackagePluginExtension
import com.android.build.gradle.LibraryExtension


internal fun configureRenameNamespace(
    libraryExtension: LibraryExtension,
    extension: RenamePackagePluginExtension,
) {
    val namespace = libraryExtension.namespace
    val from = extension.renamePackageFrom
    if (!namespace.isNullOrEmpty() && namespace.startsWith(from)) {
        libraryExtension.namespace = namespace.replace(from, extension.renamePackageTo)
    }
}