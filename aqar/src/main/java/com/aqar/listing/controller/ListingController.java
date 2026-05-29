package com.aqar.listing.controller;

import com.aqar.listing.dto.CreateListingRequest;
import com.aqar.listing.dto.ListingDetailResponse;
import com.aqar.listing.dto.ListingSummaryResponse;
import com.aqar.listing.dto.PatchListingStatusRequest;
import com.aqar.listing.dto.UpdateListingRequest;
import com.aqar.listing.service.ListingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/listings")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @PostMapping
    public ResponseEntity<ListingDetailResponse> create(@Valid @RequestBody CreateListingRequest request,
                                                        Authentication authentication) {
        ListingDetailResponse response = listingService.create(currentUserId(authentication), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingDetailResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(listingService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @listingSecurityService.isOwner(#id, authentication)")
    public ResponseEntity<ListingDetailResponse> update(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateListingRequest request) {
        return ResponseEntity.ok(listingService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or @listingSecurityService.isOwner(#id, authentication)")
    public ResponseEntity<ListingDetailResponse> changeStatus(@PathVariable Long id,
                                                              @Valid @RequestBody PatchListingStatusRequest request) {
        return ResponseEntity.ok(listingService.changeStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @listingSecurityService.isOwner(#id, authentication)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        listingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<ListingSummaryResponse>> listMine(Authentication authentication, Pageable pageable) {
        return ResponseEntity.ok(listingService.listMine(currentUserId(authentication), pageable));
    }

    private Long currentUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalArgumentException("invalid_principal");
    }
}