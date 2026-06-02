package com.example.iptvmanager.channel.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.iptvmanager.channel.dto.ChannelDTO;
import com.example.iptvmanager.channel.dto.UpdateChannelRequest;
import com.example.iptvmanager.channel.entity.Channel;
import com.example.iptvmanager.channel.mapper.ChannelMapper;
import com.example.iptvmanager.channel.repository.ChannelRepository;
import com.example.iptvmanager.exception.ResourceNotFoundException;
import com.example.iptvmanager.iptvlist.entity.IptvList;
import com.example.iptvmanager.iptvlist.service.IptvListAccessService;
import com.example.iptvmanager.user.entity.User;
import com.example.iptvmanager.user.entity.UserRole;

@Service
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final IptvListAccessService iptvListAccessService;

    public ChannelService(ChannelRepository channelRepository, IptvListAccessService iptvListAccessService) {
        this.channelRepository = channelRepository;
        this.iptvListAccessService = iptvListAccessService;
    }

    @Transactional(readOnly = true)
    public Page<ChannelDTO> findAll(String term, Long listId, String group, Boolean favorite, Boolean active, String testStatus, User currentUser, Pageable pageable) {
        Specification<Channel> spec = accessibleTo(currentUser)
                .and(nameContains(term))
                .and(inList(listId))
                .and(inGroup(group))
                .and(favoriteEquals(favorite))
                .and(activeEquals(active))
                .and(testStatusEquals(testStatus));

        return channelRepository.findAll(spec, pageable).map(ChannelMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public List<ChannelDTO> findByList(Long listId, User currentUser) {
        IptvList list = iptvListAccessService.findAccessible(listId, currentUser);
        return channelRepository.findByIptvList(list).stream().map(ChannelMapper::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public ChannelDTO findById(Long id, User currentUser) {
        return ChannelMapper.toDTO(findAccessible(id, currentUser));
    }

    @Transactional
    public ChannelDTO update(Long id, UpdateChannelRequest request, User currentUser) {
        Channel channel = findAccessible(id, currentUser);
        channel.setName(request.name().trim());
        channel.setStreamUrl(request.streamUrl().trim());
        channel.setGroupTitle(textOrDefault(request.groupTitle(), "Sem categoria"));
        channel.setLogoUrl(textOrNull(request.logoUrl()));
        channel.setTvgId(textOrNull(request.tvgId()));
        channel.setTvgName(textOrNull(request.tvgName()));
        channel.setDuration(textOrNull(request.duration()));
        channel.setActive(request.active() == null || request.active());
        return ChannelMapper.toDTO(channelRepository.save(channel));
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        channelRepository.delete(findAccessible(id, currentUser));
    }

    @Transactional(readOnly = true)
    public List<String> groups(Long listId, User currentUser) {
        List<String> groups;
        if (listId != null) {
            groups = isAdmin(currentUser)
                    ? channelRepository.findGroupsByListId(listId)
                    : channelRepository.findGroupsByOwnerAndListId(currentUser, listId);
        } else {
            groups = isAdmin(currentUser) ? channelRepository.findAllGroups() : channelRepository.findGroupsByOwner(currentUser);
        }
        return groups.stream().filter(StringUtils::hasText).toList();
    }

    @Transactional
    public ChannelDTO favorite(Long id, User currentUser, boolean favorite) {
        Channel channel = findAccessible(id, currentUser);
        channel.setFavorite(favorite);
        return ChannelMapper.toDTO(channelRepository.save(channel));
    }

    private Channel findAccessible(Long id, User currentUser) {
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Canal nao encontrado"));
        if (!isAdmin(currentUser) && !channel.getOwner().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Canal nao encontrado");
        }
        return channel;
    }

    private Specification<Channel> accessibleTo(User user) {
        return (root, query, cb) -> isAdmin(user) ? cb.conjunction() : cb.equal(root.get("owner"), user);
    }

    private Specification<Channel> nameContains(String term) {
        return (root, query, cb) -> StringUtils.hasText(term)
                ? cb.like(cb.lower(root.get("name")), "%" + term.trim().toLowerCase() + "%")
                : cb.conjunction();
    }

    private Specification<Channel> inList(Long listId) {
        return (root, query, cb) -> listId != null ? cb.equal(root.get("iptvList").get("id"), listId) : cb.conjunction();
    }

    private Specification<Channel> inGroup(String group) {
        return (root, query, cb) -> StringUtils.hasText(group) ? cb.equal(root.get("groupTitle"), group.trim()) : cb.conjunction();
    }

    private Specification<Channel> favoriteEquals(Boolean favorite) {
        return (root, query, cb) -> favorite != null ? cb.equal(root.get("favorite"), favorite) : cb.conjunction();
    }

    private Specification<Channel> activeEquals(Boolean active) {
        return (root, query, cb) -> active != null ? cb.equal(root.get("active"), active) : cb.conjunction();
    }

    private Specification<Channel> testStatusEquals(String testStatus) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(testStatus)) {
                return cb.conjunction();
            }
            String normalized = testStatus.trim().toUpperCase();
            if ("UNTESTED".equals(normalized)) {
                return cb.isNull(root.get("testStatus"));
            }
            return cb.equal(root.get("testStatus"), normalized);
        };
    }

    private boolean isAdmin(User user) {
        return user.getRole() == UserRole.ADMIN;
    }

    private String textOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String textOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
