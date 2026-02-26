# AGENTS.md

## Cursor Cloud specific instructions

### Project Overview

DJST (Dynamic Joint Sentiment-Topic Model) — a Java/Maven research project implementing the dJST model from "Dynamic Joint Sentiment-Topic Model" (He et al., 2012). It performs online Gibbs sampling to jointly discover sentiment labels and topics from time-stamped document corpora.

### Build & Test

- **Build**: `mvn compile -Dmaven.compiler.source=8 -Dmaven.compiler.target=8`
  - OpenJDK 21 requires explicit source/target 8+ (pom.xml defaults to 1.5).
- **Test**: `mvn test -Dmaven.compiler.source=8 -Dmaven.compiler.target=8`

### Running the Application

1. Entry point: `edu.nuaa.yao.DJST.Main` reads config from `./cfg/olda.properties` relative to CWD.
2. The source config at `src/main/java/cfg/olda.properties` uses Windows path separators (`\\`). For Linux, create `/workspace/cfg/olda.properties` with forward slashes (e.g., `dir ./models/olda`).
3. Corpus data: `{dir}/{dfile}/{N}.txt` — one file per time slice, each line is a document (space-separated words).
4. Run: `java -cp target/classes edu.nuaa.yao.DJST.Main`

### Key Architecture (dJST Paper)

Array index conventions:
- `nw[V][K][S]` — word × topic × sentiment count
- `nd[M][K][S]` — doc × topic × sentiment count
- `phi[S][K][V]` — sentiment × topic × word distribution
- `theta[M][S][K]` — doc × sentiment × topic distribution

Gibbs sampling jointly samples (topic, sentiment) for each word token using Eq 6 from the paper.
