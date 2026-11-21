---
description: Run one or all paid live integration tests, after confirming the spend
argument-hint: "[TestMethod, e.g. should_cancel_booking; empty = whole suite]"
---

This spends OpenAI (GPT-5.4, embeddings) and Anthropic (Claude Haiku judge) credit.

1. Work out the command:
   - With an argument: `./mvnw verify -Plive-it -Dit.test="CustomerSupportAgentIT#$ARGUMENTS"`
   - Without: `./mvnw verify -Plive-it` (all ten tests, about ten agent conversations plus judge calls)
2. Check `OPENAI_API_KEY` and `ANTHROPIC_API_KEY` are set without printing them
   (`[ -n "$OPENAI_API_KEY" ] && echo set`). If not, tell the user to load `.env`
   (`set -a; . ./.env; set +a`) and stop.
3. Show the user the exact command and which tests it runs, and ask for a yes. Do not run it without one.
4. Run it. For each failure report the test, the assertion message from
   `target/failsafe-reports/*.txt` and, for judge failures, the judge's reasoning.
   A 401 from Anthropic or OpenAI is a key problem, not a test failure.
5. Live results vary. Before proposing a prompt or tool-description change for a failure,
   suggest rerunning that single test once. If a spec has an open question about this test
   (for example `docs/specs/0002-booking-cancellation-flow.md`), record the result there.
