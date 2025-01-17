import com.android.build.gradle.LibraryPlugin
import com.endeavorstreaming.plugin.configureCopyAndRenameLint
import com.endeavorstreaming.plugin.configureCopyAndRenameSource
import com.endeavorstreaming.plugin.configureLintBaseline
import com.endeavorstreaming.plugin.configureModifyDependencies
import com.endeavorstreaming.plugin.configureRenameNamespace
import com.endeavorstreaming.plugin.configureUseRenamedSource
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

class RenamePackagePlugin : Plugin<Project> {
    companion object {
        const val RENAME_PACKAGE_NAME = "packageRenaming"
    }

    override fun apply(target: Project) {
        val extension = target.extensions.create(
            RENAME_PACKAGE_NAME,
            RenamePackagePluginExtension::class.java
        )
        with(target) {
            plugins.withType(LibraryPlugin::class.java) {
                configureCopyAndRenameSource(extensions.getByType(), extension)
                configureCopyAndRenameLint(extensions.getByType(), extension)
                configureModifyDependencies(extension)
                configureRenameNamespace(extensions.getByType(), extension)
                configureLintBaseline(extensions.getByType(), extension)
                configureUseRenamedSource(extensions.getByType(), extension)
            }
        }
    }
}
