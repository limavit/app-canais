package com.example.iptvmanager.iptvlist.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.iptvmanager.auth.security.UserPrincipal;
import com.example.iptvmanager.channel.dto.ChannelDTO;
import com.example.iptvmanager.channel.service.ChannelService;
import com.example.iptvmanager.iptvlist.dto.CreateIptvListUrlRequest;
import com.example.iptvmanager.iptvlist.dto.IptvListDTO;
import com.example.iptvmanager.iptvlist.dto.UpdateIptvListRequest;
import com.example.iptvmanager.iptvlist.service.IptvImportService;
import com.example.iptvmanager.iptvlist.service.IptvListService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/iptv-lists")
public class IptvListController {

    private final IptvListService iptvListService;
    private final IptvImportService iptvImportService;
    private final ChannelService channelService;

    public IptvListController(IptvListService iptvListService, IptvImportService iptvImportService, ChannelService channelService) {
        this.iptvListService = iptvListService;
        this.iptvImportService = iptvImportService;
        this.channelService = channelService;
    }

    @PostMapping("/url")
    public IptvListDTO createFromUrl(
            @Valid @RequestBody CreateIptvListUrlRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return iptvListService.createFromUrl(request, principal.getUser());
    }

    @PostMapping("/upload")
    public IptvListDTO createFromUpload(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return iptvListService.createFromUpload(name, description, file, principal.getUser());
    }

    @GetMapping
    public List<IptvListDTO> findAll(@AuthenticationPrincipal UserPrincipal principal) {
        return iptvListService.findAll(principal.getUser());
    }

    @GetMapping("/{id}")
    public IptvListDTO findById(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return iptvListService.findById(id, principal.getUser());
    }

    @PutMapping("/{id}")
    public IptvListDTO update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIptvListRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return iptvListService.update(id, request, principal.getUser());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        iptvListService.delete(id, principal.getUser());
    }

    @PostMapping("/{id}/import")
    public IptvListDTO importList(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return iptvImportService.importList(id, principal.getUser());
    }

    @PostMapping("/{id}/refresh")
    public IptvListDTO refreshList(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return iptvImportService.refreshList(id, principal.getUser());
    }

    @GetMapping("/{id}/channels")
    public List<ChannelDTO> channels(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return channelService.findByList(id, principal.getUser());
    }
}
