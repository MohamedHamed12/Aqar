package com.aqar.listing.outbox;

public interface ListingOutboxPublisher {
    void publish(ListingCreatedEvent event);
}