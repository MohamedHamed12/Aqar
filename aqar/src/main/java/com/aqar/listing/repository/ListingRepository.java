package com.aqar.listing.repository;

import com.aqar.listing.entity.ListingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ListingRepository extends JpaRepository<ListingEntity, Long> {
    Page<ListingEntity> findByOwnerIdOrderByCreatedAtDesc(Long ownerId, Pageable pageable);

    Optional<ListingEntity> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByIdAndOwnerId(Long id, Long ownerId);
}