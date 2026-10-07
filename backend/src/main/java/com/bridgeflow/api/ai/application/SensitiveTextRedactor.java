package com.bridgeflow.api.ai.application;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SensitiveTextRedactor {

    private static final Pattern EMAIL = Pattern.compile("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}");
    private final List<String> configuredTerms;

    public SensitiveTextRedactor(@Value("${bridgeflow.ai.redact-terms:}") String terms) {
        configuredTerms = Arrays.stream(terms.split(","))
            .map(String::trim)
            .filter(term -> !term.isEmpty())
            .toList();
    }

    public String redact(String input) {
        var redacted = EMAIL.matcher(input).replaceAll("[REDACTED_EMAIL]");
        for (var term : configuredTerms) {
            redacted = redacted.replace(term, "[REDACTED]");
        }
        return redacted;
    }
}
