package com.example.iptvmanager.iptvlist.mapper;

import com.example.iptvmanager.iptvlist.dto.IptvListDTO;
import com.example.iptvmanager.iptvlist.entity.IptvList;

public final class IptvListMapper {

    private IptvListMapper() {
    }

    public static IptvListDTO toDTO(IptvList list) {
        return new IptvListDTO(
                list.getId(),
                list.getName(),
                list.getDescription(),
                list.getSourceType(),
                list.getSourceUrl(),
                list.getOriginalFileName(),
                list.getStatus(),
                list.getTotalChannels(),
                list.getLastImportAt(),
                list.getErrorMessage(),
                list.getOwner().getId(),
                list.getOwner().getName(),
                list.getCreatedAt(),
                list.getUpdatedAt()
        );
    }
}
