package com.bridgeflow.api.export.api;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bridgeflow.api.auth.application.CurrentUser;
import com.bridgeflow.api.export.application.RequirementExportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/exports")
@Tag(name = "Exports", description = "Audited bilingual project exports")
@SecurityRequirement(name = "bearerAuth")
public class RequirementExportController {
    private final RequirementExportService service;

    public RequirementExportController(RequirementExportService service) { this.service = service; }

    @GetMapping(value = "/requirements.csv", produces = "text/csv")
    @Operation(operationId = "exportRequirementsCsv", summary = "Export active requirements as UTF-8 CSV")
    public ResponseEntity<byte[]> csv(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID projectId) {
        return response(service.exportCsv(user.id(), projectId));
    }

    @GetMapping(value = "/requirements.md", produces = "text/markdown")
    @Operation(operationId = "exportRequirementsMarkdown", summary = "Export active requirements as Markdown")
    public ResponseEntity<byte[]> markdown(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID projectId) {
        return response(service.exportMarkdown(user.id(), projectId));
    }

    private ResponseEntity<byte[]> response(RequirementExportService.ExportFile file) {
        var disposition = ContentDisposition.attachment()
            .filename(file.filename(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .contentType(MediaType.parseMediaType(file.mediaType() + ";charset=UTF-8"))
            .body(file.content());
    }
}
