package com.aqar.listing.dto;

public record ListingImageResponse(
        String dropboxPath,
        String thumbnailPath,
        Integer displayOrder
) {
}