package com.aqar.listing.dto;

import com.aqar.listing.entity.ListingPurpose;
import com.aqar.listing.entity.ListingType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateListingRequest(
        @NotBlank String title,
        String content,
        @NotNull Long neighborhoodId,
        @NotNull ListingPurpose purpose,
        @NotNull ListingType type,
        @NotNull @DecimalMin("0.01") @DecimalMax("999999999.99") BigDecimal price,
        @NotNull @Positive BigDecimal areaSqm,
        @NotNull Integer bedrooms,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        @Size(max = 20) List<@NotBlank String> imagePaths
) {
}