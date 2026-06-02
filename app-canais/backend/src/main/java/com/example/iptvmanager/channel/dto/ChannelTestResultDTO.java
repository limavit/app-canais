package com.example.iptvmanager.channel.dto;

public record ChannelTestResultDTO(
        Long channelId,
        String status,
        Integer httpStatus,
        String message
) {
}
