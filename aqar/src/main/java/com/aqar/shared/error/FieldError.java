package com.aqar.shared.error;

public record FieldError(
        String field,
        String message,
        Object rejectedValue
) {
}