plugins { java }
group = "dev.kokage"
version = "0.1.0"
repositories { maven("https://repo.papermc.io/repository/maven-public/") }
dependencies { compileOnly("io.papermc.paper:paper-api:26.2.build.133-stable") }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(26)) }
tasks.withType<JavaCompile> { options.encoding = "UTF-8" }
