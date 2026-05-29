package com.aqar.listing.dto;

import com.aqar.listing.entity.ListingStatus;
import jakarta.validation.constraints.NotNull;

public record PatchListingStatusRequest(
        @NotNull ListingStatus status
) {
}