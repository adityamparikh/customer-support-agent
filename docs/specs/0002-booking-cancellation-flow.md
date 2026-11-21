# 0002. Booking cancellation flow

- Status: Implemented; live verification of criteria 1 and 2 pending (see Open questions)
- Date: 2026-11-20
- Related: [ADR 0003](../adr/0003-llm-as-a-judge-claude-haiku.md), `CustomerSupportAgentIT#should_cancel_booking`

## Goal

A customer can cancel a booking in two turns: they ask, the agent checks the booking exists
and asks for confirmation; they confirm, the agent cancels. The agent never cancels without
the confirmation and never reveals or cancels someone else's booking.

## Background: the `should_cancel_booking` failure

The audit's live run failed at `CustomerSupportAgentIT.should_cancel_booking:138` with
"Expected <getBookingDetails> tool to be executed, but no tools were executed at all": on the
first turn GPT-5.4 asked for confirmation without looking the booking up. From the code:

- System message rule 2 said "first make sure it exists" but never said how.
- The tools had bare `@Tool` annotations, so the specification the model received was just the
  names `getBookingDetails`/`cancelBooking` and three parameter names, with no description
  tying the lookup to "make sure it exists".
- `BookingService.cancelBooking` itself checks existence, so a model can reasonably treat the
  lookup as redundant and plan to verify on confirmation.
- The test asserts an exact tool sequence per turn (`onlyToolWasExecuted`), which the old prompt
  implied but did not require. One run of a non-deterministic model is a single sample.

Change: rule 2 now says "make sure it exists by calling getBookingDetails ... Do not call
cancelBooking until the customer has confirmed", and both tools carry `@Tool`/`@P` descriptions
(`BookingTools`) saying when to call them. The test was not changed.

## Acceptance criteria

1. Turn 1, "Cancel my booking MS-777. My name is John Doe.": only `getBookingDetails("MS-777", "John", "Doe")`
   runs, `cancelBooking` does not, and the reply asks the customer to confirm.
   (`-Plive-it`: `CustomerSupportAgentIT#should_cancel_booking`)
2. Turn 2, "yes, cancel it", in the same session: only `cancelBooking("MS-777", "John", "Doe")` runs
   and the reply contains "We hope to welcome you back again soon". (`-Plive-it`: same test)
3. The system message sent to the model contains "first make sure it exists by calling getBookingDetails"
   and "Do not call cancelBooking until the customer has confirmed", and both tools are offered.
   (default build: `CustomerSupportAgentWiringTests#agent_sends_the_system_rules_and_both_booking_tools_to_the_model`)
4. The `getBookingDetails` description contains "check that a booking exists before asking the
   customer to confirm a cancellation"; the `cancelBooking` description contains "only after the
   customer has explicitly confirmed"; all three parameters are required and described.
   (default build: `BookingToolsTests`)
5. When the model makes the two-turn call sequence, the real tools run, the second request still
   carries the first turn's tool result (chat memory), and the booking is gone afterwards.
   (default build: `CustomerSupportAgentWiringTests#two_turn_cancellation_runs_the_real_tools_and_removes_the_booking`)
6. Without a first and last name, no tool runs and the agent asks for them.
   (`-Plive-it`: `should_not_provide_booking_details_when_not_enough_data_is_provided`)

## Failure cases

- Unknown booking number, or a name that does not match: `BookingNotFoundException("Booking <n> not found")`,
  the same message in both cases so the agent cannot confirm that a number exists for someone
  else (`BookingServiceTests`). LangChain4j hands the message back to the model as the tool
  result (`CustomerSupportAgentWiringTests#lookup_of_a_booking_that_does_not_exist_reaches_the_model_as_a_tool_error`).
- Booking already started: `cancelBooking` throws `IllegalStateException("Cannot cancel booking <n> because it has already started or completed")`
  and the booking is kept (`BookingServiceTests#booking_that_has_started_cannot_be_cancelled_and_is_kept`).
- The day before it starts, cancellation still succeeds (`BookingServiceTests#booking_can_still_be_cancelled_the_day_before_it_starts`).
- The model skips the lookup or cancels without confirmation: only the live test catches this.

## Non-goals

- Real persistence or authentication. `BookingService` is an in-memory sample with one booking,
  `MS-777` for John Doe, starting 23 days after the app starts (it used to be fixed at
  2025-12-13, which made every real cancellation fail once that date had passed).
- Enforcing the terms of use in code. See Open questions.

## Rollback

Revert the commit that added the tool descriptions and changed rule 2. Nothing else depends on
them; `BookingToolsTests` and the system-message assertion in `CustomerSupportAgentWiringTests` go with it.

## Open questions

- Not yet verified against the live model: run
  `./mvnw verify -Plive-it -Dit.test="CustomerSupportAgentIT#should_cancel_booking"` a few times
  and record the pass rate here.
- The terms of use say reservations can be cancelled up to 7 days before the start (4.1) and
  bookings shorter than 3 days cannot be cancelled (4.2). `BookingService` only refuses bookings
  that have already started. Enforcing 4.1 and 4.2 in `cancelBooking` is a product decision.
