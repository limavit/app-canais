package com.example.iptvmanager.parser.dto;

public record ParsedChannelDTO(
        String name,
        String streamUrl,
        String groupTitle,
        String logoUrl,
        String tvgId,
        String tvgName,
        String duration,
        String rawExtinf
) {
}
