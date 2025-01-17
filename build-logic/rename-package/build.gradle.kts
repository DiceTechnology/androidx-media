plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly("com.android.tools.build:gradle:8.2.2")
}

gradlePlugin {
    plugins {
        register("renamePackage") {
            id = "endeavorstreaming.renaming.package"
            implementationClass = "RenamePackagePlugin"
        }
    }
}
