package com.example.iptvmanager.dashboard.dto;

import java.util.List;

import com.example.iptvmanager.channel.dto.ChannelDTO;
import com.example.iptvmanager.iptvlist.dto.IptvListDTO;

public record DashboardDTO(
        Long totalLists,
        Long totalChannels,
        Long totalGroups,
        Long totalFavorites,
        List<IptvListDTO> recentLists,
        List<ChannelDTO> recentChannels
) {
}
