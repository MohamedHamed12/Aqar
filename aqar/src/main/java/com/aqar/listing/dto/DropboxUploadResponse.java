package com.aqar.listing.dto;

public record DropboxUploadResponse(
        String dropboxPath,
        String fileId
) {
}
