package dev.langchain4j.carrental.booking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingServiceTest {

    private final BookingService bookingService = new BookingService();

    @Test
    void returns_booking_when_number_and_customer_match() {
        Booking booking = bookingService.getBookingDetails("MS-777", "John", "Doe");

        assertThat(booking.bookingNumber()).isEqualTo("MS-777");
        assertThat(booking.customer().name()).isEqualTo("John");
        assertThat(booking.customer().surname()).isEqualTo("Doe");
    }

    @Test
    void throws_not_found_for_unknown_booking_number() {
        assertThatThrownBy(() -> bookingService.getBookingDetails("54321", "John", "Doe"))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessage("Booking 54321 not found");
    }

    @Test
    void throws_not_found_when_customer_does_not_match() {
        assertThatThrownBy(() -> bookingService.getBookingDetails("MS-777", "John", "Smith"))
                .isInstanceOf(BookingNotFoundException.class);
        assertThatThrownBy(() -> bookingService.cancelBooking("MS-777", "Jane", "Doe"))
                .isInstanceOf(BookingNotFoundException.class);
    }

    @Test
    void refuses_to_cancel_a_booking_that_has_already_started() {
        // the sample booking runs 2025-12-13 to 2025-12-31
        assertThatThrownBy(() -> bookingService.cancelBooking("MS-777", "John", "Doe"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already started or completed");

        assertThat(bookingService.getBookingDetails("MS-777", "John", "Doe")).isNotNull();
    }
}
