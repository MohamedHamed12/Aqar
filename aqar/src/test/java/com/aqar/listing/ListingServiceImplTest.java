package com.aqar.listing;

import com.aqar.listing.dto.CreateListingRequest;
import com.aqar.listing.dto.ListingDetailResponse;
import com.aqar.listing.dto.ListingSummaryResponse;
import com.aqar.listing.dto.UpdateListingRequest;
import com.aqar.listing.entity.ListingEntity;
import com.aqar.listing.entity.ListingPurpose;
import com.aqar.listing.entity.ListingStatus;
import com.aqar.listing.entity.ListingType;
import com.aqar.listing.exception.ListingNotFoundException;
import com.aqar.listing.mapper.ListingMapper;
import com.aqar.listing.outbox.ListingCreatedEvent;
import com.aqar.listing.outbox.ListingOutboxPublisher;
import com.aqar.listing.repository.ListingRepository;
import com.aqar.listing.service.impl.ListingServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingServiceImplTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private ListingMapper listingMapper;

    @Mock
    private ListingOutboxPublisher outboxPublisher;

    private ListingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ListingServiceImpl(listingRepository, listingMapper, outboxPublisher);
    }

    @Test
    void createPersistsListingAndPublishesOutboxEvent() {
        ListingEntity saved = baseListing();
        saved.setId(10L);
        CreateListingRequest request = new CreateListingRequest(
                "New Listing",
                "Bright apartment",
                5L,
                ListingPurpose.SALE,
                ListingType.APARTMENT,
                new BigDecimal("2500000.00"),
                new BigDecimal("120.50"),
                3,
                new BigDecimal("30.0444"),
                new BigDecimal("31.2357"),
                List.of("img-1", "img-2")
        );

        when(listingRepository.save(any(ListingEntity.class))).thenReturn(saved);
        ListingDetailResponse expected = new ListingDetailResponse(10L, 2L, 5L, "New Listing", "Bright apartment", ListingStatus.DRAFT, ListingPurpose.SALE, ListingType.APARTMENT, new BigDecimal("2500000.00"), new BigDecimal("120.50"), 3, new BigDecimal("30.0444"), new BigDecimal("31.2357"), List.of(), Instant.now(), Instant.now());
        when(listingMapper.toDetailResponse(saved)).thenReturn(expected);

        ListingDetailResponse response = service.create(2L, request);

        Assertions.assertEquals(expected, response);
        ArgumentCaptor<ListingCreatedEvent> captor = ArgumentCaptor.forClass(ListingCreatedEvent.class);
        verify(outboxPublisher).publish(captor.capture());
        Assertions.assertEquals(10L, captor.getValue().listingId());
        Assertions.assertEquals(2L, captor.getValue().ownerId());
    }

    @Test
    void getByIdReturnsMappedListing() {
        ListingEntity listing = baseListing();
        listing.setId(7L);
        when(listingRepository.findById(7L)).thenReturn(Optional.of(listing));
        ListingDetailResponse expected = new ListingDetailResponse(7L, 2L, 5L, "Listing", "Content", ListingStatus.DRAFT, ListingPurpose.SALE, ListingType.APARTMENT, new BigDecimal("100.00"), new BigDecimal("50.00"), 2, new BigDecimal("30.0"), new BigDecimal("31.0"), List.of(), Instant.now(), Instant.now());
        when(listingMapper.toDetailResponse(listing)).thenReturn(expected);

        ListingDetailResponse response = service.getById(7L);

        Assertions.assertEquals(expected, response);
    }

    @Test
    void updateReplacesFields() {
        ListingEntity listing = baseListing();
        listing.setId(7L);
        when(listingRepository.findById(7L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(ListingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ListingDetailResponse expected = new ListingDetailResponse(7L, 2L, 8L, "Updated", "Updated content", ListingStatus.DRAFT, ListingPurpose.RENT, ListingType.STUDIO, new BigDecimal("5000.00"), new BigDecimal("35.00"), 1, new BigDecimal("29.9"), new BigDecimal("31.2"), List.of(), Instant.now(), Instant.now());
        when(listingMapper.toDetailResponse(any(ListingEntity.class))).thenReturn(expected);

        ListingDetailResponse response = service.update(7L, new UpdateListingRequest(
                "Updated",
                "Updated content",
                8L,
                ListingPurpose.RENT,
                ListingType.STUDIO,
                new BigDecimal("5000.00"),
                new BigDecimal("35.00"),
                1,
                new BigDecimal("29.9"),
                new BigDecimal("31.2"),
                List.of("img-1")
        ));

        Assertions.assertEquals(expected, response);
        Assertions.assertEquals("Updated", listing.getTitle());
        Assertions.assertEquals(1, listing.getImages().size());
    }

    @Test
    void changeStatusAllowsDraftToActive() {
        ListingEntity listing = baseListing();
        listing.setId(7L);
        when(listingRepository.findById(7L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(ListingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(listingMapper.toDetailResponse(any(ListingEntity.class))).thenAnswer(invocation -> {
            ListingEntity entity = invocation.getArgument(0);
            return new ListingDetailResponse(entity.getId(), entity.getOwnerId(), entity.getNeighborhoodId(), entity.getTitle(), entity.getContent(), entity.getStatus(), entity.getPurpose(), entity.getType(), entity.getPrice(), entity.getAreaSqm(), entity.getBedrooms(), entity.getLatitude(), entity.getLongitude(), List.of(), Instant.now(), Instant.now());
        });

        ListingDetailResponse response = service.changeStatus(7L, ListingStatus.ACTIVE);

        Assertions.assertEquals(ListingStatus.ACTIVE, response.status());
    }

    @Test
    void deleteMarksListingDeleted() {
        ListingEntity listing = baseListing();
        listing.setId(7L);
        when(listingRepository.findById(7L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(ListingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.delete(7L);

        Assertions.assertEquals(ListingStatus.DELETED, listing.getStatus());
    }

    @Test
    void listMineMapsPageOfListings() {
        ListingEntity listing = baseListing();
        listing.setId(7L);
        Page<ListingEntity> page = new PageImpl<>(List.of(listing), PageRequest.of(0, 20), 1);
        when(listingRepository.findByOwnerIdOrderByCreatedAtDesc(eq(2L), any())).thenReturn(page);
        when(listingMapper.toSummaryResponse(listing)).thenReturn(new ListingSummaryResponse(7L, "Listing", ListingStatus.DRAFT, ListingPurpose.SALE, ListingType.APARTMENT, new BigDecimal("100.00"), new BigDecimal("50.00"), 2, new BigDecimal("30.0"), new BigDecimal("31.0"), Instant.now(), Instant.now()));

        Page<ListingSummaryResponse> response = service.listMine(2L, PageRequest.of(0, 20));

        Assertions.assertEquals(1, response.getTotalElements());
        Assertions.assertEquals(7L, response.getContent().get(0).id());
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(listingRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ListingNotFoundException.class, () -> service.getById(99L));
    }

    private ListingEntity baseListing() {
        ListingEntity listing = new ListingEntity();
        listing.setOwnerId(2L);
        listing.setNeighborhoodId(5L);
        listing.setTitle("Listing");
        listing.setContent("Content");
        listing.setStatus(ListingStatus.DRAFT);
        listing.setPurpose(ListingPurpose.SALE);
        listing.setType(ListingType.APARTMENT);
        listing.setPrice(new BigDecimal("100.00"));
        listing.setAreaSqm(new BigDecimal("50.00"));
        listing.setBedrooms(2);
        listing.setLatitude(new BigDecimal("30.0"));
        listing.setLongitude(new BigDecimal("31.0"));
        return listing;
    }
}