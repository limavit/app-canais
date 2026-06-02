package com.example.iptvmanager.iptvlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateIptvListRequest(
        @NotBlank
        @Size(max = 150)
        String name,

        String description
) {
}
