package dev.langchain4j.carrental.booking;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * Tools the agent may call. The {@link Tool} and {@link P} descriptions are sent to the model as the tool
 * specification, so they are part of the agent's behaviour, not documentation: change them with the same care
 * as the system message in {@code CustomerSupportAgent}. See docs/specs/0002-booking-cancellation-flow.md.
 */
@Component
public class BookingTools {

    static final String GET_BOOKING_DETAILS_DESCRIPTION =
            "Looks up a booking by its booking number for the named customer and returns its start and end dates."
                    + " Fails if no booking with that number belongs to that customer."
                    + " Call this to check that a booking exists before asking the customer to confirm a cancellation.";

    static final String CANCEL_BOOKING_DESCRIPTION = "Cancels a booking that getBookingDetails has already found"
            + " earlier in this conversation. Call this only after the customer has explicitly confirmed,"
            + " in a later message, that they want the booking cancelled.";

    private final BookingService bookingService;

    public BookingTools(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Tool(GET_BOOKING_DETAILS_DESCRIPTION)
    public Booking getBookingDetails(
            @P("booking number, for example MS-777") String bookingNumber,
            @P("customer's first name") String customerName,
            @P("customer's last name") String customerSurname) {
        return bookingService.getBookingDetails(bookingNumber, customerName, customerSurname);
    }

    @Tool(CANCEL_BOOKING_DESCRIPTION)
    public void cancelBooking(
            @P("booking number, for example MS-777") String bookingNumber,
            @P("customer's first name") String customerName,
            @P("customer's last name") String customerSurname) {
        bookingService.cancelBooking(bookingNumber, customerName, customerSurname);
    }
}
