package com.aqar.listing.exception;

public class InvalidImageException extends RuntimeException {
    public InvalidImageException(String reason) {
        super("invalid_image:" + reason);
    }
}
