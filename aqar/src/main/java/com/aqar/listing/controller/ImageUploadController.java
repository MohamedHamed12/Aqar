package com.aqar.listing.controller;

import com.aqar.listing.dto.ConfirmUploadRequest;
import com.aqar.listing.dto.DropboxUploadResponse;
import com.aqar.listing.dto.ReorderImagesRequest;
import com.aqar.listing.service.ImageService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/listings/{id}/images")
@Tag(name = "Listing Images", description = "Image upload and management for listings")
@SecurityRequirement(name = "BearerJwt")
public class ImageUploadController {

    private final ImageService imageService;

    public ImageUploadController(ImageService imageService) {
        this.imageService = imageService;
    }

    @Operation(summary = "Upload an image for a listing")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image uploaded, pending confirmation"),
            @ApiResponse(responseCode = "400", description = "Invalid image",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Not the owner or admin",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Listing not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Lock contention on concurrent upload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or @listingSecurityService.isOwner(#id, authentication)")
    public ResponseEntity<DropboxUploadResponse> upload(@PathVariable Long id,
                                                         @RequestParam MultipartFile file,
                                                         @Parameter(hidden = true) Authentication authentication) {
        DropboxUploadResponse response = imageService.upload(id, currentUserId(authentication), file);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Confirm an uploaded image and persist it to the listing")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Image confirmed"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Not the owner or admin",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Listing or image not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/confirm")
    @PreAuthorize("hasRole('ADMIN') or @listingSecurityService.isOwner(#id, authentication)")
    public ResponseEntity<Void> confirm(@PathVariable Long id,
                                         @Valid @RequestBody ConfirmUploadRequest request,
                                         @Parameter(hidden = true) Authentication authentication) {
        imageService.confirm(id, request, currentUserId(authentication));
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @Operation(summary = "Delete an image from a listing")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Image deleted"),
            @ApiResponse(responseCode = "403", description = "Not the owner or admin",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Listing or image not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasRole('ADMIN') or @listingSecurityService.isOwner(#id, authentication)")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id,
                                             @PathVariable Long imageId,
                                             @Parameter(hidden = true) Authentication authentication) {
        imageService.deleteImage(id, imageId, currentUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reorder images for a listing")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Images reordered"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Not the owner or admin",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Listing not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/order")
    @PreAuthorize("hasRole('ADMIN') or @listingSecurityService.isOwner(#id, authentication)")
    public ResponseEntity<Void> reorder(@PathVariable Long id,
                                         @Valid @RequestBody ReorderImagesRequest request,
                                         @Parameter(hidden = true) Authentication authentication) {
        imageService.reorder(id, request, currentUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    private Long currentUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalArgumentException("invalid_principal");
    }
}
