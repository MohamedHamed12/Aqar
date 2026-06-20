package com.aqar.listing.controller;

import com.aqar.listing.dto.CreateListingRequest;
import com.aqar.listing.dto.ListingDetailResponse;
import com.aqar.listing.dto.ListingSummaryResponse;
import com.aqar.listing.dto.PatchListingStatusRequest;
import com.aqar.listing.dto.UpdateListingRequest;
import com.aqar.listing.service.ListingService;
import com.aqar.shared.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Listings", description = "Real estate listing CRUD operations")
@SecurityRequirement(name = "BearerJwt")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @Operation(summary = "Create a new listing")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Listing created"),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ListingDetailResponse> create(@Valid @RequestBody CreateListingRequest request,
                                                        @Parameter(hidden = true) Authentication authentication) {
        ListingDetailResponse response = listingService.create(currentUserId(authentication), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Get listing by ID", description = "Public endpoint, no authentication required")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listing found"),
            @ApiResponse(responseCode = "404", description = "Listing not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ListingDetailResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(listingService.getById(id));
    }

    @Operation(summary = "Update a listing")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listing updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Not the owner or admin",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Listing not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @listingSecurityService.isOwner(#id, authentication)")
    public ResponseEntity<ListingDetailResponse> update(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateListingRequest request) {
        return ResponseEntity.ok(listingService.update(id, request));
    }

    @Operation(summary = "Change listing status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status changed"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Not the owner or admin",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Listing not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or @listingSecurityService.isOwner(#id, authentication)")
    public ResponseEntity<ListingDetailResponse> changeStatus(@PathVariable Long id,
                                                              @Valid @RequestBody PatchListingStatusRequest request) {
        return ResponseEntity.ok(listingService.changeStatus(id, request.status()));
    }

    @Operation(summary = "Delete a listing")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Listing deleted"),
            @ApiResponse(responseCode = "403", description = "Not the owner or admin",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Listing not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @listingSecurityService.isOwner(#id, authentication)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        listingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List current user's listings", description = "Returns paginated listings owned by the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated listing summaries"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<Page<ListingSummaryResponse>> listMine(@Parameter(hidden = true) Authentication authentication,
                                                                  Pageable pageable) {
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