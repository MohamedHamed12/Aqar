package com.aqar.listing.exception;

public class MaxImagesExceededException extends RuntimeException {
    public MaxImagesExceededException(int max) {
        super("max_images_exceeded:" + max);
    }
}
