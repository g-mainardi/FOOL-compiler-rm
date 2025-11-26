plugins {
    scala
    application
    id("antlr") // Il plugin ufficiale gestisce le convenzioni per noi
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

// --- CONFIGURAZIONE PULITA ---

// Non tocchiamo outputDirectory. Lasciamo che vada in build/generated/sources/antlr/...
// Il plugin Antlr lo fa di default.

tasks.generateGrammarSource {
    maxHeapSize = "64m"
    // Questo è l'unico parametro "obbligatorio" per dire ad ANTLR il package java
    arguments = arguments + listOf("-visitor", "-no-listener")
}

sourceSets {
    main {
        // SCALA: Convenzione standard src/main/scala
        withConvention(ScalaSourceSet::class) {
            scala.srcDirs("src/main/scala")
        }

        // JAVA: Qui sta la magia.
        // NON usiamo setSrcDirs (che cancella i default).
        // Il plugin 'antlr' aggiunge automaticamente la sua cartella di output qui.
        // Noi dobbiamo solo assicurarci che 'src/main/java' sia incluso.
        // Di default Gradle include src/main/java, ma il plugin Scala a volte fa il prepotente.

        // Per sicurezza, facciamo così (ADDITIVO):
        java {
            srcDir("src/main/java")
        }
    }
}

// Configurazione encoding
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // Questo è il segreto: diciamo esplicitamente che la compilazione Java
    // dipende dalla generazione della grammatica.
    dependsOn(tasks.generateGrammarSource)
}

tasks.withType<ScalaCompile>().configureEach {
    scalaCompileOptions.additionalParameters = listOf("-encoding", "UTF-8")
}