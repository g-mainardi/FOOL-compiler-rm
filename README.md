# FOOL Compiler

This is a simple compiler for the FOOL(Functional Object-Oriented Language) programming language, implemented in Java using ANTLR for parsing. 
The compiler performs lexical analysis, parsing, semantic analysis, and code generation.
The java code was provided by the course "Linguaggi, Compilatori e Modelli Computazionali" held at the University of Bologna.

## Build
From the project root you can build using the Gradle wrapper:

```bash
  ./gradlew build
```


## Run / Compile a source file
You can directly run the application and pass the file to compile as a command-line argument (optional).

_Example_ without argument (will use default file `foolExamples/prova.fool`):

```bash
  ./gradlew run
```

_Example_ with argument:

```bash
  ./gradlew run --args="path/to/source.fool"
```

### Note 
- replace `path/to/source.fool` with the actual file you want to compile. 
- the file must be in the project directory.
- if your shell interprets backslashes or special characters, quote or escape the path as appropriate.
