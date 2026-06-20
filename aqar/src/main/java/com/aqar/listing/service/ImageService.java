package com.aqar.listing.service;

import com.aqar.listing.dto.ConfirmUploadRequest;
import com.aqar.listing.dto.DropboxUploadResponse;
import com.aqar.listing.dto.ReorderImagesRequest;
import org.springframework.web.multipart.MultipartFile;

public interface ImageService {

    DropboxUploadResponse upload(Long listingId, Long ownerId, MultipartFile file);

    void confirm(Long listingId, ConfirmUploadRequest request, Long ownerId);

    void deleteImage(Long listingId, Long imageId, Long ownerId);

    void reorder(Long listingId, ReorderImagesRequest request, Long ownerId);
}
