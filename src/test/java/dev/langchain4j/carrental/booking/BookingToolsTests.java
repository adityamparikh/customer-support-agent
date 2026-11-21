package dev.langchain4j.carrental.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.carrental.customer.Customer;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * The tool specification is what the model reads when it decides whether to call a tool. These tests pin the parts
 * of it that the cancellation flow depends on (docs/specs/0002-booking-cancellation-flow.md).
 */
class BookingToolsTests {

    private final Map<String, ToolSpecification> specifications =
            ToolSpecifications.toolSpecificationsFrom(BookingTools.class).stream()
                    .collect(Collectors.toMap(ToolSpecification::name, Function.identity()));

    @Test
    void exposes_exactly_the_two_booking_tools() {
        assertThat(specifications).containsOnlyKeys("getBookingDetails", "cancelBooking");
    }

    @Test
    void lookup_tool_tells_the_model_to_verify_the_booking_before_asking_for_confirmation() {
        assertThat(specifications.get("getBookingDetails").description())
                .contains("check that a booking exists before asking the customer to confirm a cancellation");
    }

    @Test
    void cancel_tool_tells_the_model_to_wait_for_explicit_confirmation() {
        assertThat(specifications.get("cancelBooking").description())
                .contains("getBookingDetails has already found")
                .contains("only after the customer has explicitly confirmed");
    }

    @Test
    void every_parameter_is_required_and_described() {
        for (ToolSpecification specification : specifications.values()) {
            assertThat(specification.parameters().required())
                    .as(specification.name())
                    .containsExactly("bookingNumber", "customerName", "customerSurname");
            assertThat(specification.parameters().properties().values())
                    .as(specification.name())
                    .allSatisfy(parameter -> assertThat(((JsonStringSchema) parameter).description())
                            .isNotBlank());
        }
    }

    @Test
    void tools_delegate_to_the_booking_service() {
        BookingService bookingService = mock(BookingService.class);
        Booking booking = new Booking(
                "MS-777", LocalDate.of(2026, 12, 13), LocalDate.of(2026, 12, 31), new Customer("John", "Doe"));
        when(bookingService.getBookingDetails("MS-777", "John", "Doe")).thenReturn(booking);
        BookingTools tools = new BookingTools(bookingService);

        assertThat(tools.getBookingDetails("MS-777", "John", "Doe")).isEqualTo(booking);
        tools.cancelBooking("MS-777", "John", "Doe");

        verify(bookingService).cancelBooking("MS-777", "John", "Doe");
    }
}
