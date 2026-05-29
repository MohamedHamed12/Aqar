package com.aqar.listing.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class JpaListingOutboxPublisher implements ListingOutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JpaListingOutboxPublisher(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Override
    public void publish(ListingCreatedEvent event) {
        try {
            OutboxEventEntity entity = new OutboxEventEntity();
            entity.setAggregateType("Listing");
            entity.setAggregateId(String.valueOf(event.listingId()));
            entity.setEventType("ListingCreatedEvent");
            entity.setPayload(objectMapper.writeValueAsString(event));
            outboxEventRepository.save(entity);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("outbox_serialization_failed", exception);
        }
    }
}