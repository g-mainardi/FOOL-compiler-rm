plugins {
    scala
    application
    id("antlr")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.scala-lang:scala3-library_3:3.3.3")
    implementation("org.antlr:antlr4-runtime:4.13.1")
    antlr("org.antlr:antlr4:4.13.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

application {
    mainClass.set("compiler.Test")
}

// --- ANTLR CONFIGURATION ---
tasks.generateGrammarSource {
    maxHeapSize = "64m"
    arguments = arguments + listOf("-visitor", "-no-listener")
}

// --- SOURCES CONFIGURATION ---
sourceSets {
    main {
        // 'java' as extension, not convention
        java {
            // srcDir is additive (it does not clear other paths)
            srcDir("build/generated/sources/antlr/main")
        }
    }
}

// --- COMPILE CONFIGURATION ---
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // To ensure ANTLR runs before Java
    dependsOn(tasks.generateGrammarSource)
}

tasks.withType<ScalaCompile>().configureEach {
    scalaCompileOptions.additionalParameters = listOf("-encoding", "UTF-8")
}

tasks.run<JavaExec> {
    args("foolExamples/prova.fool")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}