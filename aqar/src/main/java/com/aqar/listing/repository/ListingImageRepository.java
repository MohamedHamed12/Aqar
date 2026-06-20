package com.aqar.listing.repository;

import com.aqar.listing.entity.ListingImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListingImageRepository extends JpaRepository<ListingImageEntity, Long> {

    List<ListingImageEntity> findByListingIdOrderByDisplayOrderAsc(Long listingId);

    long countByListingId(Long listingId);
}
