rootProject.name = "AndroidSMSGateway"

include("app")

includeBuild("src/mobile/android") {
    name = "androidApp"
}
