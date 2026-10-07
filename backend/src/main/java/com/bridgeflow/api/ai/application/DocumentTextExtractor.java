package com.bridgeflow.api.ai.application;

import java.io.ByteArrayInputStream;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DocumentTextExtractor {

    private final int maxCharacters;

    public DocumentTextExtractor(@Value("${bridgeflow.ai.max-input-characters:60000}") int maxCharacters) {
        if (maxCharacters < 1000) throw new IllegalArgumentException("AI input limit must be at least 1000 characters");
        this.maxCharacters = maxCharacters;
    }

    public String extract(byte[] bytes, String contentType) {
        try (var input = new ByteArrayInputStream(bytes)) {
            var metadata = new Metadata();
            metadata.set(Metadata.CONTENT_TYPE, contentType);
            var handler = new BodyContentHandler(maxCharacters * 2);
            new AutoDetectParser().parse(input, handler, metadata, new ParseContext());
            var normalized = handler.toString()
                .replace('\u0000', ' ')
                .replaceAll("[\\t\\x0B\\f]+", " ")
                .replaceAll(" *\\R *", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
            if (normalized.isBlank()) {
                throw new IllegalArgumentException("Không trích xuất được văn bản từ version tài liệu này.");
            }
            return normalized.length() <= maxCharacters
                ? normalized
                : normalized.substring(0, maxCharacters);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Không thể trích xuất văn bản từ tài liệu.", exception);
        }
    }
}
