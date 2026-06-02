package com.example.iptvmanager.channel.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import com.example.iptvmanager.channel.dto.ChannelTestBatchDTO;
import com.example.iptvmanager.channel.dto.ChannelTestResultDTO;
import com.example.iptvmanager.channel.entity.Channel;
import com.example.iptvmanager.channel.repository.ChannelRepository;
import com.example.iptvmanager.exception.ResourceNotFoundException;
import com.example.iptvmanager.user.entity.User;
import com.example.iptvmanager.user.entity.UserRole;

@Service
public class ChannelTestService {

    private static final int BATCH_CONCURRENCY = 20;

    private final ChannelRepository channelRepository;
    private final TaskExecutor batchExecutor;
    private final TransactionTemplate transactionTemplate;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public ChannelTestService(
            ChannelRepository channelRepository,
            @Qualifier("channelTestBatchExecutor") TaskExecutor batchExecutor,
            TransactionTemplate transactionTemplate
    ) {
        this.channelRepository = channelRepository;
        this.batchExecutor = batchExecutor;
        this.transactionTemplate = transactionTemplate;
    }

    public ChannelTestResultDTO test(Long channelId, User currentUser) {
        ChannelTarget target = loadTarget(channelId, currentUser);
        ChannelTestResultDTO result = testUrl(target.id(), target.streamUrl());
        saveResult(result);
        return result;
    }

    public ChannelTestBatchDTO testBatch(String term, Long listId, String group, String testStatus, User currentUser) {
        User owner = currentUser.getRole() == UserRole.ADMIN ? null : currentUser;
        List<Long> ids = channelRepository.findIdsForBatch(
                owner,
                listId,
                clean(group),
                clean(term),
                normalizeTestStatus(testStatus)
        );
        String batchId = UUID.randomUUID().toString();
        if (ids.isEmpty()) {
            return new ChannelTestBatchDTO(batchId, 0, "EMPTY", "Nenhum canal encontrado para testar");
        }

        batchExecutor.execute(() -> runBatch(ids, currentUser));
        return new ChannelTestBatchDTO(batchId, ids.size(), "STARTED", "Teste em lote iniciado");
    }

    private void runBatch(List<Long> ids, User currentUser) {
        ExecutorService pool = Executors.newFixedThreadPool(BATCH_CONCURRENCY);
        CompletionService<Void> completionService = new ExecutorCompletionService<>(pool);
        int submitted = 0;
        int completed = 0;

        try {
            while (submitted < ids.size() && submitted - completed < BATCH_CONCURRENCY) {
                submitTest(completionService, ids.get(submitted++), currentUser);
            }

            while (completed < submitted) {
                try {
                    completionService.take().get();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception ignored) {
                    // Cada canal salva o proprio resultado; falhas isoladas nao devem parar o lote.
                }
                completed++;
                while (submitted < ids.size() && submitted - completed < BATCH_CONCURRENCY) {
                    submitTest(completionService, ids.get(submitted++), currentUser);
                }
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private void submitTest(CompletionService<Void> completionService, Long channelId, User currentUser) {
        completionService.submit(() -> {
            test(channelId, currentUser);
            return null;
        });
    }

    private ChannelTestResultDTO testUrl(Long channelId, String streamUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(streamUrl))
                    .timeout(Duration.ofSeconds(8))
                    .header("Range", "bytes=0-0")
                    .header("User-Agent", "IPTV-Manager/1.0")
                    .GET()
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();

            if ((status >= 200 && status < 400) || status == 401 || status == 403) {
                String message = status == 401 || status == 403
                        ? "Stream respondeu, mas exige autorizacao no servidor de origem"
                        : "Stream respondeu com sucesso";
                return new ChannelTestResultDTO(channelId, "ONLINE", status, message);
            }

            return new ChannelTestResultDTO(channelId, "OFFLINE", status, "Stream retornou HTTP " + status);
        } catch (IllegalArgumentException ex) {
            return new ChannelTestResultDTO(channelId, "INVALID", null, "URL invalida");
        } catch (IOException ex) {
            return new ChannelTestResultDTO(channelId, "OFFLINE", null, "Nao foi possivel conectar ao stream");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new ChannelTestResultDTO(channelId, "UNKNOWN", null, "Teste interrompido");
        }
    }

    private ChannelTarget loadTarget(Long channelId, User currentUser) {
        return transactionTemplate.execute(status -> {
            Channel channel = findAccessible(channelId, currentUser);
            return new ChannelTarget(channel.getId(), channel.getStreamUrl());
        });
    }

    private void saveResult(ChannelTestResultDTO result) {
        transactionTemplate.executeWithoutResult(status -> channelRepository.findById(result.channelId()).ifPresent(channel -> {
            channel.setTestStatus(result.status());
            channel.setTestHttpStatus(result.httpStatus());
            channel.setTestMessage(limit(result.message(), 255));
            channel.setLastTestAt(LocalDateTime.now());
            channelRepository.save(channel);
        }));
    }

    @Transactional(readOnly = true)
    private Channel findAccessible(Long id, User currentUser) {
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Canal nao encontrado"));
        if (currentUser.getRole() != UserRole.ADMIN && !channel.getOwner().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Canal nao encontrado");
        }
        return channel;
    }

    private String normalizeTestStatus(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record ChannelTarget(Long id, String streamUrl) {
    }
}
