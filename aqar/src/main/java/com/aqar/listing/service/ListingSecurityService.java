package com.aqar.listing.service;

import com.aqar.listing.repository.ListingRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("listingSecurityService")
public class ListingSecurityService {

    private final ListingRepository listingRepository;

    public ListingSecurityService(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    public boolean isOwner(Long id, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return false;
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Number number)) {
            return false;
        }
        return listingRepository.existsByIdAndOwnerId(id, number.longValue());
    }
}