package com.example.iptvmanager.channel.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.iptvmanager.auth.security.UserPrincipal;
import com.example.iptvmanager.channel.dto.ChannelDTO;
import com.example.iptvmanager.channel.dto.ChannelTestBatchDTO;
import com.example.iptvmanager.channel.dto.ChannelTestResultDTO;
import com.example.iptvmanager.channel.dto.UpdateChannelRequest;
import com.example.iptvmanager.channel.service.ChannelService;
import com.example.iptvmanager.channel.service.ChannelTestService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/channels")
public class ChannelController {

    private final ChannelService channelService;
    private final ChannelTestService channelTestService;

    public ChannelController(ChannelService channelService, ChannelTestService channelTestService) {
        this.channelService = channelService;
        this.channelTestService = channelTestService;
    }

    @GetMapping
    public Page<ChannelDTO> findAll(
            @RequestParam(required = false) String term,
            @RequestParam(required = false) Long listId,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) Boolean favorite,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String testStatus,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return channelService.findAll(term, listId, group, favorite, active, testStatus, principal.getUser(), pageable);
    }

    @GetMapping("/{id}")
    public ChannelDTO findById(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return channelService.findById(id, principal.getUser());
    }

    @PutMapping("/{id}")
    public ChannelDTO update(@PathVariable Long id, @Valid @RequestBody UpdateChannelRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return channelService.update(id, request, principal.getUser());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        channelService.delete(id, principal.getUser());
    }

    @GetMapping("/search")
    public Page<ChannelDTO> search(
            @RequestParam String term,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return channelService.findAll(term, null, null, null, true, null, principal.getUser(), pageable);
    }

    @GetMapping("/groups")
    public List<String> groups(@RequestParam(required = false) Long listId, @AuthenticationPrincipal UserPrincipal principal) {
        return channelService.groups(listId, principal.getUser());
    }

    @GetMapping("/group/{groupName}")
    public Page<ChannelDTO> group(
            @PathVariable String groupName,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return channelService.findAll(null, null, groupName, null, true, null, principal.getUser(), pageable);
    }

    @PostMapping("/{id}/favorite")
    public ChannelDTO favorite(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return channelService.favorite(id, principal.getUser(), true);
    }

    @DeleteMapping("/{id}/favorite")
    public ChannelDTO unfavorite(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return channelService.favorite(id, principal.getUser(), false);
    }

    @PostMapping("/{id}/test")
    public ChannelTestResultDTO test(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return channelTestService.test(id, principal.getUser());
    }

    @PostMapping("/test-batch")
    public ChannelTestBatchDTO testBatch(
            @RequestParam(required = false) String term,
            @RequestParam(required = false) Long listId,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String testStatus,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return channelTestService.testBatch(term, listId, group, testStatus, principal.getUser());
    }

    @GetMapping("/favorites")
    public Page<ChannelDTO> favorites(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return channelService.findAll(null, null, null, true, true, null, principal.getUser(), pageable);
    }
}
