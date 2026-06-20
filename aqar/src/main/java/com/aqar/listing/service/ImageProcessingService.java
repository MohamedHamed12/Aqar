package com.aqar.listing.service;

import com.aqar.listing.config.DropboxProperties;
import com.aqar.listing.entity.ListingEntity;
import com.aqar.listing.entity.ListingImageEntity;
import com.aqar.listing.exception.InvalidImageException;
import com.aqar.listing.exception.ListingNotFoundException;
import com.aqar.listing.exception.MaxImagesExceededException;
import com.aqar.listing.repository.ListingImageRepository;
import com.aqar.listing.repository.ListingRepository;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Service
public class ImageProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ImageProcessingService.class);
    private static final int MAX_IMAGES = 20;
    private static final int THUMBNAIL_WIDTH = 400;
    private static final int THUMBNAIL_HEIGHT = 300;
    private static final float THUMBNAIL_QUALITY = 0.85f;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");

    private final DropboxStorageService dropboxStorageService;
    private final ListingRepository listingRepository;
    private final ListingImageRepository listingImageRepository;
    private final String appFolder;

    public ImageProcessingService(DropboxStorageService dropboxStorageService,
                                  ListingRepository listingRepository,
                                  ListingImageRepository listingImageRepository,
                                  DropboxProperties dropboxProperties) {
        this.dropboxStorageService = dropboxStorageService;
        this.listingRepository = listingRepository;
        this.listingImageRepository = listingImageRepository;
        this.appFolder = dropboxProperties.appFolder();
    }

    @Async
    @Transactional
    public void  processImage(Long listingId, String dropboxPath) {
        try {
            ListingEntity listing = listingRepository.findById(listingId)
                    .orElseThrow(() -> new ListingNotFoundException(listingId));

            long imageCount = listingImageRepository.countByListingId(listingId);
            if (imageCount >= MAX_IMAGES) {
                dropboxStorageService.delete(dropboxPath);
                throw new MaxImagesExceededException(MAX_IMAGES);
            }

            validateExtension(dropboxPath);

            byte[] imageBytes;
            try (InputStream in = dropboxStorageService.download(dropboxPath)) {
                imageBytes = in.readAllBytes();
            }

            validateMagicBytes(imageBytes);

            BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (original == null) {
                dropboxStorageService.delete(dropboxPath);
                throw new InvalidImageException("unreadable_image");
            }

            int width = original.getWidth();
            int height = original.getHeight();

            ByteArrayOutputStream thumbOut = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(imageBytes))
                    .size(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
                    .outputQuality(THUMBNAIL_QUALITY)
                    .outputFormat("jpg")
                    .toOutputStream(thumbOut);
            byte[] thumbnailBytes = thumbOut.toByteArray();

            String thumbnailPath = appFolder + "/thumbnails/" + listingId + "/" + UUID.randomUUID() + ".jpg";
            dropboxStorageService.upload(thumbnailPath, new ByteArrayInputStream(thumbnailBytes), thumbnailBytes.length);

            int displayOrder = (int) listingImageRepository
                    .findByListingIdOrderByDisplayOrderAsc(listingId)
                    .stream()
                    .count();

            ListingImageEntity image = new ListingImageEntity();
            image.setListing(listing);
            image.setDropboxPath(dropboxPath);
            image.setThumbnailPath(thumbnailPath);
            image.setDisplayOrder(displayOrder);
            listingImageRepository.save(image);

        } catch (InvalidImageException e) {
            try {
                dropboxStorageService.delete(dropboxPath);
            } catch (Exception ex) {
                log.warn("Failed to clean up invalid image from Dropbox: {}", dropboxPath, ex);
            }
            log.warn("Image validation failed for {}: {}", dropboxPath, e.getMessage());
        } catch (MaxImagesExceededException e) {
            log.warn("Max images exceeded for listing {}: {}", listingId, e.getMessage());
        } catch (ListingNotFoundException e) {
            try {
                dropboxStorageService.delete(dropboxPath);
            } catch (Exception ex) {
                log.warn("Failed to clean up image from Dropbox for deleted listing: {}", dropboxPath, ex);
            }
            log.warn("Listing not found for image processing: {}", listingId);
        } catch (Exception e) {
            log.error("Unexpected error processing image {} for listing {}", dropboxPath, listingId, e);
        }
    }

    private void validateExtension(String dropboxPath) {
        String lower = dropboxPath.toLowerCase();
        boolean valid = ALLOWED_EXTENSIONS.stream().anyMatch(lower::endsWith);
        if (!valid) {
            throw new InvalidImageException("unsupported_format");
        }
    }

    private void validateMagicBytes(byte[] header) {
        if (header.length < 12) {
            throw new InvalidImageException("file_too_small");
        }

        boolean jpeg = header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF;
        if (jpeg) return;

        boolean png = header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47;
        if (png) return;

        boolean webp = header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50;
        if (webp) return;

        throw new InvalidImageException("invalid_magic_bytes");
    }
}
