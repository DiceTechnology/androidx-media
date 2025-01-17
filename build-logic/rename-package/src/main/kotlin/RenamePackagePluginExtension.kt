import org.gradle.api.Project

class RenameDependency(
    var group: String,
    var name: String? = null,
    var namePrefix: String? = null,
    var renamedVersionSuffix: String,
)

abstract class RenamePackagePluginExtension {
    var renameRoot: String = "renamed"

    var renamePackageFrom: String = "androidx.media3."
    var renamePackageTo: String = "es.androidx.media3."
    var renamedVersionSuffix: String = "-renamed"

    var renamedDependencies: Set<RenameDependency> = setOf(
        RenameDependency(
            group = "com.endeavorstreaming.androidx-media",
            namePrefix = "media3-",
            renamedVersionSuffix = "-renamed"
        ),
        RenameDependency(
            group = "com.endeavorstreaming.shield",
            name = "shield-android",
            renamedVersionSuffix = "-renamed"
        ),
    )

    internal fun getRenamedRootDir(project: Project): String {
        val buildDir = project.layout.buildDirectory.asFile.get()
        return buildDir.name + "/$renameRoot"
    }
}