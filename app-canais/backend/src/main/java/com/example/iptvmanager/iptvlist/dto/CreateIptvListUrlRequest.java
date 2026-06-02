package com.example.iptvmanager.iptvlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateIptvListUrlRequest(
        @NotBlank
        @Size(max = 150)
        String name,

        String description,

        @NotBlank
        @Pattern(regexp = "^https?://.+", message = "deve comecar com http:// ou https://")
        String sourceUrl
) {
}
