package dev.langchain4j.carrental.booking;


import dev.langchain4j.carrental.customer.Customer;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BookingService {

    private static final Customer CUSTOMER = new Customer("John", "Doe");

    private static final String BOOKING_NUMBER = "MS-777";
    private static final Booking BOOKING = new Booking(
            BOOKING_NUMBER,
            LocalDate.of(2025, 12, 13),
            LocalDate.of(2025, 12, 31),
            CUSTOMER
    );

    // Imitating a DB table. This singleton bean is called from concurrent HTTP requests,
    // so the map must be thread-safe.
    private final Map<String, Booking> bookings = new ConcurrentHashMap<>(Map.of(BOOKING_NUMBER, BOOKING));

    public Booking getBookingDetails(String bookingNumber, String customerName, String customerSurname) {
        return findBooking(bookingNumber, customerName, customerSurname);
    }

    public void cancelBooking(String bookingNumber, String customerName, String customerSurname) {
        Booking booking = findBooking(bookingNumber, customerName, customerSurname);
        if (!booking.bookingBeginDate().isAfter(LocalDate.now())) {
            throw new IllegalStateException("Cannot cancel booking " + bookingNumber
                    + " because it has already started or completed");
        }

        // Imitating booking cancellation. Remove only the booking that was checked above,
        // so two concurrent cancellations cannot both succeed.
        if (!bookings.remove(bookingNumber, booking)) {
            throw new BookingNotFoundException(bookingNumber);
        }
    }

    private Booking findBooking(String bookingNumber, String customerName, String customerSurname) {
        // Imitating DB lookup
        Booking booking = bookings.get(bookingNumber);
        if (booking == null) {
            throw new BookingNotFoundException(bookingNumber);
        }

        Customer customer = booking.customer();
        if (!customer.name().equals(customerName)) {
            throw new BookingNotFoundException(bookingNumber);
        }
        if (!customer.surname().equals(customerSurname)) {
            throw new BookingNotFoundException(bookingNumber);
        }
        return booking;
    }
}
