package com.aqar.listing.controller;

import com.aqar.listing.dto.ConfirmUploadRequest;
import com.aqar.listing.dto.DropboxUploadResponse;
import com.aqar.listing.dto.ReorderImagesRequest;
import com.aqar.listing.service.ImageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
public class ImageUploadController {

    private final ImageService imageService;

    public ImageUploadController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN') or @listingSecurityService.isOwner(#id, authentication)")
    public ResponseEntity<DropboxUploadResponse> upload(@PathVariable Long id,
                                                         @RequestParam MultipartFile file,
                                                         Authentication authentication) {
        DropboxUploadResponse response = imageService.upload(id, currentUserId(authentication), file);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasRole('ADMIN') or @listingSecurityService.isOwner(#id, authentication)")
    public ResponseEntity<Void> confirm(@PathVariable Long id,
                                         @Valid @RequestBody ConfirmUploadRequest request,
                                         Authentication authentication) {
        imageService.confirm(id, request, currentUserId(authentication));
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasRole('ADMIN') or @listingSecurityService.isOwner(#id, authentication)")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id,
                                             @PathVariable Long imageId,
                                             Authentication authentication) {
        imageService.deleteImage(id, imageId, currentUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/order")
    @PreAuthorize("hasRole('ADMIN') or @listingSecurityService.isOwner(#id, authentication)")
    public ResponseEntity<Void> reorder(@PathVariable Long id,
                                         @Valid @RequestBody ReorderImagesRequest request,
                                         Authentication authentication) {
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
