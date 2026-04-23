dependencies {
    implementation(project(":core:core-enum"))
    implementation(project(":storage:db-core"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-h2console")
    implementation("tools.jackson.module:jackson-module-kotlin")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation(libs.awaitility)
}

tasks.named("bootJar") { enabled = true }
tasks.named("jar") { enabled = false }
