tasks.register("test") {
    group = "verification"
    description = "Runs the nested Android app's unit tests."
    dependsOn(gradle.includedBuild("androidApp").task(":app:test"))
}

tasks.register("assembleDebug") {
    group = "build"
    description = "Builds the nested Android app debug APK."
    dependsOn(gradle.includedBuild("androidApp").task(":app:assembleDebug"))
}

tasks.register("assembleRelease") {
    group = "build"
    description = "Builds the nested Android app release APK."
    dependsOn(gradle.includedBuild("androidApp").task(":app:assembleRelease"))
}

tasks.register("lint") {
    group = "verification"
    description = "Runs lint for the nested Android app."
    dependsOn(gradle.includedBuild("androidApp").task(":app:lint"))
}
