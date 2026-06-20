package com.aqar.listing.exception;

public class ImageLockAcquisitionException extends RuntimeException {
    public ImageLockAcquisitionException(Long listingId) {
        super("image_lock_contention:" + listingId);
    }
}
