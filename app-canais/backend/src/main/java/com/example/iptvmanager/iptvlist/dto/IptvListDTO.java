package com.example.iptvmanager.iptvlist.dto;

import java.time.LocalDateTime;

import com.example.iptvmanager.iptvlist.entity.IptvListSourceType;
import com.example.iptvmanager.iptvlist.entity.IptvListStatus;

public record IptvListDTO(
        Long id,
        String name,
        String description,
        IptvListSourceType sourceType,
        String sourceUrl,
        String originalFileName,
        IptvListStatus status,
        Integer totalChannels,
        LocalDateTime lastImportAt,
        String errorMessage,
        Long ownerId,
        String ownerName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
