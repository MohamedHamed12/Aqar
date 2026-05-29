package com.aqar.listing.service;

import com.aqar.listing.dto.CreateListingRequest;
import com.aqar.listing.dto.ListingDetailResponse;
import com.aqar.listing.dto.ListingSummaryResponse;
import com.aqar.listing.dto.UpdateListingRequest;
import com.aqar.listing.entity.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ListingService {
    ListingDetailResponse create(Long ownerId, CreateListingRequest request);

    ListingDetailResponse getById(Long id);

    ListingDetailResponse update(Long id, UpdateListingRequest request);

    ListingDetailResponse changeStatus(Long id, ListingStatus status);

    void delete(Long id);

    Page<ListingSummaryResponse> listMine(Long ownerId, Pageable pageable);
}