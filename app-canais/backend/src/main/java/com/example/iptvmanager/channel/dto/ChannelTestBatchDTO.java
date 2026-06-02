package com.example.iptvmanager.channel.dto;

public record ChannelTestBatchDTO(
        String batchId,
        int totalChannels,
        String status,
        String message
) {
}
