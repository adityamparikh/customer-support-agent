package dev.langchain4j.example.customersupportagent.booking;

import dev.langchain4j.example.customersupportagent.customer.Customer;

import java.time.LocalDate;

public record Booking(
        String bookingNumber,
        LocalDate bookingBeginDate,
        LocalDate bookingEndDate,
        Customer customer) {
}