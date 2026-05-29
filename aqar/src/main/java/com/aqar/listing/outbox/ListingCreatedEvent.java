package com.aqar.listing.outbox;

import java.time.Instant;

public record ListingCreatedEvent(
        Long listingId,
        Long ownerId,
        Long neighborhoodId,
        String title,
        Instant occurredAt
) {
}