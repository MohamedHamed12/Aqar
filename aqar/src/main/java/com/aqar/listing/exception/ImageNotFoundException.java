package com.aqar.listing.exception;

public class ImageNotFoundException extends RuntimeException {
    public ImageNotFoundException(Long id) {
        super("image_not_found:" + id);
    }
}
