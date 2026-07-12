tasks.register("test") {
    group = "verification"
    description = "Runs the nested Android project's unit tests."
    dependsOn(gradle.includedBuild("androidApp").task(":app:test"))
}

tasks.register("assembleDebug") {
    group = "build"
    description = "Builds the nested Android project's debug APK."
    dependsOn(gradle.includedBuild("androidApp").task(":app:assembleDebug"))
}

tasks.register("assembleRelease") {
    group = "build"
    description = "Builds the nested Android project's release APK."
    dependsOn(gradle.includedBuild("androidApp").task(":app:assembleRelease"))
}

tasks.register("lint") {
    group = "verification"
    description = "Runs lint for the nested Android project."
    dependsOn(gradle.includedBuild("androidApp").task(":app:lint"))
}
