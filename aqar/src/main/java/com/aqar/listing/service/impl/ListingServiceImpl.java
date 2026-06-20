package com.aqar.listing.service.impl;

import com.aqar.listing.dto.CreateListingRequest;
import com.aqar.listing.dto.ListingDetailResponse;
import com.aqar.listing.dto.ListingSummaryResponse;
import com.aqar.listing.dto.UpdateListingRequest;
import com.aqar.listing.entity.ListingEntity;
import com.aqar.listing.entity.ListingImageEntity;
import com.aqar.listing.entity.ListingStatus;
import com.aqar.listing.exception.ListingNotFoundException;
import com.aqar.listing.exception.ListingStateTransitionException;
import com.aqar.listing.mapper.ListingMapper;
import com.aqar.listing.outbox.ListingCreatedEvent;
import com.aqar.listing.outbox.ListingOutboxPublisher;
import com.aqar.listing.repository.ListingRepository;
import com.aqar.listing.service.ListingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ListingServiceImpl implements ListingService {

    private final ListingRepository listingRepository;
    private final ListingMapper listingMapper;
    private final ListingOutboxPublisher outboxPublisher;

    public ListingServiceImpl(ListingRepository listingRepository,
                              ListingMapper listingMapper,
                              ListingOutboxPublisher outboxPublisher) {
        this.listingRepository = listingRepository;
        this.listingMapper = listingMapper;
        this.outboxPublisher = outboxPublisher;
    }

    @Override
    public ListingDetailResponse create(Long ownerId, CreateListingRequest request) {
        ListingEntity listing = new ListingEntity();
        applyRequest(listing, ownerId, request.title(), request.content(), request.neighborhoodId(), request.purpose(), request.type(), request.price(), request.areaSqm(), request.bedrooms(), request.latitude(), request.longitude(), request.imagePaths());
        listing.setStatus(ListingStatus.DRAFT);
        ListingEntity saved = listingRepository.save(listing);
        outboxPublisher.publish(new ListingCreatedEvent(saved.getId(), saved.getOwnerId(), saved.getNeighborhoodId(), saved.getTitle(), saved.getCreatedAt()));
        return listingMapper.toDetailResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ListingDetailResponse getById(Long id) {
        return listingMapper.toDetailResponse(findListing(id));
    }

    @Override
    public ListingDetailResponse update(Long id, UpdateListingRequest request) {
        ListingEntity listing = findListing(id);
        applyRequest(listing, listing.getOwnerId(), request.title(), request.content(), request.neighborhoodId(), request.purpose(), request.type(), request.price(), request.areaSqm(), request.bedrooms(), request.latitude(), request.longitude(), request.imagePaths());
        ListingEntity saved = listingRepository.save(listing);
        return listingMapper.toDetailResponse(saved);
    }

    @Override
    public ListingDetailResponse changeStatus(Long id, ListingStatus status) {
        ListingEntity listing = findListing(id);
        validateTransition(listing.getStatus(), status);
        listing.setStatus(status);
        return listingMapper.toDetailResponse(listingRepository.save(listing));
    }

    @Override
    public void delete(Long id) {
        ListingEntity listing = findListing(id);
        listing.setStatus(ListingStatus.DELETED);
        listingRepository.save(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ListingSummaryResponse> listMine(Long ownerId, Pageable pageable) {
        return listingRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId, pageable)
                .map(listingMapper::toSummaryResponse);
    }

    private ListingEntity findListing(Long id) {
        return listingRepository.findById(id).orElseThrow(() -> new ListingNotFoundException(id));
    }

    private void validateTransition(ListingStatus current, ListingStatus requested) {
        if (requested == null) {
            throw new ListingStateTransitionException("status_required");
        }
        if (current == requested) {
            return;
        }
        boolean valid = (current == ListingStatus.DRAFT && requested == ListingStatus.ACTIVE)
                || (current == ListingStatus.ACTIVE && requested == ListingStatus.ARCHIVED);
        if (!valid) {
            throw new ListingStateTransitionException("invalid_status_transition:" + current + "->" + requested);
        }
    }

    private void applyRequest(ListingEntity listing,
                              Long ownerId,
                              String title,
                              String content,
                              Long neighborhoodId,
                              com.aqar.listing.entity.ListingPurpose purpose,
                              com.aqar.listing.entity.ListingType type,
                              java.math.BigDecimal price,
                              java.math.BigDecimal areaSqm,
                              Integer bedrooms,
                              java.math.BigDecimal latitude,
                              java.math.BigDecimal longitude,
                              List<String> imagePaths) {
        listing.setOwnerId(ownerId);
        listing.setTitle(title);
        listing.setContent(content);
        listing.setNeighborhoodId(neighborhoodId);
        listing.setPurpose(purpose);
        listing.setType(type);
        listing.setPrice(price);
        listing.setAreaSqm(areaSqm);
        listing.setBedrooms(bedrooms);
        listing.setLatitude(latitude);
        listing.setLongitude(longitude);
        listing.setImages(buildImages(listing, imagePaths));
    }

    private List<ListingImageEntity> buildImages(ListingEntity listing, List<String> imagePaths) {
        List<ListingImageEntity> images = new ArrayList<>();
        if (imagePaths == null) {
            return images;
        }
        for (int index = 0; index < imagePaths.size(); index++) {
            ListingImageEntity image = new ListingImageEntity();
            image.setListing(listing);
            image.setDropboxPath(imagePaths.get(index));
            image.setDisplayOrder(index);
            images.add(image);
        }
        return images;
    }
}