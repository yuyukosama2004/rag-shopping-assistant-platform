package com.biyesheji.order.service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class AiStreamingContentFilter {
    private static final String MASK = "[内容已过滤]";

    private final List<String> keywords;
    private final int maxKeywordLength;
    private final StringBuilder pending = new StringBuilder();

    public AiStreamingContentFilter(String configuredKeywords) {
        this.keywords = configuredKeywords == null ? List.of() : Arrays.stream(configuredKeywords.split("[,，\\n]"))
                .map(String::trim)
                .filter(keyword -> !keyword.isEmpty())
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .distinct()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
        this.maxKeywordLength = keywords.stream().mapToInt(String::length).max().orElse(0);
    }

    public String accept(String chunk) {
        if (keywords.isEmpty()) return chunk;
        pending.append(chunk);
        return drain(false);
    }

    public String finish() {
        return drain(true);
    }

    private String drain(boolean flush) {
        StringBuilder safe = new StringBuilder();
        while (!pending.isEmpty() && (flush || pending.length() >= maxKeywordLength)) {
            String match = matchingKeyword();
            if (match != null) {
                safe.append(MASK);
                pending.delete(0, match.length());
            } else {
                safe.append(pending.charAt(0));
                pending.deleteCharAt(0);
            }
        }
        return safe.toString();
    }

    private String matchingKeyword() {
        for (String keyword : keywords) {
            if (pending.length() >= keyword.length()
                    && pending.substring(0, keyword.length()).toLowerCase(Locale.ROOT).equals(keyword)) {
                return keyword;
            }
        }
        return null;
    }
}
