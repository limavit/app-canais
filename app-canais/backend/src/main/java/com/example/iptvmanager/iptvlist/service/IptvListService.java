package com.example.iptvmanager.iptvlist.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.iptvmanager.exception.BadRequestException;
import com.example.iptvmanager.exception.ResourceNotFoundException;
import com.example.iptvmanager.iptvlist.dto.CreateIptvListUrlRequest;
import com.example.iptvmanager.iptvlist.dto.IptvListDTO;
import com.example.iptvmanager.iptvlist.dto.UpdateIptvListRequest;
import com.example.iptvmanager.iptvlist.entity.IptvList;
import com.example.iptvmanager.iptvlist.entity.IptvListSourceType;
import com.example.iptvmanager.iptvlist.entity.IptvListStatus;
import com.example.iptvmanager.iptvlist.mapper.IptvListMapper;
import com.example.iptvmanager.iptvlist.repository.IptvListRepository;
import com.example.iptvmanager.channel.repository.ChannelRepository;
import com.example.iptvmanager.user.entity.User;
import com.example.iptvmanager.user.entity.UserRole;

@Service
public class IptvListService {

    private final IptvListRepository iptvListRepository;
    private final ChannelRepository channelRepository;
    private final Path uploadDir;

    public IptvListService(
            IptvListRepository iptvListRepository,
            ChannelRepository channelRepository,
            @Value("${app.upload.dir}") String uploadDir
    ) {
        this.iptvListRepository = iptvListRepository;
        this.channelRepository = channelRepository;
        this.uploadDir = Path.of(uploadDir);
    }

    @Transactional
    public IptvListDTO createFromUrl(CreateIptvListUrlRequest request, User owner) {
        IptvList list = new IptvList();
        list.setName(request.name().trim());
        list.setDescription(cleanDescription(request.description()));
        list.setSourceType(IptvListSourceType.URL);
        list.setSourceUrl(request.sourceUrl().trim());
        list.setStatus(IptvListStatus.PENDING);
        list.setTotalChannels(0);
        list.setOwner(owner);

        return IptvListMapper.toDTO(iptvListRepository.save(list));
    }

    @Transactional
    public IptvListDTO createFromUpload(String name, String description, MultipartFile file, User owner) {
        validateUpload(file);

        IptvList list = new IptvList();
        list.setName(requireName(name));
        list.setDescription(cleanDescription(description));
        list.setSourceType(IptvListSourceType.FILE);
        list.setOriginalFileName(StringUtils.cleanPath(file.getOriginalFilename()));
        list.setStatus(IptvListStatus.PENDING);
        list.setTotalChannels(0);
        list.setOwner(owner);

        IptvList saved = iptvListRepository.save(list);
        String savedPath = storeFile(saved.getId(), file);
        saved.setSourceUrl(savedPath);

        return IptvListMapper.toDTO(iptvListRepository.save(saved));
    }

    @Transactional(readOnly = true)
    public List<IptvListDTO> findAll(User currentUser) {
        List<IptvList> lists = isAdmin(currentUser)
                ? iptvListRepository.findAllByOrderByCreatedAtDesc()
                : iptvListRepository.findByOwnerOrderByCreatedAtDesc(currentUser);

        return lists.stream().map(IptvListMapper::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public IptvListDTO findById(Long id, User currentUser) {
        return IptvListMapper.toDTO(findAccessible(id, currentUser));
    }

    @Transactional
    public IptvListDTO update(Long id, UpdateIptvListRequest request, User currentUser) {
        IptvList list = findAccessible(id, currentUser);
        list.setName(request.name().trim());
        list.setDescription(cleanDescription(request.description()));
        return IptvListMapper.toDTO(iptvListRepository.save(list));
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        IptvList list = findAccessible(id, currentUser);
        channelRepository.deleteByIptvList(list);
        iptvListRepository.delete(list);
    }

    private IptvList findAccessible(Long id, User currentUser) {
        if (isAdmin(currentUser)) {
            return iptvListRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Lista IPTV nao encontrada"));
        }

        return iptvListRepository.findByIdAndOwner(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Lista IPTV nao encontrada"));
    }

    private boolean isAdmin(User user) {
        return user.getRole() == UserRole.ADMIN;
    }

    private String requireName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BadRequestException("Nome da lista e obrigatorio");
        }
        if (name.trim().length() > 150) {
            throw new BadRequestException("Nome da lista deve ter no maximo 150 caracteres");
        }
        return name.trim();
    }

    private String cleanDescription(String description) {
        return StringUtils.hasText(description) ? description.trim() : null;
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Arquivo .m3u ou .m3u8 e obrigatorio");
        }

        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new BadRequestException("Arquivo deve ter extensao .m3u ou .m3u8");
        }

        String filename = StringUtils.cleanPath(originalFilename);
        if (!isM3uFile(filename)) {
            throw new BadRequestException("Arquivo deve ter extensao .m3u ou .m3u8");
        }
    }

    private boolean isM3uFile(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".m3u") || lower.endsWith(".m3u8");
    }

    private String storeFile(Long listId, MultipartFile file) {
        try {
            Files.createDirectories(uploadDir);
            String filename = StringUtils.cleanPath(file.getOriginalFilename());
            String extension = filename.substring(filename.lastIndexOf('.'));
            Path target = uploadDir.resolve("list-" + listId + "-" + UUID.randomUUID() + extension);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException ex) {
            throw new BadRequestException("Nao foi possivel salvar o arquivo da lista");
        }
    }
}
