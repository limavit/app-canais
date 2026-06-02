package com.example.iptvmanager.iptvlist.service;

import org.springframework.stereotype.Service;

import com.example.iptvmanager.exception.ResourceNotFoundException;
import com.example.iptvmanager.iptvlist.entity.IptvList;
import com.example.iptvmanager.iptvlist.repository.IptvListRepository;
import com.example.iptvmanager.user.entity.User;
import com.example.iptvmanager.user.entity.UserRole;

@Service
public class IptvListAccessService {

    private final IptvListRepository iptvListRepository;

    public IptvListAccessService(IptvListRepository iptvListRepository) {
        this.iptvListRepository = iptvListRepository;
    }

    public IptvList findAccessible(Long id, User currentUser) {
        if (currentUser.getRole() == UserRole.ADMIN) {
            return iptvListRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Lista IPTV nao encontrada"));
        }
        return iptvListRepository.findByIdAndOwner(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Lista IPTV nao encontrada"));
    }
}
