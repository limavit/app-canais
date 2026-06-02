package com.example.iptvmanager.channel.dto;

import java.time.LocalDateTime;

public record ChannelDTO(
        Long id,
        String name,
        String streamUrl,
        String groupTitle,
        String logoUrl,
        String tvgId,
        String tvgName,
        String duration,
        Boolean favorite,
        Boolean active,
        String testStatus,
        Integer testHttpStatus,
        String testMessage,
        LocalDateTime lastTestAt,
        Long iptvListId,
        String iptvListName,
        Long ownerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
