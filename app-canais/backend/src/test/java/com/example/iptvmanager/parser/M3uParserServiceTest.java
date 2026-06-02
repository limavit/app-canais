package com.example.iptvmanager.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.example.iptvmanager.parser.service.M3uParserService;

class M3uParserServiceTest {

    private final M3uParserService parserService = new M3uParserService();

    @Test
    void shouldParseChannelsFromM3uContent() {
        var channels = parserService.parse("""
                #EXTM3U
                #EXTINF:-1 tvg-id="canal1" tvg-name="Canal 1" tvg-logo="https://logo.com/canal1.png" group-title="Filmes",Canal 1
                https://servidor.com/live/canal1.m3u8
                #EXTINF:-1 tvg-id="canal2" tvg-name="Canal 2" tvg-logo="https://logo.com/canal2.png" group-title="Esportes",Canal 2
                http://servidor.com/live/canal2.m3u8
                """);

        assertThat(channels).hasSize(2);
        assertThat(channels.getFirst().name()).isEqualTo("Canal 1");
        assertThat(channels.getFirst().streamUrl()).isEqualTo("https://servidor.com/live/canal1.m3u8");
        assertThat(channels.getFirst().groupTitle()).isEqualTo("Filmes");
        assertThat(channels.getFirst().logoUrl()).isEqualTo("https://logo.com/canal1.png");
        assertThat(channels.getFirst().tvgId()).isEqualTo("canal1");
        assertThat(channels.getFirst().tvgName()).isEqualTo("Canal 1");
        assertThat(channels.getFirst().duration()).isEqualTo("-1");
    }

    @Test
    void shouldUseFallbackValues() {
        var channels = parserService.parse("""
                #EXTM3U
                #EXTINF:10 tvg-name="Nome TVG",
                https://example.com/stream.m3u8
                #EXTINF:-1,
                https://example.com/no-name.m3u8
                """);

        assertThat(channels).hasSize(2);
        assertThat(channels.get(0).name()).isEqualTo("Nome TVG");
        assertThat(channels.get(0).groupTitle()).isEqualTo("Sem categoria");
        assertThat(channels.get(0).logoUrl()).isNull();
        assertThat(channels.get(0).duration()).isEqualTo("10");
        assertThat(channels.get(1).name()).isEqualTo("Canal sem nome");
    }

    @Test
    void shouldIgnoreInvalidUrlsAndDuplicatedStreams() {
        var channels = parserService.parse("""
                #EXTM3U
                #EXTINF:-1,Canal invalido
                ftp://example.com/stream.m3u8
                #EXTINF:-1,Canal A
                https://example.com/a.m3u8
                #EXTINF:-1,Canal A duplicado
                https://example.com/a.m3u8
                """);

        assertThat(channels).hasSize(1);
        assertThat(channels.getFirst().name()).isEqualTo("Canal A");
    }

    @Test
    void shouldSkipCommentsBetweenExtinfAndStreamUrl() {
        var channels = parserService.parse("""
                #EXTM3U
                #EXTINF:-1 group-title="Noticias",Canal News
                # comentario intermediario

                https://example.com/news.m3u8
                """);

        assertThat(channels).hasSize(1);
        assertThat(channels.getFirst().groupTitle()).isEqualTo("Noticias");
    }
}
