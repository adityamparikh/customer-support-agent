# 0005. Error Prone with `-Werror`, and palantir-java-format through Spotless

- Status: Accepted 2026-11-20
- Code: `pom.xml` (`maven-compiler-plugin`, `spotless-maven-plugin`), `.mvn/jvm.config`

## Context

The build had `javac` and nothing else: no linter, no formatter. An agent could not tell a
warning it introduced from one that was already there, and its diffs could churn on style.

## Decision

- Error Prone runs as a javac plugin on main and test sources, with `-Xlint:all,-processing,-serial`
  and `-Werror`, so every Error Prone or lint warning fails compilation. The baseline is zero.
- Spotless checks palantir-java-format (plus unused imports) in the `verify` phase;
  `./mvnw spotless:apply` rewrites files. palantir rather than google-java-format for its
  120-column lines, which suit the long LangChain4j builder chains.
- `.mvn/jvm.config` opens the `jdk.compiler` packages Error Prone and the formatter need on JDK 25.

## Consequences

- `./mvnw compile` can fail on code that javac alone accepts (for example `LocalDate.now()`
  without a zone, `String.split`, double-brace initialisation). Fix the finding; suppress a check
  only with `@SuppressWarnings("CheckName")` and a comment saying why.
- `./mvnw install` fails on unformatted code. Run `./mvnw spotless:apply` before committing.
- Upgrading the JDK can need new `--add-exports` lines or a newer Error Prone.
