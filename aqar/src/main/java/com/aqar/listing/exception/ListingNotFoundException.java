package com.aqar.listing.exception;

public class ListingNotFoundException extends RuntimeException {
    public ListingNotFoundException(Long id) {
        super("listing_not_found:" + id);
    }
}