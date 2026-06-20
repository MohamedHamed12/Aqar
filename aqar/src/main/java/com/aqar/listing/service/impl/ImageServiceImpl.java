package com.aqar.listing.service.impl;

import com.aqar.listing.dto.ConfirmUploadRequest;
import com.aqar.listing.dto.DropboxUploadResponse;
import com.aqar.listing.dto.ReorderImagesRequest;
import com.aqar.listing.entity.ListingEntity;
import com.aqar.listing.entity.ListingImageEntity;
import com.aqar.listing.exception.ImageLockAcquisitionException;
import com.aqar.listing.exception.ImageNotFoundException;
import com.aqar.listing.exception.InvalidImageException;
import com.aqar.listing.exception.ListingNotFoundException;
import com.aqar.listing.exception.MaxImagesExceededException;
import com.aqar.listing.repository.ListingImageRepository;
import com.aqar.listing.repository.ListingRepository;
import com.aqar.listing.config.DropboxProperties;
import com.aqar.listing.service.DropboxStorageService;
import com.aqar.listing.service.ImageProcessingService;
import com.aqar.listing.service.ImageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class ImageServiceImpl implements ImageService {

    private static final int MAX_IMAGES = 20;
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".jpg", ".jpeg", ".png", ".webp");

    private final ListingRepository listingRepository;
    private final ListingImageRepository listingImageRepository;
    private final DropboxStorageService dropboxStorageService;
    private final ImageProcessingService imageProcessingService;
    private final StringRedisTemplate redisTemplate;
    private final String appFolder;
    private final long lockTtl;

    public ImageServiceImpl(ListingRepository listingRepository,
                            ListingImageRepository listingImageRepository,
                            DropboxStorageService dropboxStorageService,
                            ImageProcessingService imageProcessingService,
                            StringRedisTemplate redisTemplate,
                            DropboxProperties dropboxProperties,
                            @Value("${app.redis.lock-ttl}") long lockTtl) {
        this.listingRepository = listingRepository;
        this.listingImageRepository = listingImageRepository;
        this.dropboxStorageService = dropboxStorageService;
        this.imageProcessingService = imageProcessingService;
        this.redisTemplate = redisTemplate;
        this.appFolder = dropboxProperties.appFolder();
        this.lockTtl = lockTtl;
    }

    @Override
    public DropboxUploadResponse upload(Long listingId, Long ownerId, MultipartFile file) {
        ListingEntity listing = findOwnedListing(listingId, ownerId);

        long imageCount = listingImageRepository.countByListingId(listingId);
        if (imageCount >= MAX_IMAGES) {
            throw new MaxImagesExceededException(MAX_IMAGES);
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidImageException("unsupported_extension");
        }

        String uuid = UUID.randomUUID().toString();
        String dropboxPath = appFolder + "/listings/" + listingId + "/" + uuid + extension;

        try (InputStream inputStream = file.getInputStream()) {
            var metadata = dropboxStorageService.upload(dropboxPath, inputStream, file.getSize());
            return new DropboxUploadResponse(dropboxPath, metadata.getId());
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to Dropbox", e);
        }
    }

    @Override
    public void confirm(Long listingId, ConfirmUploadRequest request, Long ownerId) {
        findOwnedListing(listingId, ownerId);
        imageProcessingService.processImage(listingId, request.dropboxPath());
    }

    @Override
    public void deleteImage(Long listingId, Long imageId, Long ownerId) {
        findOwnedListing(listingId, ownerId);

        ListingImageEntity image = listingImageRepository.findById(imageId)
                .orElseThrow(() -> new ImageNotFoundException(imageId));

        if (!image.getListing().getId().equals(listingId)) {
            throw new ImageNotFoundException(imageId);
        }

        dropboxStorageService.delete(image.getDropboxPath());
        if (image.getThumbnailPath() != null) {
            dropboxStorageService.delete(image.getThumbnailPath());
        }

        listingImageRepository.delete(image);

        List<ListingImageEntity> remaining = listingImageRepository
                .findByListingIdOrderByDisplayOrderAsc(listingId);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setDisplayOrder(i);
        }
        listingImageRepository.saveAll(remaining);
    }

    @Override
    public void reorder(Long listingId, ReorderImagesRequest request, Long ownerId) {
        findOwnedListing(listingId, ownerId);

        String lockKey = "listing:" + listingId + ":image-lock";
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked");
        if (!Boolean.TRUE.equals(acquired)) {
            throw new ImageLockAcquisitionException(listingId);
        }

        redisTemplate.expire(lockKey, lockTtl, TimeUnit.MILLISECONDS);

        try {
            List<ListingImageEntity> images = listingImageRepository
                    .findByListingIdOrderByDisplayOrderAsc(listingId);

            for (int i = 0; i < request.imageIds().size(); i++) {
                Long targetId = request.imageIds().get(i);
                ListingImageEntity image = images.stream()
                        .filter(img -> img.getId().equals(targetId))
                        .findFirst()
                        .orElseThrow(() -> new ImageNotFoundException(targetId));
                image.setDisplayOrder(i);
            }

            listingImageRepository.saveAll(images);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private ListingEntity findOwnedListing(Long listingId, Long ownerId) {
        ListingEntity listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));
        if (!listing.getOwnerId().equals(ownerId)) {
            throw new ListingNotFoundException(listingId);
        }
        return listing;
    }

    private String extractExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.')).toLowerCase();
    }
}
