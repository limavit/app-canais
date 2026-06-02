package com.example.iptvmanager.parser.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.iptvmanager.parser.dto.ParsedChannelDTO;

@Service
public class M3uParserService {

    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("([\\w-]+)=\"([^\"]*)\"");

    public List<ParsedChannelDTO> parse(String content) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }

        String[] lines = content.split("\\R");
        List<ParsedChannelDTO> channels = new ArrayList<>();
        Set<String> seenStreamUrls = new HashSet<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.startsWith("#EXTINF")) {
                continue;
            }

            String streamUrl = nextStreamUrl(lines, i + 1);
            if (!isValidUrl(streamUrl) || !seenStreamUrls.add(streamUrl)) {
                continue;
            }

            channels.add(parsedChannel(line, streamUrl));
        }

        return channels;
    }

    private ParsedChannelDTO parsedChannel(String extinf, String streamUrl) {
        Map<String, String> attributes = attributes(extinf);
        String tvgName = blankToNull(attributes.get("tvg-name"));
        String explicitName = nameAfterComma(extinf);
        String name = firstText(explicitName, tvgName, "Canal sem nome");
        String groupTitle = firstText(attributes.get("group-title"), "Sem categoria");
        String logoUrl = blankToNull(firstText(attributes.get("tvg-logo"), attributes.get("logo")));

        return new ParsedChannelDTO(
                name,
                streamUrl,
                groupTitle,
                logoUrl,
                blankToNull(attributes.get("tvg-id")),
                tvgName,
                duration(extinf),
                extinf
        );
    }

    private Map<String, String> attributes(String extinf) {
        Map<String, String> attributes = new HashMap<>();
        Matcher matcher = ATTRIBUTE_PATTERN.matcher(extinf);
        while (matcher.find()) {
            attributes.put(matcher.group(1), matcher.group(2));
        }
        return attributes;
    }

    private String duration(String extinf) {
        int start = extinf.indexOf(':');
        if (start < 0 || start + 1 >= extinf.length()) {
            return null;
        }

        String remainder = extinf.substring(start + 1);
        int space = remainder.indexOf(' ');
        int comma = remainder.indexOf(',');
        int end = minPositive(space, comma);
        String duration = end >= 0 ? remainder.substring(0, end) : remainder;
        return blankToNull(duration);
    }

    private int minPositive(int first, int second) {
        if (first < 0) {
            return second;
        }
        if (second < 0) {
            return first;
        }
        return Math.min(first, second);
    }

    private String nameAfterComma(String extinf) {
        int comma = extinf.lastIndexOf(',');
        if (comma < 0 || comma + 1 >= extinf.length()) {
            return null;
        }
        return blankToNull(extinf.substring(comma + 1).trim());
    }

    private String nextStreamUrl(String[] lines, int startIndex) {
        for (int i = startIndex; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!StringUtils.hasText(line)) {
                continue;
            }
            if (line.startsWith("#")) {
                continue;
            }
            return line;
        }
        return null;
    }

    private boolean isValidUrl(String streamUrl) {
        return streamUrl != null && (streamUrl.startsWith("http://") || streamUrl.startsWith("https://"));
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
