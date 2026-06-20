package com.aqar.listing.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmUploadRequest(
        @NotBlank String dropboxPath
) {
}
