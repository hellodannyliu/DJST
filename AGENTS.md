# AGENTS.md

## Cursor Cloud specific instructions

### Project Overview

DJST (Dynamic Joint Sentiment-Topic Model) — a Java/Maven research project implementing online LDA with sentiment analysis using Gibbs sampling. It is a CLI application with no web server or database dependencies.

### Build & Test

- **Build**: `mvn compile -Dmaven.compiler.source=8 -Dmaven.compiler.target=8`
  - OpenJDK 21 does not support source/target level 5 (the pom.xml default). You must pass `-Dmaven.compiler.source=8 -Dmaven.compiler.target=8`.
- **Test**: `mvn test -Dmaven.compiler.source=8 -Dmaven.compiler.target=8 -Dmaven.compiler.failOnError=false`
  - The `-Dmaven.compiler.failOnError=false` flag is needed because the latest commit (`a37b1f1`, "更改初始化中") introduced pre-existing compilation errors in `Model.java`, `OldaTrainer.java`, `Inferencer.java`, and `SimlarityAnalyser.java` (3D array refactoring for sentiment dimension is incomplete). The single JUnit test (`AppTest`) does not depend on those classes and passes.
- **Compile errors**: The HEAD commit has type-mismatch errors across multiple files due to an in-progress refactoring of array dimensions (adding sentiment `S` dimension). Pre-compiled `.class` files from the previous working commit (`bdd36b3`) are tracked in `target/classes/`.

### Running the Application

1. The entry point is `edu.nuaa.yao.DJST.Main`, which reads config from `./cfg/olda.properties` relative to CWD.
2. The original `src/main/java/cfg/olda.properties` uses Windows path separators (`\\`). For Linux, create `/workspace/cfg/olda.properties` with forward slashes (e.g., `dir ./models/olda`).
3. Corpus data goes in `{dir}/{dfile}/{N}.txt` (one file per time slice, e.g., `models/olda/corpus/1.txt`). Each line is a document (space-separated words).
4. Run: `java -cp target/classes edu.nuaa.yao.DJST.Main`
5. Use the pre-compiled class files from `target/classes/` (tracked in git from commit `bdd36b3`) since HEAD does not compile cleanly.

### Key Configuration (`cfg/olda.properties`)

- `niters`: Number of Gibbs sampling iterations (use small values like 20 for quick testing)
- `ntopics`: Number of topics
- `docnum`: Number of time-slice corpus files expected
- `delta`: Time window size for OLDA
- `S`: Number of sentiment labels
