package com.bridgeflow.api.document.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LocalDocumentStorage {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain",
        "text/markdown"
    );

    private final Path root;
    private final long maxBytes;

    public LocalDocumentStorage(
        @Value("${bridgeflow.storage.root}") String root,
        @Value("${bridgeflow.storage.max-bytes:10485760}") long maxBytes
    ) {
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
    }

    public StoredFile store(UUID projectId, UUID documentId, MultipartFile file) {
        validate(file);
        var filename = safeFilename(file.getOriginalFilename());
        var contentType = file.getContentType().toLowerCase(Locale.ROOT);
        try {
            var bytes = file.getBytes();
            validateSignature(contentType, bytes);
            var storageKey = projectId + "/" + documentId + "/" + UUID.randomUUID();
            var target = resolve(storageKey);
            Files.createDirectories(target.getParent());
            var temporary = Files.createTempFile(target.getParent(), "upload-", ".tmp");
            try {
                Files.write(temporary, bytes);
                try {
                    Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException exception) {
                    Files.move(temporary, target);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
            return new StoredFile(
                storageKey, filename, contentType, bytes.length, sha256(bytes)
            );
        } catch (IOException exception) {
            throw new UncheckedIOException("Không thể lưu file tài liệu.", exception);
        }
    }

    public byte[] read(String storageKey) {
        try {
            return Files.readAllBytes(resolve(storageKey));
        } catch (IOException exception) {
            throw new UncheckedIOException("Không thể đọc file tài liệu.", exception);
        }
    }

    public void deleteQuietly(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException ignored) {
            // Preserve the original database error; orphan cleanup can be retried operationally.
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File tài liệu không được để trống.");
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("File tài liệu vượt quá giới hạn " + maxBytes + " bytes.");
        }
        var contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Chỉ hỗ trợ PDF, DOCX, TXT và Markdown.");
        }
    }

    private void validateSignature(String contentType, byte[] bytes) {
        if ("application/pdf".equals(contentType)
            && !startsWith(bytes, "%PDF".getBytes(StandardCharsets.US_ASCII))) {
            throw new IllegalArgumentException("Nội dung file không phải PDF hợp lệ.");
        }
        if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)
            && !startsWith(bytes, new byte[] {'P', 'K'})) {
            throw new IllegalArgumentException("Nội dung file không phải DOCX hợp lệ.");
        }
        if (contentType.startsWith("text/") && containsNullByte(bytes)) {
            throw new IllegalArgumentException("File văn bản chứa dữ liệu nhị phân không hợp lệ.");
        }
    }

    private Path resolve(String storageKey) {
        var resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root)) throw new IllegalArgumentException("Storage key không hợp lệ.");
        return resolved;
    }

    private String safeFilename(String value) {
        if (value == null || value.isBlank()) return "document";
        var filename = Path.of(value).getFileName().toString().trim();
        return filename.length() <= 255 ? filename : filename.substring(filename.length() - 255);
    }

    private boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) return false;
        for (var index = 0; index < prefix.length; index++) {
            if (bytes[index] != prefix[index]) return false;
        }
        return true;
    }

    private boolean containsNullByte(byte[] bytes) {
        for (var value : bytes) if (value == 0) return true;
        return false;
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record StoredFile(
        String storageKey,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String sha256
    ) {
    }
}
