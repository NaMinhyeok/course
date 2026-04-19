dependencies {
    implementation(project(":core:core-enum"))
    implementation(project(":storage:db-core"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-h2console")
    implementation("tools.jackson.module:jackson-module-kotlin")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
}

tasks.named("bootJar") { enabled = true }
tasks.named("jar") { enabled = false }
