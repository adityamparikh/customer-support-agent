package dev.langchain4j.carrental.booking;

import dev.langchain4j.carrental.customer.Customer;

import java.time.LocalDate;

public record Booking(
        String bookingNumber,
        LocalDate bookingBeginDate,
        LocalDate bookingEndDate,
        Customer customer) {
}