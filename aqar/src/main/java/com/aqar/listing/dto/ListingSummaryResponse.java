package com.aqar.listing.dto;

import com.aqar.listing.entity.ListingPurpose;
import com.aqar.listing.entity.ListingStatus;
import com.aqar.listing.entity.ListingType;

import java.math.BigDecimal;
import java.time.Instant;

public record ListingSummaryResponse(
        Long id,
        String title,
        ListingStatus status,
        ListingPurpose purpose,
        ListingType type,
        BigDecimal price,
        BigDecimal areaSqm,
        Integer bedrooms,
        BigDecimal latitude,
        BigDecimal longitude,
        Instant createdAt,
        Instant updatedAt
) {
}