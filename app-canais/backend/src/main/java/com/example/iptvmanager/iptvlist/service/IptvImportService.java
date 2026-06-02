package com.example.iptvmanager.iptvlist.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.iptvmanager.channel.entity.Channel;
import com.example.iptvmanager.channel.repository.ChannelRepository;
import com.example.iptvmanager.exception.BadRequestException;
import com.example.iptvmanager.iptvlist.dto.IptvListDTO;
import com.example.iptvmanager.iptvlist.entity.IptvList;
import com.example.iptvmanager.iptvlist.entity.IptvListSourceType;
import com.example.iptvmanager.iptvlist.entity.IptvListStatus;
import com.example.iptvmanager.iptvlist.mapper.IptvListMapper;
import com.example.iptvmanager.iptvlist.repository.IptvListRepository;
import com.example.iptvmanager.parser.dto.ParsedChannelDTO;
import com.example.iptvmanager.parser.service.M3uParserService;
import com.example.iptvmanager.user.entity.User;

@Service
public class IptvImportService {

    private final IptvListAccessService iptvListAccessService;
    private final IptvListRepository iptvListRepository;
    private final ChannelRepository channelRepository;
    private final M3uParserService parserService;
    private final TaskExecutor importExecutor;
    private final TransactionTemplate transactionTemplate;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public IptvImportService(
            IptvListAccessService iptvListAccessService,
            IptvListRepository iptvListRepository,
            ChannelRepository channelRepository,
            M3uParserService parserService,
            @Qualifier("iptvImportExecutor") TaskExecutor importExecutor,
            TransactionTemplate transactionTemplate
    ) {
        this.iptvListAccessService = iptvListAccessService;
        this.iptvListRepository = iptvListRepository;
        this.channelRepository = channelRepository;
        this.parserService = parserService;
        this.importExecutor = importExecutor;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional
    public IptvListDTO importList(Long listId, User currentUser) {
        IptvList list = iptvListAccessService.findAccessible(listId, currentUser);
        IptvListDTO queued = markProcessing(list);
        importExecutor.execute(() -> runImportJob(listId, false));
        return queued;
    }

    @Transactional
    public IptvListDTO refreshList(Long listId, User currentUser) {
        IptvList list = iptvListAccessService.findAccessible(listId, currentUser);
        if (list.getSourceType() != IptvListSourceType.URL) {
            throw new BadRequestException("Apenas listas cadastradas por URL podem ser atualizadas");
        }
        IptvListDTO queued = markProcessing(list);
        importExecutor.execute(() -> runImportJob(listId, true));
        return queued;
    }

    private void runImportJob(Long listId, boolean preserveFavorites) {
        transactionTemplate.executeWithoutResult(status -> {
            IptvList list = iptvListRepository.findById(listId).orElse(null);
            if (list == null) {
                return;
            }
            if (preserveFavorites && list.getSourceType() != IptvListSourceType.URL) {
                markError(list, "Apenas listas cadastradas por URL podem ser atualizadas");
                return;
            }
            if (list.getSourceType() == IptvListSourceType.FILE) {
                importFromFile(list, preserveFavorites);
            } else {
                importFromUrl(list, preserveFavorites);
            }
        });
    }

    private IptvListDTO markProcessing(IptvList list) {
        list.setStatus(IptvListStatus.PROCESSING);
        list.setErrorMessage(null);
        return IptvListMapper.toDTO(iptvListRepository.saveAndFlush(list));
    }

    private IptvListDTO importFromFile(IptvList list, boolean preserveFavorites) {
        if (list.getSourceUrl() == null) {
            throw new BadRequestException("Arquivo da lista nao encontrado");
        }
        try {
            return importContent(list, Files.readString(Path.of(list.getSourceUrl())), preserveFavorites);
        } catch (IOException ex) {
            return markError(list, "Nao foi possivel ler o arquivo da lista");
        }
    }

    private IptvListDTO importFromUrl(IptvList list, boolean preserveFavorites) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(list.getSourceUrl()))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return markError(list, "URL retornou status HTTP " + response.statusCode());
            }
            return importContent(list, response.body(), preserveFavorites);
        } catch (IllegalArgumentException ex) {
            return markError(list, "URL da lista invalida");
        } catch (IOException ex) {
            return markError(list, "Nao foi possivel baixar a lista");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return markError(list, "Importacao interrompida");
        }
    }

    private IptvListDTO importContent(IptvList list, String content, boolean preserveFavorites) {
        try {
            Set<String> favoriteUrls = preserveFavorites ? channelRepository.findFavoriteStreamUrlsByIptvList(list) : Set.of();
            List<ParsedChannelDTO> parsedChannels = parserService.parse(content);

            channelRepository.deleteByIptvList(list);
            List<Channel> channels = parsedChannels.stream()
                    .map(parsed -> toChannel(parsed, list, favoriteUrls.contains(parsed.streamUrl())))
                    .toList();
            channelRepository.saveAll(channels);

            list.setTotalChannels(channels.size());
            list.setLastImportAt(LocalDateTime.now());
            list.setStatus(IptvListStatus.IMPORTED);
            list.setErrorMessage(null);
            return IptvListMapper.toDTO(iptvListRepository.save(list));
        } catch (RuntimeException ex) {
            return markError(list, "Erro ao importar lista");
        }
    }

    private Channel toChannel(ParsedChannelDTO parsed, IptvList list, boolean favorite) {
        Channel channel = new Channel();
        channel.setName(parsed.name());
        channel.setStreamUrl(parsed.streamUrl());
        channel.setGroupTitle(parsed.groupTitle());
        channel.setLogoUrl(parsed.logoUrl());
        channel.setTvgId(parsed.tvgId());
        channel.setTvgName(parsed.tvgName());
        channel.setDuration(parsed.duration());
        channel.setFavorite(favorite);
        channel.setActive(true);
        channel.setIptvList(list);
        channel.setOwner(list.getOwner());
        return channel;
    }

    private IptvListDTO markError(IptvList list, String message) {
        list.setStatus(IptvListStatus.ERROR);
        list.setErrorMessage(message);
        return IptvListMapper.toDTO(iptvListRepository.save(list));
    }
}
