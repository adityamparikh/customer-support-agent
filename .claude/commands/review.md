---
description: Review the current diff against this repo's rules and traps
argument-hint: "[base ref, default main]"
---

Base ref: "$ARGUMENTS" (use `main` if that is empty). Review the changes on this branch against it:
run `git diff <base>...HEAD` and `git status --short` (include unstaged and untracked files), then read
every changed file in full.

Report findings ordered by severity, each with `path:line`, what is wrong and the fix. Check these
first, because they are the mistakes this repo makes easily:

1. **Paid calls in the default build.** Any new test that needs `OPENAI_API_KEY` or
   `ANTHROPIC_API_KEY` must be a `*IT` with `@EnabledIfSystemProperty(named = "live-it", matches = "true")`.
   A `*Tests` class must not reach OpenAI or Anthropic: it must keep `@TestBean` replacements for
   `openAiChatModel` and the `EmbeddingModel` (see `CustomerSupportAgentWiringTests`).
2. **Model-facing text.** Changes to the `@SystemMessage` in `CustomerSupportAgent`, to `@Tool`/`@P`
   descriptions in `BookingTools`, or to `langchain4j.rag.*` / chunking in `RagConfig` change agent
   behaviour. Check they match `docs/specs/0002-booking-cancellation-flow.md`, that `BookingToolsTests`
   and the wiring test were updated, and that the PR says which `-Plive-it` test was run.
3. **Judge conditions** in `CustomerSupportAgentIT` must be answerable from the response text alone;
   the judge never sees the user's message.
4. **Explicit wiring.** Bean names in `@AiService(...)` must match real beans (`openAiChatModel`,
   `chatMemoryProvider`, `contentRetriever`, `bookingTools`); a new tool class must be listed in `tools`.
5. **Booking rules.** A wrong name must give the same `BookingNotFoundException` as a wrong number;
   `cancelBooking` must refuse a booking that has started.
6. **Secrets.** No `.env`, `.run/`, `*.pem`, `*.key` or key-shaped string staged.
7. **Suppressions.** Any new `@SuppressWarnings` needs a comment saying why.
8. **Docs.** If commands, layout or behaviour changed, `CLAUDE.md`, `README.md` and the spec or ADR
   should change too.

Then run `./mvnw install` (no paid calls) and report the result. Do not run `-Plive-it`; if the change
needs it, say which test and why. End with a verdict: ready, ready with nits, or changes needed.
