package com.example.iptvmanager.dashboard.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.iptvmanager.channel.mapper.ChannelMapper;
import com.example.iptvmanager.channel.repository.ChannelRepository;
import com.example.iptvmanager.dashboard.dto.DashboardDTO;
import com.example.iptvmanager.iptvlist.mapper.IptvListMapper;
import com.example.iptvmanager.iptvlist.repository.IptvListRepository;
import com.example.iptvmanager.user.entity.User;
import com.example.iptvmanager.user.entity.UserRole;

@Service
public class DashboardService {

    private final IptvListRepository iptvListRepository;
    private final ChannelRepository channelRepository;

    public DashboardService(IptvListRepository iptvListRepository, ChannelRepository channelRepository) {
        this.iptvListRepository = iptvListRepository;
        this.channelRepository = channelRepository;
    }

    @Transactional(readOnly = true)
    public DashboardDTO dashboard(User currentUser) {
        boolean admin = currentUser.getRole() == UserRole.ADMIN;

        long totalLists = admin ? iptvListRepository.count() : iptvListRepository.countByOwner(currentUser);
        long totalChannels = admin ? channelRepository.count() : channelRepository.countByOwner(currentUser);
        long totalGroups = admin ? channelRepository.countDistinctGroupTitle() : channelRepository.countDistinctGroupTitleByOwner(currentUser);
        long totalFavorites = admin ? channelRepository.countByFavoriteTrue() : channelRepository.countByOwnerAndFavoriteTrue(currentUser);

        var recentLists = (admin ? iptvListRepository.findTop5ByOrderByCreatedAtDesc() : iptvListRepository.findTop5ByOwnerOrderByCreatedAtDesc(currentUser))
                .stream()
                .map(IptvListMapper::toDTO)
                .toList();
        var recentChannels = (admin ? channelRepository.findTop5ByOrderByCreatedAtDesc() : channelRepository.findTop5ByOwnerOrderByCreatedAtDesc(currentUser))
                .stream()
                .map(ChannelMapper::toDTO)
                .toList();

        return new DashboardDTO(totalLists, totalChannels, totalGroups, totalFavorites, recentLists, recentChannels);
    }
}
