plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.treasurehunt.buah"
    compileSdk = 35
    buildToolsVersion = "35.0.0"
    defaultConfig { applicationId = "com.treasurehunt.buah"; minSdk = 24; targetSdk = 35; versionCode = 4; versionName = "1.3" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            val testHome = gradle.gradleUserHomeDir.resolve("robolectric-home")
            testHome.mkdirs()
            it.systemProperty("user.home", testHome.absolutePath)
            it.systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
            // JVM test dependencies honour the same proxy route as the Gradle invocation.
            listOf("http.proxyHost", "http.proxyPort", "https.proxyHost", "https.proxyPort").forEach { name ->
                System.getProperty(name)?.let { value -> it.systemProperty(name, value) }
            }
        }
    }
}
dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
}
