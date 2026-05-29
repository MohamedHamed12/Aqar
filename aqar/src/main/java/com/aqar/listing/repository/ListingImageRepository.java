package com.aqar.listing.repository;

import com.aqar.listing.entity.ListingImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingImageRepository extends JpaRepository<ListingImageEntity, Long> {
}