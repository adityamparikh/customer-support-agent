package dev.langchain4j.carrental.booking;

import static dev.langchain4j.carrental.booking.BookingService.SAMPLE_BOOKING_NUMBER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class BookingServiceTests {

    private static final Instant TODAY = Instant.parse("2026-11-20T10:00:00Z");

    private final MutableClock clock = new MutableClock(TODAY);
    private final BookingService bookingService = new BookingService(clock);

    @Test
    void sample_booking_starts_in_the_future_so_it_can_be_cancelled() {
        Booking booking = bookingService.getBookingDetails(SAMPLE_BOOKING_NUMBER, "John", "Doe");

        assertThat(booking.bookingBeginDate()).isEqualTo(LocalDate.of(2026, 12, 13));
        assertThat(booking.bookingEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(booking.customer().name()).isEqualTo("John");
        assertThat(booking.customer().surname()).isEqualTo("Doe");
    }

    @Test
    void unknown_booking_number_is_not_found() {
        assertThatThrownBy(() -> bookingService.getBookingDetails("54321", "John", "Doe"))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessage("Booking 54321 not found");
    }

    @Test
    void booking_of_another_customer_looks_exactly_like_a_missing_booking() {
        assertThatThrownBy(() -> bookingService.getBookingDetails(SAMPLE_BOOKING_NUMBER, "Jane", "Doe"))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessage("Booking MS-777 not found");
        assertThatThrownBy(() -> bookingService.getBookingDetails(SAMPLE_BOOKING_NUMBER, "John", "Smith"))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessage("Booking MS-777 not found");
    }

    @Test
    void cancelled_booking_can_no_longer_be_found() {
        bookingService.cancelBooking(SAMPLE_BOOKING_NUMBER, "John", "Doe");

        assertThatThrownBy(() -> bookingService.getBookingDetails(SAMPLE_BOOKING_NUMBER, "John", "Doe"))
                .isInstanceOf(BookingNotFoundException.class);
        assertThatThrownBy(() -> bookingService.cancelBooking(SAMPLE_BOOKING_NUMBER, "John", "Doe"))
                .isInstanceOf(BookingNotFoundException.class);
    }

    @Test
    void another_customer_cannot_cancel_the_booking() {
        assertThatThrownBy(() -> bookingService.cancelBooking(SAMPLE_BOOKING_NUMBER, "Jane", "Doe"))
                .isInstanceOf(BookingNotFoundException.class);

        assertThat(bookingService.getBookingDetails(SAMPLE_BOOKING_NUMBER, "John", "Doe"))
                .isNotNull();
    }

    @Test
    void booking_that_has_started_cannot_be_cancelled_and_is_kept() {
        clock.advance(Duration.ofDays(BookingService.SAMPLE_BOOKING_STARTS_IN_DAYS));

        assertThatThrownBy(() -> bookingService.cancelBooking(SAMPLE_BOOKING_NUMBER, "John", "Doe"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot cancel booking MS-777 because it has already started or completed");
        assertThat(bookingService.getBookingDetails(SAMPLE_BOOKING_NUMBER, "John", "Doe"))
                .isNotNull();
    }

    @Test
    void booking_can_still_be_cancelled_the_day_before_it_starts() {
        clock.advance(Duration.ofDays(BookingService.SAMPLE_BOOKING_STARTS_IN_DAYS - 1L));

        bookingService.cancelBooking(SAMPLE_BOOKING_NUMBER, "John", "Doe");

        assertThatThrownBy(() -> bookingService.getBookingDetails(SAMPLE_BOOKING_NUMBER, "John", "Doe"))
                .isInstanceOf(BookingNotFoundException.class);
    }

    /** A UTC clock that a test can move forward. */
    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
