package com.aqar.listing.dto;

import com.aqar.listing.entity.ListingPurpose;
import com.aqar.listing.entity.ListingStatus;
import com.aqar.listing.entity.ListingType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ListingDetailResponse(
        Long id,
        Long ownerId,
        Long neighborhoodId,
        String title,
        String content,
        ListingStatus status,
        ListingPurpose purpose,
        ListingType type,
        BigDecimal price,
        BigDecimal areaSqm,
        Integer bedrooms,
        BigDecimal latitude,
        BigDecimal longitude,
        List<ListingImageResponse> images,
        Instant createdAt,
        Instant updatedAt
) {
}