package com.example.iptvmanager.channel.mapper;

import com.example.iptvmanager.channel.dto.ChannelDTO;
import com.example.iptvmanager.channel.entity.Channel;

public final class ChannelMapper {

    private ChannelMapper() {
    }

    public static ChannelDTO toDTO(Channel channel) {
        return new ChannelDTO(
                channel.getId(),
                channel.getName(),
                channel.getStreamUrl(),
                channel.getGroupTitle(),
                channel.getLogoUrl(),
                channel.getTvgId(),
                channel.getTvgName(),
                channel.getDuration(),
                channel.getFavorite(),
                channel.getActive(),
                channel.getTestStatus(),
                channel.getTestHttpStatus(),
                channel.getTestMessage(),
                channel.getLastTestAt(),
                channel.getIptvList().getId(),
                channel.getIptvList().getName(),
                channel.getOwner().getId(),
                channel.getCreatedAt(),
                channel.getUpdatedAt()
        );
    }
}
