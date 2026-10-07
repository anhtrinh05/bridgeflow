package com.bridgeflow.api.document.api;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bridgeflow.api.auth.application.CurrentUser;
import com.bridgeflow.api.document.api.DocumentModels.DocumentResponse;
import com.bridgeflow.api.document.application.DocumentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@Validated
@RequestMapping("/api/v1")
@Tag(name = "Documents", description = "Project document and immutable version management")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/projects/{projectId}/documents")
    @Operation(operationId = "listDocuments", summary = "List project documents")
    public List<DocumentResponse> list(
        @AuthenticationPrincipal CurrentUser user,
        @PathVariable UUID projectId,
        @RequestParam(defaultValue = "false") boolean includeArchived
    ) {
        return documentService.list(user.id(), projectId, includeArchived);
    }

    @PostMapping(value = "/projects/{projectId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(operationId = "uploadDocument", summary = "Upload a new document and its first version")
    @ApiResponse(responseCode = "201", description = "Document uploaded")
    public ResponseEntity<DocumentResponse> create(
        @AuthenticationPrincipal CurrentUser user,
        @PathVariable UUID projectId,
        @RequestPart @NotBlank @Size(max = 200) String title,
        @RequestPart MultipartFile file
    ) {
        var document = documentService.create(user.id(), projectId, title, file);
        return ResponseEntity.created(URI.create("/api/v1/documents/" + document.id())).body(document);
    }

    @PostMapping(value = "/documents/{documentId}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(operationId = "uploadDocumentVersion", summary = "Upload an immutable document version")
    public DocumentResponse addVersion(
        @AuthenticationPrincipal CurrentUser user,
        @PathVariable UUID documentId,
        @RequestPart MultipartFile file
    ) {
        return documentService.addVersion(user.id(), documentId, file);
    }

    @GetMapping("/documents/{documentId}/versions/{versionId}/content")
    @Operation(operationId = "downloadDocumentVersion", summary = "Download a document version")
    public ResponseEntity<ByteArrayResource> content(
        @AuthenticationPrincipal CurrentUser user,
        @PathVariable UUID documentId,
        @PathVariable UUID versionId
    ) {
        var content = documentService.content(user.id(), documentId, versionId);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(content.contentType()))
            .contentLength(content.bytes().length)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(content.filename(), StandardCharsets.UTF_8).build().toString())
            .body(new ByteArrayResource(content.bytes()));
    }

    @PostMapping("/documents/{documentId}/archive")
    @Operation(operationId = "archiveDocument", summary = "Archive a document")
    public DocumentResponse archive(
        @AuthenticationPrincipal CurrentUser user,
        @PathVariable UUID documentId
    ) {
        return documentService.archive(user.id(), documentId);
    }
}
