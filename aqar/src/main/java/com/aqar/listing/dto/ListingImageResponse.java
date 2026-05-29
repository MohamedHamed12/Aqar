package com.aqar.listing.dto;

public record ListingImageResponse(
        String s3Key,
        String thumbnailKey,
        Integer displayOrder
) {
}