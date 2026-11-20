package dev.langchain4j.carrental.booking;

import dev.langchain4j.carrental.customer.Customer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory stand-in for a booking database. It is seeded with one sample booking, {@code MS-777} for John Doe,
 * that always starts in the future so the cancellation flow can be tried against a running app.
 */
@Component
public class BookingService {

    static final String SAMPLE_BOOKING_NUMBER = "MS-777";
    static final Customer SAMPLE_CUSTOMER = new Customer("John", "Doe");

    /** Days from today until the sample booking starts; longer than the 7-day cancellation window. */
    static final int SAMPLE_BOOKING_STARTS_IN_DAYS = 23;

    static final int SAMPLE_BOOKING_LENGTH_DAYS = 18;

    private final Clock clock;
    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();

    public BookingService() {
        // The JVM's zone, the same one LangChain4j uses for {{current_date}} in the system message.
        this(Clock.system(ZoneId.systemDefault()));
    }

    BookingService(Clock clock) {
        this.clock = clock;
        LocalDate begin = LocalDate.now(clock).plusDays(SAMPLE_BOOKING_STARTS_IN_DAYS);
        bookings.put(
                SAMPLE_BOOKING_NUMBER,
                new Booking(SAMPLE_BOOKING_NUMBER, begin, begin.plusDays(SAMPLE_BOOKING_LENGTH_DAYS), SAMPLE_CUSTOMER));
    }

    public Booking getBookingDetails(String bookingNumber, String customerName, String customerSurname) {
        return findOwnedBy(bookingNumber, customerName, customerSurname);
    }

    public void cancelBooking(String bookingNumber, String customerName, String customerSurname) {
        Booking booking = findOwnedBy(bookingNumber, customerName, customerSurname);
        if (!booking.bookingBeginDate().isAfter(LocalDate.now(clock))) {
            throw new IllegalStateException(
                    "Cannot cancel booking " + bookingNumber + " because it has already started or completed");
        }
        bookings.remove(bookingNumber);
    }

    /**
     * Returns the booking only when both the number and the customer's name match, and otherwise throws the same
     * {@link BookingNotFoundException}, so a caller cannot learn that a booking number exists for someone else.
     */
    private Booking findOwnedBy(String bookingNumber, String customerName, String customerSurname) {
        Booking booking = bookings.get(bookingNumber);
        if (booking == null) {
            throw new BookingNotFoundException(bookingNumber);
        }
        Customer customer = booking.customer();
        if (!customer.name().equals(customerName) || !customer.surname().equals(customerSurname)) {
            throw new BookingNotFoundException(bookingNumber);
        }
        return booking;
    }
}
