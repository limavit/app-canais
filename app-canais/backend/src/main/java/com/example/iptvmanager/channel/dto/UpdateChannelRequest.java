package com.example.iptvmanager.channel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateChannelRequest(
        @NotBlank
        @Size(max = 255)
        String name,

        @NotBlank
        @Pattern(regexp = "^https?://.+", message = "deve comecar com http:// ou https://")
        String streamUrl,

        String groupTitle,
        String logoUrl,
        String tvgId,
        String tvgName,
        String duration,
        Boolean active
) {
}
